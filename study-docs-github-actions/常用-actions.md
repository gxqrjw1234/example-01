# GitHub Actions 常用 Actions 清单

GitHub Actions 可以通过 `uses` 引用可复用的 Action。下面按能力域整理常见项目，版本号仅作示例，正式使用前应查看项目文档和最新发布版本。

## 1. GitHub 官方基础类

用于检出代码、配置开发环境、缓存依赖、传递构建产物和调用 GitHub API。

| Action | 常见用途 |
|---|---|
| `actions/checkout` | 将仓库代码检出到 runner |
| `actions/setup-node` | 安装或配置 Node.js，并支持 npm/yarn/pnpm 缓存 |
| `actions/setup-python` | 安装或配置 Python，并支持 pip 缓存 |
| `actions/setup-java` | 安装 Java，配置 Maven/Gradle 环境 |
| `actions/setup-go` | 安装 Go |
| `actions/setup-dotnet` | 安装 .NET SDK |
| `actions/cache` | 缓存依赖或构建目录 |
| `actions/upload-artifact` | 上传测试报告、日志、构建产物 |
| `actions/download-artifact` | 下载其他 job 上传的产物 |
| `actions/github-script` | 使用 JavaScript 调用 GitHub API |

示例：

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-python@v5
    with:
      python-version: '3.12'
      cache: pip
  - run: python -m pytest
```

## 2. AWS 云平台类

用于 AWS 身份认证、ECR 镜像推送、ECS/EKS/Lambda/CloudFormation 部署。

| Action | 常见用途 |
|---|---|
| `aws-actions/configure-aws-credentials` | 配置 AWS 凭据，推荐使用 OIDC Assume Role |
| `aws-actions/amazon-ecr-login` | 登录 Amazon ECR |
| `aws-actions/amazon-ecs-render-task-definition` | 将镜像地址写入 ECS task definition |
| `aws-actions/amazon-ecs-deploy-task-definition` | 部署 ECS task definition |
| `aws-actions/aws-lambda-deploy` | 部署 AWS Lambda |
| `aws-actions/aws-cloudformation-github-deploy` | 部署 CloudFormation 模板 |
| `aws-actions/aws-sam-cli` | 安装或运行 AWS SAM CLI |

常见组合：

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: aws-actions/configure-aws-credentials@v4
    with:
      role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
      aws-region: us-east-1
  - uses: aws-actions/amazon-ecr-login@v2
  - run: docker push "$ECR_REGISTRY/my-app:$GITHUB_SHA"
```

## 3. Azure 云平台类

用于登录 Azure、执行 Azure CLI、部署 App Service/Container Apps/AKS 和 Kubernetes 应用。

| Action | 常见用途 |
|---|---|
| `azure/login` | 登录 Azure，支持服务主体和 OIDC |
| `azure/cli` | 在 workflow 中执行 Azure CLI |
| `azure/webapps-deploy` | 部署 Azure App Service / Web App |
| `azure/container-apps-deploy-action` | 部署 Azure Container Apps |
| `azure/aks-set-context` | 配置 AKS 的 kubectl 上下文 |
| `azure/k8s-deploy` | 将 Kubernetes 清单部署到 AKS 或其他集群 |
| `azure/setup-kubectl` | 安装 kubectl |
| `azure/setup-helm` | 安装 Helm |
| `azure/arm-deploy` | 部署 ARM/Bicep 模板 |

常见组合：

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: azure/login@v2
    with:
      client-id: ${{ secrets.AZURE_CLIENT_ID }}
      tenant-id: ${{ secrets.AZURE_TENANT_ID }}
      subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
  - uses: azure/cli@v2
    with:
      inlineScript: az account show
