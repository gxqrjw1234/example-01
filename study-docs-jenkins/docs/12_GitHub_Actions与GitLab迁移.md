# 12. GitHub Actions 与 GitLab CI 迁移到 Jenkins

## 1. 三套模型的核心差异

| GitHub Actions | GitLab CI/CD | Jenkins |
|---|---|---|
| Event → Workflow → Job → Step | Pipeline → Stage → Job → Script | Trigger → Job/Build → Stage → Step |
| `on` | `rules`/`only`/Schedule | SCM/Webhook/`triggers` + `when` |
| `runs-on` | Runner tags | Agent label / Pod Template |
| `container` | `image` | Docker/Kubernetes Agent |
| `needs` | `needs`/stage | stage 顺序、`parallel`、`build`、显式数据传递 |
| `run` | `script` | `sh`/`bat`/`powershell` |
| `uses` Action | include/template/component | 插件、Pipeline Step、Shared Library |
| `env`/`vars`/`secrets` | variables/CI secrets | environment、params、Credentials |
| Job outputs | dotenv/artifact/variables | env/返回值/文件/归档/外部制品库 |
| upload/download artifact | artifacts/dependencies | archive、stash/unstash、copyArtifacts |
| cache | cache | 工具缓存、Docker/外部缓存 |
| reusable workflow | include/trigger | Shared Library、下游 Job、Job DSL |
| matrix | parallel:matrix | Declarative matrix/parallel |
| `permissions` | token/protected variables/roles | RBAC、Folder 权限、凭据和 Agent 权限 |
| environment reviewers | protected environment/manual job | `input` + Folder/RBAC/环境锁 |
| `concurrency` | resource_group/interruptible | `disableConcurrentBuilds`、`lock`、Throttle |
| Job summary/annotations | report/artifact | JUnit、Warnings、HTML、description、通知 |

## 2. 迁移步骤

### 第一步：清点流程边界

把原配置按“触发、构建、测试、扫描、制品、部署、通知”拆分，标注每个 Job 的：

- 输入：分支、标签、参数、Secret、上游产物。
- 执行环境：操作系统、镜像、工具、网络和权限。
- 输出：字符串、文件、报告、镜像 digest、部署 URL。
- 失败语义：阻断、不稳定、重试、通知、清理。
- 信任边界：PR/Fork、生产凭据、云身份和 Agent。

### 第二步：选择 Jenkins Job 形态

- 单仓库固定流程：Pipeline Job + Jenkinsfile。
- 分支/PR：Multibranch Pipeline + Branch Source。
- 多仓库统一治理：Organization Folder + Shared Library。
- 原有跨项目流水线：保留独立 Job，使用 `build`/Copy Artifact 或改造成同一 Pipeline 的 stages。

### 第三步：映射运行环境

- GitHub `runs-on`/GitLab Runner tags → Jenkins Agent label。
- GitHub `container`/GitLab `image` → Docker Agent 或 Kubernetes Pod。
- GitHub `services`/GitLab `services` → Pod sidecar、Docker Compose 或外部测试服务。
- Hosted runner 预装工具 → 版本化 Agent 镜像和工具清单。

### 第四步：重建数据流

不要把 `needs`、stage 顺序和数据传递混成一件事：

- 少量字符串：函数返回值、受控 `env` 或 metadata 文件。
- 同一次 Build 的文件：`stash/unstash`。
- 可下载产物：`archiveArtifacts` 或制品库。
- 跨 Job：Copy Artifact/制品库 + 明确 Build Number/digest。
- Secret：Credentials Store + 最小 stage 绑定。

### 第五步：重建权限和审批

GitHub `permissions` 不能直接翻译成一个 Jenkins 权限块。分别设计：

- 谁能看到/构建/配置/取消 Job。
- Job 能使用哪些 Jenkins Credentials。
- Agent 的 OS、容器、Kubernetes 和云权限。
- 生产部署的 `input`、Folder 权限、锁和审计。
- Fork/外部贡献的可信策略。

## 3. 语法对照

### 基础 CI

GitHub Actions：

```yaml
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm test
```

Jenkins Declarative Pipeline：

```groovy
pipeline {
    agent { label 'linux-node' }
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Test') {
            steps {
                sh 'npm ci'
                sh 'npm test'
            }
        }
    }
}
```

### 触发和条件

GitHub：

```yaml
on:
  push:
    branches: [main]
  pull_request:
jobs:
  deploy:
    if: github.ref == 'refs/heads/main'
```

