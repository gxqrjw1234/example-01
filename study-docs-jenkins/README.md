# Jenkins 学习资料

本目录根据 `study-docs-github-actions` 的知识点重新组织为 Jenkins 学习路线，重点覆盖 Jenkins LTS、Pipeline as Code、插件生态、凭据与权限、流水线编排、容器/云原生、共享库、可观测性和企业治理。

> 示例默认以 Linux Jenkins Agent 为主，命令使用 POSIX shell。Windows Agent 请把 `sh` 替换为 `bat` 或 `powershell`，并调整路径和虚拟环境命令。

## 学习顺序

0. [00_源资料分析与Jenkins映射](docs/00_源资料分析与Jenkins映射.md)
1. [01_架构安装与核心概念](docs/01_架构安装与核心概念.md)
2. [02_Pipeline语法与Jenkinsfile](docs/02_Pipeline语法与Jenkinsfile.md)
3. [03_触发器与SCM集成](docs/03_触发器与SCM集成.md)
4. [04_变量参数凭据与上下文](docs/04_变量参数凭据与上下文.md)
5. [05_控制流并发与错误处理](docs/05_控制流并发与错误处理.md)
6. [06_产物报告缓存与数据传递](docs/06_产物报告缓存与数据传递.md)
7. [07_Docker_Kubernetes与云部署](docs/07_Docker_Kubernetes与云部署.md)
8. [08_Shared_Library与自动化复用](docs/08_Shared_Library与自动化复用.md)
9. [09_安全权限与供应链](docs/09_安全权限与供应链.md)
10. [10_调试观测通知与运维](docs/10_调试观测通知与运维.md)
11. [11_高级架构多分支矩阵性能与高可用](docs/11_高级架构多分支矩阵性能与高可用.md)
12. [12_GitHub_Actions与GitLab迁移](docs/12_GitHub_Actions与GitLab迁移.md)
13. [13_Jenkins关键字与步骤速查](docs/13_Jenkins关键字与步骤速查.md)

## 示例

### 中级示例

每个示例都有一个可复制的流水线/配置文件和一个同名说明文件。说明文件列出插件、前置条件、知识点、运行方式和生产注意事项。

- [01_python-ci.groovy](examples/intermediate/01_python-ci.groovy) · [说明](examples/intermediate/01_python-ci.md)：Python CI、测试报告和归档
- [02_parameters-conditions.groovy](examples/intermediate/02_parameters-conditions.groovy) · [说明](examples/intermediate/02_parameters-conditions.md)：参数、环境、条件和审批
- [03_parallel-matrix.groovy](examples/intermediate/03_parallel-matrix.groovy) · [说明](examples/intermediate/03_parallel-matrix.md)：并行阶段和矩阵构建
- [04_docker-build-push.groovy](examples/intermediate/04_docker-build-push.groovy) · [说明](examples/intermediate/04_docker-build-push.md)：Docker 构建、Registry 和凭据
- [05_github-pr-ci.groovy](examples/intermediate/05_github-pr-ci.groovy) · [说明](examples/intermediate/05_github-pr-ci.md)：GitHub PR 检查与 Multibranch
- [06_kubernetes-deploy.groovy](examples/intermediate/06_kubernetes-deploy.groovy) · [说明](examples/intermediate/06_kubernetes-deploy.md)：Kubernetes 部署、锁和环境选择

### 高级示例

- [01_shared-library-pipeline.groovy](examples/advanced/01_shared-library-pipeline.groovy) · [说明](examples/advanced/01_shared-library-pipeline.md)：版本化 Shared Library
- [02_multibranch-preview.groovy](examples/advanced/02_multibranch-preview.groovy) · [说明](examples/advanced/02_multibranch-preview.md)：PR 预览环境与生产审批
- [03_cross-job-orchestration.groovy](examples/advanced/03_cross-job-orchestration.groovy) · [说明](examples/advanced/03_cross-job-orchestration.md)：下游 Job、参数、产物和结果传播
- [04_secure-supply-chain.groovy](examples/advanced/04_secure-supply-chain.groovy) · [说明](examples/advanced/04_secure-supply-chain.md)：SAST、镜像扫描、SBOM 和签名
- [05_kubernetes-pod-agent.groovy](examples/advanced/05_kubernetes-pod-agent.groovy) · [说明](examples/advanced/05_kubernetes-pod-agent.md)：Kubernetes 动态 Agent
- [06_jcasc-baseline.yaml](examples/advanced/06_jcasc-baseline.yaml) · [说明](examples/advanced/06_jcasc-baseline.md)：Jenkins Configuration as Code