```

## 4. Docker 与容器镜像类

用于构建镜像、多平台构建、登录镜像仓库、生成标签和推送镜像。

| Action | 常见用途 |
|---|---|
| `docker/login-action` | 登录 Docker Hub、GHCR、ECR、ACR 等镜像仓库 |
| `docker/setup-qemu-action` | 配置跨架构构建支持 |
| `docker/setup-buildx-action` | 配置 Docker Buildx |
| `docker/metadata-action` | 根据分支、Tag、SHA 自动生成镜像标签 |
| `docker/build-push-action` | 构建并可选推送 Docker 镜像 |
| `docker/scout-action` | Docker 镜像安全和供应链分析 |
| `aquasecurity/trivy-action` | 扫描镜像、文件系统和 IaC 配置 |

常见组合：

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: docker/setup-buildx-action@v3
  - uses: docker/login-action@v3
    with:
      registry: ghcr.io
      username: ${{ github.actor }}
      password: ${{ secrets.GITHUB_TOKEN }}
  - uses: docker/build-push-action@v6
    with:
      context: .
      push: true
      tags: ghcr.io/${{ github.repository }}:${{ github.sha }}
```

## 5. Terraform 与基础设施即代码类

Terraform 通常由 Action 安装 CLI，再用 `run` 执行 `fmt`、`init`、`validate`、`plan` 和 `apply`。

| Action / 命令 | 常见用途 |
|---|---|
| `hashicorp/setup-terraform` | 安装指定版本的 Terraform CLI |
| `terraform fmt -check` | 检查 Terraform 文件格式 |
| `terraform init` | 初始化 provider 和 backend |
| `terraform validate` | 验证 Terraform 配置 |
| `terraform plan` | 预览基础设施变更 |
| `terraform apply` | 应用基础设施变更 |
| `hashicorp/tfc-workflows-github` | 对接 HCP Terraform / Terraform Cloud |
| `terraform-linters/setup-tflint` | 安装 TFLint 并执行规则检查 |
| `bridgecrewio/checkov-action` | 扫描 Terraform、CloudFormation 等 IaC 配置 |

最小检查示例：

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: hashicorp/setup-terraform@v3
    with:
      terraform_version: 1.9.8
  - run: terraform fmt -check -recursive
  - run: terraform init -backend=false
  - run: terraform validate
  - run: terraform plan -input=false
```

## 6. Ansible 自动化运维类

Ansible 没有必须使用的官方安装 Action。常见做法是用 `actions/setup-python` 安装 Ansible 和 `ansible-lint`，再通过 `run` 执行命令；需要简化 SSH、inventory 和 Vault 参数传递时，也可以使用第三方 Action。

| Action / 命令 | 常见用途 |
|---|---|
| `actions/setup-python` | 安装 Python，为 Ansible 提供运行环境 |
| `dawidd6/action-ansible-playbook` | 执行 Ansible Playbook，并支持 SSH key、inventory 和 Vault password |
| `ansible-lint` | 检查 Playbook、Role 和 Task 的规范及常见错误 |
| `ansible-galaxy collection install` | 根据 `requirements.yml` 安装 Ansible Collections |
| `ansible-playbook --syntax-check` | 在真正执行前检查 Playbook 语法 |

推荐的 CI 检查示例：

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-python@v5
    with:
      python-version: '3.12'
  - name: Install Ansible
    run: |
      python -m pip install --upgrade pip
      pip install ansible ansible-lint
      if [ -f requirements.yml ]; then
        ansible-galaxy collection install -r requirements.yml
      fi
  - name: Lint
    run: ansible-lint
  - name: Syntax check
    run: ansible-playbook --syntax-check -i inventory.ini deploy.yml
```

使用第三方 Action 执行部署时，可以这样传入 SSH 私钥、inventory 和 Ansible Vault 密码：

```yaml
steps:
  - uses: actions/checkout@v4
  - name: Run Ansible playbook
    uses: dawidd6/action-ansible-playbook@v9
    with:
      playbook: deploy.yml
      directory: ./
      key: ${{ secrets.SSH_PRIVATE_KEY }}
      vault_password: ${{ secrets.ANSIBLE_VAULT_PASSWORD }}
      inventory: |
        [web]
        web-01.example.com
      options: |
        --limit web
        --verbose
```

生产部署建议使用 GitHub `environment` 审批，并将 SSH 私钥、Vault 密码和必要的连接信息放在对应环境的 Secrets 中。使用第三方 Action 前应检查其最新版本和输入参数；高权限部署场景优先固定到完整 commit SHA。

## 7. Kubernetes 类

用于配置集群访问、安装 kubectl/Helm 和发布 Kubernetes 清单。Azure AKS 场景常与 Azure 类 Action 一起使用。

