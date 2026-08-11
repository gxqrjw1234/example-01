# 07. Docker、Kubernetes 与云部署

## 1. 容器化 Jenkins Agent

固定工具链优先通过版本化 Docker 镜像提供，而不是在每次构建中临时安装：

```groovy
pipeline {
    agent {
        docker {
            image 'python:3.12-slim@sha256:<pin-in-production>'
            args '--user 1000:1000'
            reuseNode true
        }
    }
    stages {
        stage('Test') {
            steps {
                sh 'python --version && pip install --require-hashes -r requirements.txt'
                sh 'pytest --junitxml=reports/junit.xml'
            }
        }
    }
}
```

生产中应固定基础镜像 digest、定期重建、扫描镜像、限制用户权限。不要为了构建镜像而无条件把 `/var/run/docker.sock` 挂进不可信 Agent；这等价于给 Pipeline 较高的宿主机控制能力。

## 2. Docker 镜像构建和推送

推荐使用 Docker Pipeline 或直接调用 CLI。无论哪种方式，都要：

- 以 commit SHA 或版本号作为不可变标签，同时可维护一个受控的发布别名。
- 推送后读取 digest，后续部署使用 digest。
- 使用 `withCredentials` 或云厂商短期凭据登录 Registry。
- 构建上下文中排除 Secret、测试数据和无关文件。
- 使用 BuildKit/多阶段构建和非 root 运行时用户。
- 对最终镜像执行 Trivy/Grype 等扫描，扫描失败策略明确。

示例命令：

```groovy
withCredentials([usernamePassword(
    credentialsId: 'registry-push',
    usernameVariable: 'REGISTRY_USER',
    passwordVariable: 'REGISTRY_PASSWORD'
)]) {
    sh '''
        set -Eeuo pipefail
        printf '%s' "$REGISTRY_PASSWORD" | docker login registry.example.com \\
          --username "$REGISTRY_USER" --password-stdin
        docker build --pull --tag "registry.example.com/app:${GIT_COMMIT}" .
        docker push "registry.example.com/app:${GIT_COMMIT}"
        docker image inspect "registry.example.com/app:${GIT_COMMIT}" \\
          --format '{{index .RepoDigests 0}}' > image-digest.txt
    '''
}
archiveArtifacts artifacts: 'image-digest.txt', fingerprint: true
```

## 3. Kubernetes Plugin

Kubernetes Plugin 可以按 Pod Template 动态创建 Agent。Pod 中每个容器承担一个职责，例如 `jnlp` 负责连接、`builder` 负责构建、`kubectl` 负责部署：

```groovy
podTemplate(
    yaml: '''
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins-builder
  containers:
  - name: builder
    image: gcr.io/kaniko-project/executor:v1.23.2
    command: ["sleep"]
    args: ["99d"]
  - name: kubectl
    image: bitnami/kubectl:1.31
    command: ["sleep"]
    args: ["99d"]
'''
) {
    node(POD_LABEL) {
        container('builder') {
            sh '/kaniko/executor --context "$WORKSPACE" --destination registry.example.com/app:$GIT_COMMIT'
        }
    }
}
```

生产注意：

- Pod ServiceAccount 只授予构建命名空间的最小权限。
- 默认启用 `automountServiceAccountToken: false`，只有确实需要 Kubernetes API 的容器才启用受限 Token。
- 使用 NetworkPolicy、Pod Security、只读根文件系统、非 root 用户和资源 request/limit。
- 不要让 PR 的不可信代码复用拥有生产权限的 Pod Template。
- Agent 结束后销毁，避免工作区和凭据残留。

完整高级示例见 [05_kubernetes-pod-agent.groovy](../examples/advanced/05_kubernetes-pod-agent.groovy)。

## 4. Helm 和 Kubernetes 部署

部署步骤的核心是“验证后发布”：

```groovy
withCredentials([file(credentialsId: 'kubeconfig-staging', variable: 'KUBECONFIG')]) {
    sh '''
        set -Eeuo pipefail
        helm lint charts/app
        helm template app charts/app --namespace staging \\
          --set image.repository=registry.example.com/app \\
          --set image.digest="$IMAGE_DIGEST" > rendered.yaml
        kubectl --kubeconfig "$KUBECONFIG" apply --dry-run=server -f rendered.yaml
        helm upgrade --install app charts/app \\
          --namespace staging --create-namespace \\
          --set image.digest="$IMAGE_DIGEST" \\
          --atomic --timeout 10m
    '''
}
```

- `--atomic` 使失败发布回滚，但要验证数据库迁移等不可逆操作。
- 使用 readiness/liveness、滚动策略和 smoke test。
- 生产部署使用受保护的凭据、审批、Lock 和审计。
- 最好由 GitOps Controller（Argo CD/Flux）执行集群变更，Jenkins 只生成并签名制品或更新 GitOps 仓库。

## 5. Azure、AWS、GCP

Jenkins 不提供一个跨云通用的“登录 Action”。常见方式：

- **Azure**：Azure CLI/插件、Managed Identity、Workload Identity Federation；使用 Azure RBAC 最小角色。
- **AWS**：IAM Role、OIDC Web Identity、实例/Pod Role；避免长期 Access Key。
- **GCP**：Workload Identity Federation、短期服务账号凭据；避免提交 JSON key。

云凭据应在 Jenkins Credentials 或外部 Secret Manager 中管理。优先短期 Token 和角色联邦，限制 subject 为特定 Job/Folder/环境。Jenkins 的“凭据可用”不等于云端有权限，必须分别审计。

### Azure CLI 概念示例

```groovy
withCredentials([usernamePassword(
    credentialsId: 'azure-service-principal',
    usernameVariable: 'AZURE_CLIENT_ID',
    passwordVariable: 'AZURE_CLIENT_SECRET'
)]) {
    withEnv(['AZURE_TENANT_ID=replace-me', 'AZURE_SUBSCRIPTION_ID=replace-me']) {
        sh '''
            set +x
            az login --service-principal \\
              --username "$AZURE_CLIENT_ID" \\
              --password "$AZURE_CLIENT_SECRET" \\
              --tenant "$AZURE_TENANT_ID" >/dev/null
            az account set --subscription "$AZURE_SUBSCRIPTION_ID"
            az webapp deploy --resource-group rg-staging --name app-staging --src-path dist.zip
            az logout
        '''
    }
}
```

生产环境优先使用 Jenkins Agent 的 Managed Identity 或 OIDC，而不是上面的长期 Secret。示例中的占位符必须替换为受控配置，不能原样部署。

## 6. 基础设施即代码

Terraform/Bicep/CloudFormation/Ansible 都应作为独立阶段：

1. `fmt`/lint。
2. validate。
3. plan 并保存计划文件。
4. 安全和策略扫描。
5. 审批。
6. apply 到指定环境。
7. 输出资源标识并归档审计信息。

Plan 必须与 Apply 使用同一提交和同一输入。不要让 Pipeline 在 Apply 阶段重新生成未经审查的 Plan。状态文件放远程后端并启用锁、加密和最小权限。

## 7. 云原生部署最佳实践

- 构建与部署分离：一个 Build 只构建一次，多个环境复用相同 digest。
- 生产不使用 `latest`，不在部署阶段执行 `docker build`。
- 逐步发布：蓝绿、金丝雀、滚动和自动回滚。
- 发布前做策略、漏洞、签名和供应链证明检查。
- 使用环境锁和健康检查，避免两个 Build 同时修改同一资源。
- 把 Kubernetes 访问配置限制在单个命名空间和单个环境。
- 云账号、Jenkins Folder、Job、Agent 和凭据保持一一对应的信任边界。