Jenkins：

```groovy
triggers { githubPush() }
stages {
    stage('Deploy') {
        when {
            branch 'main'
            not { changeRequest() }
        }
        steps { sh './deploy.sh' }
    }
}
```

Webhook 是否触发由 Branch Source/Job 配置决定，`when` 只控制 stage 是否执行。生产发布还要加 `input`、凭据、Lock 和环境策略。

### 矩阵

GitHub：

```yaml
strategy:
  matrix:
    node: ['18', '20']
    os: [ubuntu-latest, windows-latest]
```

Jenkins：

```groovy
matrix {
    axes {
        axis { name 'NODE'; values '18', '20' }
        axis { name 'OS'; values 'linux', 'windows' }
    }
    stages {
        stage('Test') {
            steps { echo "NODE=${NODE} OS=${OS}" }
        }
    }
}
```

Jenkins 矩阵的轴值会进入环境变量，Agent 选择、报告路径和 Shell 语法必须显式处理。

### Artifact

GitHub 的 upload/download：

```yaml
- uses: actions/upload-artifact@v4
  with: { name: dist, path: dist }
```

Jenkins：

```groovy
stash name: 'dist', includes: 'dist/**'
// 另一个 Agent/stage
unstash 'dist'
archiveArtifacts artifacts: 'dist/**', fingerprint: true
```

`stash` 是一次 Build 内的临时传递，`archiveArtifacts` 是 Jenkins 归档；生产长期保存使用外部制品库。

## 4. 关键非语法差异

### Workflow 事件与 Jenkins 触发器

GitHub Actions 由事件原生驱动；Jenkins 需要 SCM 插件/Webhook/Timer/Job 配置。多个触发器可能重复构建，应统一入口并做幂等。

### Action 与插件

GitHub Action 通常是版本化仓库组件；Jenkins 插件在 Controller 上运行，权限和升级风险更大。简单脚本不应为了“像 Action”而安装插件，优先使用核心 Step、工具镜像或 Shared Library。

### Runner 与 Agent

GitHub Hosted Runner 通常是短生命周期预装环境；Jenkins 需要显式设计 Agent 池、标签、工具镜像、容量、网络和清理。

### 上下文

GitHub 有 `github`、`runner`、`matrix`、`needs` 等统一 Context；Jenkins 把信息分布在 `env`、`params`、`currentBuild`、SCM 插件和 Job API 中。迁移时不要机械替换变量名，要重建数据契约。

### 权限

GitHub `permissions` 控制 `GITHUB_TOKEN` API scope；Jenkins 要同时限制 RBAC、凭据绑定、Agent OS/容器权限和云端 RBAC。一个拥有 Shell 的 Jenkins Job 即使没有 Jenkins API 权限，也可能滥用 Agent 可见的资源。

## 5. 常见迁移错误

- 把 GitLab `stage` 直接翻译成一组无依赖的 Jenkins stage，导致数据和顺序错误。
- 以为 Jenkins `build` 会自动携带上游 workspace；实际应使用归档、Copy Artifact 或制品库。
- 把 GitHub `secrets` 翻译成 Jenkins 参数；参数会出现在 UI/API/重放记录中。
- 用 `any` Agent 运行不可信 PR，接触到 Docker socket 或云凭据。
- 用 Jenkins 当前工作区作为长期制品存储。
- 让 Jenkinsfile 中的 `sh` 直接拼接分支名、用户输入或 PR 标题。
- 把 `input` 当作完整的生产审批和审计系统，忽略 RBAC、锁、环境凭据和变更记录。
- 只迁移成功路径，没有迁移取消、失败、重试、清理和通知语义。

## 6. 迁移验收清单

- [ ] Push、PR、标签、定时和手动触发分别验证。
- [ ] Branch Source 能发现/删除分支和 PR，Webhook 不重复触发。
- [ ] 代码、工具、容器和服务环境可复现。
- [ ] 变量、参数、Secret、云身份和权限已经重新设计。
- [ ] 字符串、文件、报告、缓存和正式制品使用正确机制。
- [ ] 矩阵/并行有容量、隔离、失败汇总和报告策略。
- [ ] 部署使用不可变版本，有审批、锁、回滚和审计。
- [ ] 失败、取消、超时、重试、清理和通知路径通过测试。
- [ ] Jenkins LTS、插件、Java、Agent 镜像和 Shared Library 有版本清单。
