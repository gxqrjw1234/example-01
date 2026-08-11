# 常用 GitHub Actions 列表

面向 GitHub Actions 初学者，特别适合熟悉 GitLab CI/CD 的开发者。

## 1. 基础 Action

| Action | 用途 | GitLab CI/CD 类比 |
|---|---|---|
| `actions/checkout@v4` | 将仓库代码检出到 runner | GitLab Runner 自动 clone 代码 |
| `actions/setup-node@v4` | 安装并配置 Node.js | `image: node:20` 或安装 Node.js |
| `actions/setup-python@v5` | 安装并配置 Python | `image: python:3.12` |
| `actions/setup-java@v4` | 安装并配置 Java | `image: maven` 或 `image: eclipse-temurin` |
| `actions/setup-go@v5` | 安装并配置 Go | `image: golang` |
| `actions/setup-dotnet@v4` | 安装并配置 .NET | `image: mcr.microsoft.com/dotnet/sdk` |

### 基本示例

```yaml
steps:
  - name: Checkout source
    uses: actions/checkout@v4

  - name: Setup Node.js
    uses: actions/setup-node@v4
    with:
      node-version: '20'
```

`actions/checkout` 通常应是使用源码的 job 中的第一个 Action，因为 GitHub Actions 不会像 GitLab Runner 一样自动把仓库代码放到工作目录。

---

## 2. 依赖安装、缓存和构建

| Action | 用途 | 常见场景 |
|---|---|---|
| `actions/cache@v4` | 缓存依赖或构建目录 | npm、Maven、Gradle、pip 缓存 |
| `actions/upload-artifact@v4` | 上传构建产物、测试报告 | 保存 `dist/`、coverage、日志 |
| `actions/download-artifact@v4` | 下载其他 job 上传的产物 | 部署 job 获取 build 结果 |
| `actions/upload-pages-artifact@v3` | 准备 GitHub Pages 发布产物 | 静态网站部署 |

### Node.js 缓存示例

Node.js 项目通常直接使用 `setup-node` 的内置缓存：

```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: npm

- run: npm ci
- run: npm test
```

### Artifact 示例

```yaml
- name: Upload build artifact
  uses: actions/upload-artifact@v4
  with:
    name: dist
    path: dist/
    retention-days: 7

- name: Download build artifact
  uses: actions/download-artifact@v4
  with:
    name: dist
    path: dist/
```

注意：`needs` 只表达 job 依赖关系，不会自动传递文件；文件需要显式 upload 和 download。

---

## 3. Docker 和容器镜像

| Action | 用途 |
|---|---|
| `docker/setup-buildx-action@v3` | 设置 Docker Buildx，支持高级构建和多平台镜像 |
| `docker/login-action@v3` | 登录 Docker Hub、GHCR、ACR 等镜像仓库 |
| `docker/metadata-action@v5` | 根据分支、Tag、SHA 自动生成镜像标签 |
| `docker/build-push-action@v6` | 构建并推送 Docker 镜像 |
| `docker/setup-qemu-action@v3` | 配置 QEMU，支持跨 CPU 架构构建 |
| `docker/scout-action@v1` | Docker 镜像分析和安全建议 |

### 构建并推送到 GHCR

```yaml
permissions:
  contents: read
  packages: write

steps:
  - uses: actions/checkout@v4

  - name: Login to GHCR
    uses: docker/login-action@v3
    with:
      registry: ghcr.io
      username: ${{ github.actor }}
      password: ${{ secrets.GITHUB_TOKEN }}

  - name: Build and push image
    uses: docker/build-push-action@v6
    with:
      context: .
      push: true
      tags: ghcr.io/${{ github.repository }}:${{ github.sha }}
```

对应 GitLab 常见逻辑：

```yaml
docker build -t image:tag .
docker push image:tag
```

---

## 4. 代码检查、测试和安全扫描

以下 Action 常见于 Marketplace 或社区维护，使用前应审查仓库、版本和权限。