| Action | 常见用途 |
|---|---|
| `azure/aks-set-context` | 设置 AKS 集群上下文 |
| `azure/k8s-deploy` | 部署 Kubernetes 清单 |
| `azure/setup-kubectl` | 安装 kubectl |
| `azure/setup-helm` | 安装 Helm |
| `helm/kind-action` | 创建用于测试的本地 kind 集群 |
| `deliverybot/helm` | 执行 Helm 部署流程 |

## 8. 发布、Release 与制品类

用于创建 GitHub Release、生成变更日志、上传 Release 资产和发布 GitHub Pages。

| Action | 常见用途 |
|---|---|
| `softprops/action-gh-release` | 创建 GitHub Release 并上传资产 |
| `release-drafter/release-drafter` | 根据 PR 自动草拟 Release 说明 |
| `goreleaser/goreleaser-action` | Go 项目跨平台构建、打包和发布的标准工具 |
| `actions/upload-pages-artifact` | 上传 GitHub Pages 静态站点产物 |
| `actions/deploy-pages` | 部署 GitHub Pages |
| `actions/upload-artifact` | 保存 workflow 运行期间的构建产物 |

## 9. 代码质量与安全类

用于静态分析、依赖审查、代码规范检查和漏洞扫描。

| Action | 常见用途 |
|---|---|
| `github/codeql-action` | CodeQL 代码安全分析 |
| `actions/dependency-review-action` | 检查 Pull Request 引入的依赖风险 |
| `super-linter/super-linter` | 统一运行多种语言的 lint 工具 |
| `aquasecurity/trivy-action` | 扫描容器、依赖和 IaC |
| `terraform-linters/setup-tflint` | Terraform 代码规范与规则检查 |
| `gitleaks/gitleaks-action` | 检测代码中意外提交的密钥 |

## 10. 测试与覆盖率类

用于发布测试结果、上传覆盖率和在 Pull Request 中展示测试状态。

| Action | 常见用途 |
|---|---|
| `dorny/test-reporter` | 将 JUnit 等测试报告转换为 GitHub 检查结果 |
| `codecov/codecov-action` | 上传代码覆盖率报告 |
| `EnricoMi/publish-unit-test-result-action` | 发布单元测试结果和摘要 |
| `browser-actions/setup-chrome` | 安装 Chrome，用于浏览器端测试 |
| `cypress-io/github-action` | 执行 Cypress 测试 |

## 11. Pull Request、Issue 与仓库治理类

用于自动评论、添加标签、维护 reviewer 和自动合并流程。

| Action | 常见用途 |
|---|---|
| `actions/labeler` | 根据变更文件自动添加 PR 标签 |
| `peter-evans/create-or-update-comment` | 创建或更新 Issue/PR 评论 |
| `peter-evans/enable-pull-request-automerge` | 启用 Pull Request 自动合并 |
| `peter-evans/find-comment` | 查找已有评论，配合更新评论使用 |
| `actions/stale` | 标记或关闭长期无活动的 Issue/PR |
| `github/issue-labeler` | 根据内容自动标记 Issue |

## 12. 通知类

用于在 workflow 成功、失败或部署完成后通知团队。

| Action | 常见用途 |
|---|---|
| `8398a7/action-slack` | 发送 Slack 通知 |
| `Ilshidur/action-discord` | 发送 Discord 通知 |
| `peter-evans/repository-dispatch` | 向其他仓库发送自定义 dispatch 事件 |
| `dawidd6/action-send-mail` | 发送邮件通知 |

## 13. 按场景选择组合

| 场景 | 推荐组合 |
|---|---|
| Node.js / Python CI | `checkout` + `setup-*` + 测试命令 + `upload-artifact` |
| Docker 构建并推送 GHCR | `checkout` + `docker/login-action` + `docker/build-push-action` |
| AWS ECR/ECS 部署 | AWS credentials + ECR login + ECS render/deploy |
| Azure App Service 部署 | `azure/login` + `azure/webapps-deploy` |
| Azure AKS 部署 | `azure/login` + `azure/aks-set-context` + `azure/k8s-deploy` |
| Ansible 自动化运维 | `checkout` + `setup-python` + `ansible-lint` + `ansible-playbook` |
| Terraform CI | `checkout` + `setup-terraform` + `fmt`/`init`/`validate`/`plan` |
| Terraform 生产部署 | Terraform CI + 云平台 OIDC 登录 + `environment` 审批 + `apply` |
| PR 安全检查 | CodeQL + dependency review + secret/IaC 扫描 |
| Release 发布 | Release Drafter + `softprops/action-gh-release` |

