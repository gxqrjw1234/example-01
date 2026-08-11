# 00. GitHub Actions 源资料分析与 Jenkins 映射

本文件记录对 `study-docs-github-actions` 目录的分析结果，并说明本目录如何把同一知识点重建为 Jenkins 学习内容。Jenkins 不是 GitHub Actions 的 YAML 兼容实现，因此下面采用“概念对应 + 运行模型差异 + 可落地机制”的方式映射。

## 1. 逐文件覆盖关系

| 源文件 | 主要知识点 | Jenkins 对应文档/示例 |
|---|---|---|
| `github-actions.md` | checkout、语言环境、缓存、Artifact、Docker、扫描、Release、Pages、云部署、通知和 Action 安全 | [02_Pipeline语法与Jenkinsfile](02_Pipeline语法与Jenkinsfile.md)、[06_产物报告缓存与数据传递](06_产物报告缓存与数据传递.md)、[07_Docker_Kubernetes与云部署](07_Docker_Kubernetes与云部署.md)、示例 04/06 |
| `常用-actions.md` | 官方/第三方 Action 选型、Docker、CodeQL、Trivy、Release、云登录、通知 | [07_Docker_Kubernetes与云部署](07_Docker_Kubernetes与云部署.md)、[09_安全权限与供应链](09_安全权限与供应链.md)、高级示例 04 |
| `触发事件.md` | push、PR、review、release、schedule、workflow_run、workflow_call、dispatch、路径/分支过滤 | [03_触发器与SCM集成](03_触发器与SCM集成.md)、[11_高级架构多分支矩阵性能与高可用](11_高级架构多分支矩阵性能与高可用.md)、示例 05 和高级示例 02 |
| `变量.md` | env/vars/secrets、作用域、环境文件、job outputs、跨平台变量 | [04_变量参数凭据与上下文](04_变量参数凭据与上下文.md)、[06_产物报告缓存与数据传递](06_产物报告缓存与数据传递.md)、示例 02 |
| `上下文参考.md` | github、env、vars、job、runner、strategy、matrix、inputs、secrets、steps、needs | [04_变量参数凭据与上下文](04_变量参数凭据与上下文.md)、[13_Jenkins关键字与步骤速查](13_Jenkins关键字与步骤速查.md) |
| `工作流命令.md` | debug、notice、warning、error、mask、summary、输出和环境文件 | [10_调试观测通知与运维](10_调试观测通知与运维.md)、[06_产物报告缓存与数据传递](06_产物报告缓存与数据传递.md) |
| `if-语句.md` | 默认 success、always/failure/success/cancelled、needs 结果、条件组合 | [05_控制流并发与错误处理](05_控制流并发与错误处理.md)、示例 02/03 |
| `代码复用.md` | YAML anchor、Reusable Workflow、Composite Action、输入输出和 Secret 传递 | [08_Shared_Library与自动化复用](08_Shared_Library与自动化复用.md)、高级示例 01 |
| `权限.md` | GITHUB_TOKEN、最小权限、job 覆盖、Fork/Dependabot、OIDC | [09_安全权限与供应链](09_安全权限与供应链.md)、高级示例 02/04/05/06 |
| `调试.md` | 调试日志、上下文、失败产物、tmate、结果、Summary、CLI | [10_调试观测通知与运维](10_调试观测通知与运维.md)、[06_产物报告缓存与数据传递](06_产物报告缓存与数据传递.md) |
| `github-jobs-data-transfer.md` | outputs、artifact、GITHUB_ENV、cache、跨 workflow 数据 | [06_产物报告缓存与数据传递](06_产物报告缓存与数据传递.md)、高级示例 03 |
| `github-reference-keywords-common.md` | GitLab 与 GitHub 常用关键词映射 | [12_GitHub_Actions与GitLab迁移](12_GitHub_Actions与GitLab迁移.md)、[13_Jenkins关键字与步骤速查](13_Jenkins关键字与步骤速查.md) |
| `github-reference-keywords-details.md` | workflow/job/step 级关键字 | [02_Pipeline语法与Jenkinsfile](02_Pipeline语法与Jenkinsfile.md)、[13_Jenkins关键字与步骤速查](13_Jenkins关键字与步骤速查.md) |
| `github-vs-gitlab-details.md` | 运行模型、image/container、rules、artifact、权限和复用差异 | [12_GitHub_Actions与GitLab迁移](12_GitHub_Actions与GitLab迁移.md) |
| `GitHub Actions 与 GitLab语法对比.md` | 语法、stage/needs、变量、artifact、cache、matrix、manual、schedule | [12_GitHub_Actions与GitLab迁移](12_GitHub_Actions与GitLab迁移.md)、中级示例 01-06 |
| `example.yml` | 完整 workflow、matrix、条件、summary、artifact、容器 | [02_Pipeline语法与Jenkinsfile](02_Pipeline语法与Jenkinsfile.md)、中级示例 03、高级示例 05 |
| `doc-links.txt` | 官方文档、Action、云部署、OIDC、调试工具索引 | 本目录各文档的“官方参考”和 [README](../README.md) |