| Action | 用途 |
|---|---|
| `github/codeql-action/init@v3` | 初始化 CodeQL 代码安全分析 |
| `github/codeql-action/autobuild@v3` | 自动构建 CodeQL 分析所需项目 |
| `github/codeql-action/analyze@v3` | 执行 CodeQL 分析并上传结果 |
| `github/dependabot/fetch-metadata@v2` | 读取 Dependabot PR 元数据 |
| `github/super-linter/slim@v7` | 集成多种语言的代码检查器 |
| `super-linter/super-linter/slim@v7` | 多语言 lint 和格式检查 |
| `github/advanced-security/sbom-action@v1` | 生成软件物料清单 SBOM |
| `aquasecurity/trivy-action@master` | 扫描文件系统、容器镜像和依赖漏洞 |
| `sonarsource/sonarqube-scan-action@v6` | 执行 SonarQube 代码质量分析 |
| `reviewdog/action-eslint@v1` | 将 ESLint 结果评论到 Pull Request |

### CodeQL 基本示例

```yaml
name: CodeQL

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

permissions:
  security-events: write
  contents: read

jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: javascript

      - name: Analyze
        uses: github/codeql-action/analyze@v3
```

### Trivy 容器扫描示例

```yaml
- name: Scan image
  uses: aquasecurity/trivy-action@0.28.0
  with:
    image-ref: ghcr.io/${{ github.repository }}:${{ github.sha }}
    format: table
    exit-code: '1'
    severity: CRITICAL,HIGH
```

生产环境不建议无条件使用 `@master`；应优先固定到正式版本或完整 commit SHA。

---

## 5. 发布、Release 和包管理

| Action | 用途 |
|---|---|
| `actions/create-release` | 创建 GitHub Release（历史项目常见） |
| `softprops/action-gh-release@v2` | 创建 Release 并上传附件 |
| `actions/upload-pages-artifact@v3` | 上传 GitHub Pages 静态站点产物 |
| `actions/deploy-pages@v4` | 部署到 GitHub Pages |
| `JS-DevTools/npm-publish@v4` | 发布 npm 包 |
| `docker/build-push-action@v6` | 发布容器镜像 |
| `peter-evans/create-pull-request@v7` | 自动创建或更新 Pull Request |
| `EndBug/add-and-commit@v9` | 自动提交生成的文件 |

### 创建 GitHub Release

```yaml
- name: Create release
  uses: softprops/action-gh-release@v2
  with:
    tag_name: v${{ github.run_number }}
    generate_release_notes: true
    files: |
      dist/app.zip
      dist/checksums.txt
```

发布 Action 通常需要额外的 `contents: write` 权限：

```yaml
permissions:
  contents: write
```

---

## 6. GitHub Pages

| Action | 用途 |
|---|---|
| `actions/configure-pages@v5` | 配置 Pages 环境 |
| `actions/upload-pages-artifact@v3` | 上传静态网站文件 |
| `actions/deploy-pages@v4` | 部署到 GitHub Pages |

### GitHub Pages 基本示例

```yaml
name: Deploy Pages

on:
  push:
    branches: [main]

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/configure-pages@v5

      - uses: actions/upload-pages-artifact@v3
        with:
          path: './dist'

      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

---

## 7. 云平台部署

### Azure

| Action | 用途 |
|---|---|
| `azure/login@v2` | 使用 Service Principal 或 OpenID Connect 登录 Azure |
| `azure/webapps-deploy@v3` | 部署到 Azure App Service |
| `azure/aks-set-context@v4` | 设置 AKS Kubernetes 上下文 |
| `azure/k8s-bake@v3` | 使用 Helm、Kustomize 等生成 Kubernetes manifests |
| `azure/k8s-deploy@v5` | 部署 Kubernetes manifests 到 AKS |
| `azure/container-apps-deploy-action@v2` | 部署到 Azure Container Apps |

### AWS

| Action | 用途 |
|---|---|
| `aws-actions/configure-aws-credentials@v4` | 配置 AWS 凭据，推荐 OIDC |
| `aws-actions/amazon-ecr-login@v2` | 登录 Amazon ECR |
| `aws-actions/amazon-ecs-deploy-task-definition@v2` | 部署到 Amazon ECS |
| `aws-actions/amazon-eks-update-kubeconfig@v2` | 配置 EKS kubeconfig |

### Google Cloud

| Action | 用途 |
|---|---|
| `google-github-actions/auth@v2` | 使用 Workload Identity Federation 登录 Google Cloud |
| `google-github-actions/setup-gcloud@v2` | 安装并配置 gcloud CLI |
| `google-github-actions/deploy-cloudrun@v2` | 部署到 Cloud Run |
| `google-github-actions/get-gke-credentials@v2` | 获取 GKE 集群凭据 |

### Azure OIDC 登录示例

```yaml
permissions:
  id-token: write
  contents: read