## 快速开始

### 1. Jenkins 基线

- 使用受支持的 Jenkins LTS 和受支持的 Java 版本。
- 为 Controller 设置 `0` 个构建 Executor，把构建放到 Agent 上。
- 安装必要插件后重启并确认插件版本；不要在生产环境无审查地安装插件。
- 配置 HTTPS、时区、URL、备份和日志保留策略。
- 建议用 Multibranch Pipeline 读取仓库中的 `Jenkinsfile`，不要在 Web UI 中手工维护长脚本。

### 2. 最小 Pipeline Job

1. 创建 Pipeline 或 Multibranch Pipeline Job。
2. 在仓库根目录提交 `Jenkinsfile`，或暂时把示例内容粘贴到 Pipeline 定义中。
3. 将示例中的凭据 ID、Agent label、镜像仓库和命令替换为本环境值。
4. 第一次运行先在非生产分支验证；部署步骤使用受保护的环境和审批。

### 3. 常用插件类别

Git、Pipeline、Pipeline Stage View/Graph、Credentials Binding、GitHub Branch Source、GitLab Branch Source、Docker Pipeline、Kubernetes、JUnit、Warnings Next Generation、Email Extension、Slack Notification、Configuration as Code、Matrix Authorization 或 Role-based Authorization Strategy。插件不是越多越好，应按最小集合和维护状态选择。

## GitHub Actions 知识点映射

| GitHub Actions | Jenkins 学习位置 |
|---|---|
| Workflow / `on` | Pipeline、触发器、Multibranch |
| Job / `needs` | Stage、`parallel`、`matrix`、`build` |
| Step `run` / `uses` | `sh`、`bat`、Pipeline Step、插件 |
| `env` / `vars` / `secrets` | `environment`、`withEnv`、`params`、Credentials |
| Contexts | `env`、`params`、`currentBuild`、Multibranch 环境变量 |
| Outputs | `env`、返回值、文件、`stash/unstash`、归档产物 |
| Artifacts | `archiveArtifacts`、`stash`、Artifact Manager、`copyArtifacts` |
| Cache | 依赖缓存、Docker 层缓存、外部缓存服务 |
| Reusable workflow / Action | Shared Library、插件、Job DSL、JCasC |
| `if` / status functions | `when`、`post`、`catchError`、`currentBuild.result` |
| `permissions` | Jenkins 授权策略、Folder 权限、凭据最小权限 |
| Runner | Agent、Executor、云端动态 Agent |
| Debug logs / Summary | Console Log、System Log、JUnit/HTML 报告、通知 |

## 最佳实践总清单

- Jenkinsfile、共享库和 Job DSL 全部版本化，关键变更走代码评审。
- Controller 不执行构建；构建使用短生命周期、可复现、最小权限的 Agent。
- Secrets 只放 Credentials Store，使用 `withCredentials` 绑定；不要写入日志、参数、输出或归档文件。
- 生产发布使用不可变版本或镜像 digest，不使用 `latest`。
- 使用 `timeout`、`buildDiscarder`、`disableConcurrentBuilds`、时间戳和明确的 `post` 清理策略。
- 测试、构建、发布拆分权限和凭据；PR 来自 Fork 时按不可信代码处理。
- `stash` 只用于同一次运行的临时传递；长期产物放制品库或外部 Artifact Manager。
- 第三方插件和共享库固定版本并定期升级；升级前在预生产 Controller 验证。
- 不把 Jenkins 当作唯一制品库；备份 JENKINS_HOME、插件清单、JCasC 和外部依赖，并定期演练恢复。

## 官方参考

- Jenkins 用户文档：<https://www.jenkins.io/doc/>
- Pipeline Syntax：<https://www.jenkins.io/doc/book/pipeline/syntax/>
- Pipeline Steps Reference：<https://www.jenkins.io/doc/pipeline/steps/>
- Shared Libraries：<https://www.jenkins.io/doc/book/pipeline/shared-libraries/>
- Jenkins Configuration as Code：<https://github.com/jenkinsci/configuration-as-code-plugin>
- GitHub Branch Source：<https://github.com/jenkinsci/github-branch-source-plugin>
- Kubernetes Plugin：<https://plugins.jenkins.io/kubernetes/>