## 2. 概念不应机械翻译

### Workflow 与 Pipeline

GitHub 的 Workflow 由事件启动，Jenkins 的 Pipeline 通常属于一个 Job/Build，触发器由 SCM/Branch Source/Job 配置和 `triggers` 组合。Jenkins 还具有 Controller、Agent、Executor、Queue 等平台运行时概念。

### Job 与 Stage

GitHub Job 是独立 Runner 上的执行单元，Jenkins Declarative Pipeline 的 `stage` 更接近可视化和控制边界。真正的 Jenkins Job 是外层任务；跨 Job 编排使用 `build`、Copy Artifact 或外部制品库。

### `needs` 与文件传递

Jenkins stage 顺序、`parallel` 和 `build` 表达控制流，但不会自动把 workspace 传给另一 Agent/Job。字符串、临时文件、正式制品、报告和缓存分别使用变量/返回值、`stash`、归档/制品库、报告插件和缓存机制。

### Action 与插件

Action 通常在 Runner 中执行；Jenkins 插件可能在 Controller 上加载并影响全实例。因此迁移时要先检查核心 Pipeline Step、Agent 镜像和 Shared Library，只有长期平台能力才安装插件。

### Context 与环境

Jenkins 没有一个统一的 Context 对象。`env`、`params`、`currentBuild`、SCM/Branch Source 变量和插件 API 的作用域不同，迁移时应定义显式数据契约，不能把所有 `${{ github.* }}` 简单替换为 `${env.*}`。

### 权限与 Secret

GitHub `permissions` 主要控制 `GITHUB_TOKEN` API scope；Jenkins 必须分别设计认证、RBAC、凭据使用权、Agent OS/容器权限、Kubernetes ServiceAccount 和云端 RBAC。任何可执行不可信 Shell 的 Job 都可能读取它所能接触的资源。

## 3. 生成的 Jenkins 学习范围

本目录在源资料之外补充了 Jenkins 必需的平台知识：

- Controller/Agent/Executor/Queue、Job 类型和插件管理。
- Declarative 与 Scripted Pipeline、CPS、Script Approval 和 Pipeline 重启。
- Multibranch Pipeline、Organization Folder、Fork/PR Trust。
- Folder/RBAC、Credentials Store、审计、JCasC 和 Job DSL。
- 动态 Kubernetes Agent、容器安全、云身份联邦和 GitOps 边界。
- 高可用/灾备、性能、Queue/Executor 指标和恢复演练。

## 4. 示例覆盖矩阵

| 示例 | 中心知识点 |
|---|---|
| 中级 01 | 基础 CI、测试报告、归档、stash、Python 环境 |
| 中级 02 | 参数、when、input、环境锁和部署隔离 |
| 中级 03 | parallel、matrix、排除组合、报告和容量 |
| 中级 04 | Docker、Registry、扫描、digest、凭据 |
| 中级 05 | GitHub PR、Multibranch、Fork 不可信代码 |
| 中级 06 | Helm/Kubernetes、kubeconfig、Lock、回滚 |
| 高级 01 | Shared Library、版本、API 契约和标准化 |
| 高级 02 | PR Preview、生产审批、动态环境和清理 |
| 高级 03 | 跨 Job、参数、Copy Artifact、下游结果和并行 |
| 高级 04 | SAST/SCA/Secret Scan、SBOM、Cosign、Quality Gate |
| 高级 05 | Kubernetes 动态 Pod、sidecar、ServiceAccount 和资源隔离 |
| 高级 06 | JCasC、Controller 基线、外部 Secret 和配置治理 |

## 5. 学习验证标准

完成本目录后，应能：

1. 从 SCM Webhook 触发 Multibranch Pipeline，并解释 Queue/Agent/Executor。
2. 写出包含超时、并发、凭据、测试报告、产物和清理的 Jenkinsfile。
3. 根据数据类型选择环境变量、参数、stash、archive、Copy Artifact 或制品库。
4. 为 PR、发布和生产环境设计不同的信任边界和权限。
5. 使用 Shared Library、JCasC、Job DSL 和插件构建可治理的平台。
6. 解释 Jenkins 与 GitHub Actions/GitLab 的非语法差异，并完成一次失败/取消/重试/回滚演练。