steps:
  - uses: actions/checkout@v4

  - name: Login to Azure
    uses: azure/login@v2
    with:
      client-id: ${{ secrets.AZURE_CLIENT_ID }}
      tenant-id: ${{ secrets.AZURE_TENANT_ID }}
      subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}

  - name: Deploy to App Service
    uses: azure/webapps-deploy@v3
    with:
      app-name: my-app
      package: ./dist
```

使用 OIDC 时通常不需要把长期 Azure client secret 存在 GitHub Secrets 中，但仍需在 Azure 和 GitHub 之间正确配置 Federated Identity Credential。

---

## 8. 通知和协作

| Action | 用途 |
|---|---|
| `slackapi/slack-github-action@v2` | 发送 Slack 消息 |
| `actions/github-script@v7` | 使用 JavaScript 调用 GitHub API |
| `peter-evans/find-comment@v3` | 查找 Pull Request 评论 |
| `peter-evans/create-or-update-comment@v4` | 创建或更新 Pull Request 评论 |
| `marocchino/sticky-pull-request-comment@v2` | 维护可更新的 PR 评论 |
| `dorny/test-reporter@v2` | 将测试结果转换为 GitHub 检查报告 |
| `EnricoMi/publish-unit-test-result-action@v2` | 发布 JUnit 等单元测试结果 |

### github-script 示例

```yaml
- name: Add PR comment
  uses: actions/github-script@v7
  with:
    script: |
      const body = 'CI completed successfully.';
      await github.rest.issues.createComment({
        owner: context.repo.owner,
        repo: context.repo.repo,
        issue_number: context.issue.number,
        body,
      });
```

如果 Action 需要写 PR、Issue 或 Release，必须同时检查 `permissions`：

```yaml
permissions:
  issues: write
  pull-requests: write
```

---

## 9. 选型和安全建议

### 按任务选择

```text
检出代码          → actions/checkout
设置开发语言      → actions/setup-node / setup-python / setup-java
缓存依赖          → setup-* 的内置 cache 或 actions/cache
保存文件          → upload-artifact / download-artifact
构建镜像          → docker/build-push-action
扫描漏洞          → CodeQL / Trivy
发布 Release      → softprops/action-gh-release
发布 GitHub Pages → configure-pages + upload-pages-artifact + deploy-pages
云平台登录        → 各云厂商 login/auth Action
调用 GitHub API   → actions/github-script
```

### 安全检查清单

- 优先使用官方或经过组织审查的 Action。
- 查看 Action 仓库的 README、`action.yml`、Release、Issues 和维护状态。
- 生产环境优先使用正式版本号或完整 commit SHA，不要直接使用 `@main`、`@master`。
- 为 workflow 或 job 声明最小 `permissions`。
- 不要把 secrets 拼接到 shell 命令中；通过 `env:` 传递，并避免打印敏感值。
- 对 `pull_request` 中来自 Fork 的代码按不可信代码处理。
- 第三方 Action 如果需要高权限、执行 Docker 或上传文件，应先审查其实现。
- 使用 GitHub 官方 Actions 页面和 Action 仓库的 README 核对当前版本，版本号可能随时间变化。

## 10. 官方查找入口

- GitHub Marketplace Actions：<https://github.com/marketplace?type=actions>
- GitHub Actions 文档：<https://docs.github.com/actions>
- GitHub Actions 语法：<https://docs.github.com/actions/using-workflows/workflow-syntax-for-github-actions>
- GitHub 官方 Actions 组织：<https://github.com/actions>
- GitHub Actions Runner 文档：<https://docs.github.com/actions/hosting-your-own-runners>
- GitHub Actions 安全强化：<https://docs.github.com/actions/security-for-github-actions>

Action 引用的基本格式：

```yaml
- uses: OWNER/REPOSITORY@VERSION
```

例如：

```yaml
- uses: actions/checkout@v4
```

其中：

- `actions` 是组织或用户名称。
- `checkout` 是仓库名称。
- `v4` 是引用的版本标签。