## 14. 离线 / 隔离网络（Air-gapped）场景

Runner 无法访问互联网时，有两种主要解决方式。

### 方式一：在内部 Git 服务镜像 Action 仓库

将需要使用的 Action 仓库完整同步到组织内部的 Git 服务（GitHub Enterprise Server、Gitea 等），再通过内部域名引用。Runner 只需能访问内部 Git 服务即可，无需访问 `github.com`。

```yaml
# 外网写法
- uses: actions/checkout@v4

# 内网写法（内部镜像仓库）
- uses: internal-github.example.com/mirror/actions-checkout@v4
```

对于 GitHub Enterprise Server（GHES），可以通过 **GitHub Connect** 自动同步 `github.com` 上的 Action，无需手动逐个镜像。若使用其他 Git 服务，则需要自行定期同步 Action 仓库，并在 workflow 中统一替换 `uses` 路径。

常见需要镜像的 Action（本文档中的高频项）：

```
actions/checkout
actions/setup-python / setup-node / setup-go
actions/cache
actions/upload-artifact / download-artifact
docker/setup-buildx-action
docker/build-push-action
hashicorp/setup-terraform
```

### 方式二：GitHub Actions Runner Controller（ARC）+ 内网自定义 Runner 镜像

在内网 Kubernetes 集群部署 ARC，Runner Pod 使用预装了所有工具的自定义镜像，从内网 Registry（Harbor、ACR、ECR 等）拉取，运行时不依赖任何外部下载。

```yaml
# runner pod 镜像示例（内网 Harbor）
image: harbor.internal.example.com/runners/ubuntu-runner:24.04
```

自定义镜像中通常预装：

```dockerfile
FROM ubuntu:24.04

# 运行时和工具
RUN apt-get install -y python3 python3-pip nodejs docker.io kubectl helm && \
    pip install ansible ansible-lint && \
    # 从内网 HTTP 文件服务器下载 Terraform 二进制
    curl -fsSL http://files.internal.example.com/terraform_1.9.8_linux_amd64.zip \
      -o /tmp/tf.zip && unzip /tmp/tf.zip -d /usr/local/bin/
```

使用预装镜像后，workflow 中去掉对应的 `setup-*` Action，直接调用命令：

```yaml
# 不再需要这些 setup Action
# - uses: actions/setup-python@v5
# - uses: hashicorp/setup-terraform@v3

steps:
  - uses: internal-github.example.com/mirror/actions-checkout@v4
  - run: python --version        # 镜像已预装
  - run: terraform version       # 镜像已预装
  - run: ansible --version       # 镜像已预装
```

两种方式可以组合使用：**ARC + 自定义镜像** 解决工具依赖，**内部 Action 镜像仓库** 解决 `uses` 引用，共同实现完全离线的 CI/CD 流程。

## 15. 使用安全建议

1. 优先选择 GitHub 官方、云厂商官方或维护活跃的 Action。
2. 版本至少固定到 major 版本，例如 `@v4`；对生产环境和高权限 Action，优先固定到完整 commit SHA。
3. 引入第三方 Action 前检查维护状态、来源、权限需求和安全公告。
4. 在 workflow 或 job 级别声明最小 `permissions`，不要默认使用 `write-all`。
5. 云平台优先使用 OIDC 短期身份凭据，避免长期保存 AWS Access Key 或 Azure Client Secret。
6. 不要把 secrets 直接打印到日志，也不要把包含敏感信息的 Terraform plan 随意上传为 artifact。
7. 对来自 Fork 的 Pull Request 按不可信代码处理，不要让其直接接触生产 secrets 或部署权限。
8. Ansible 部署应保护 SSH 私钥和 Vault 密码，使用受保护的 `environment`，并避免通过日志或 `--extra-vars` 暴露敏感值。
9. 不要为了绕过连接问题而长期关闭 SSH host key 校验；应通过受保护的 `known_hosts` 配置验证目标主机。

