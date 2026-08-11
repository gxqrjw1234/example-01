# 02. Pipeline 语法与 Jenkinsfile

## 1. 声明式和脚本式

### 声明式 Pipeline

声明式 Pipeline 以 `pipeline {}` 为根，结构固定、可读性好、适合团队治理、可视化和静态检查。生产项目优先使用声明式语法，只在确实需要动态逻辑时进入 `script {}`。

### 脚本式 Pipeline

脚本式以 `node {}` 为常见入口，Groovy 控制能力强，但更容易产生不可控的动态逻辑、序列化问题和脚本审批。适合复杂编排、共享库内部实现或历史迁移，不应把所有业务都写成一段自由脚本。

```groovy
// Scripted Pipeline 的最小形态
node('linux') {
    stage('Checkout') {
        checkout scm
    }
    stage('Test') {
        sh 'make test'
    }
}
```

## 2. 声明式 Pipeline 骨架

```groovy
pipeline {
    agent { label 'linux' }

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    }

    parameters {
        choice(name: 'TARGET', choices: ['test', 'staging'], description: '目标环境')
    }

    triggers {
        cron('H 2 * * 1-5')
    }

    environment {
        APP_ENV = 'ci'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Test') {
            when {
                anyOf {
                    branch 'main'
                    changeRequest()
                }
            }
            steps {
                sh 'make test'
            }
        }
    }

    post {
        always {
            junit testResults: 'reports/*.xml', allowEmptyResults: true
        }
        success {
            echo 'Pipeline succeeded'
        }
        failure {
            echo 'Pipeline failed'
        }
        cleanup {
            deleteDir()
        }
    }
}
```

## 3. 顶层关键指令

| 指令 | 作用 | 注意 |
|---|---|---|
| `agent` | 选择执行节点、Docker 或 Kubernetes Pod | `agent none` 后每个 stage 要有自己的 agent |
| `options` | 超时、日志、并发、保留策略、Pipeline durability | 先设置保护性选项，再按需增加 |
| `parameters` | 构建参数和手动输入 | 参数是用户输入，必须校验和白名单化 |
| `triggers` | cron、SCM polling、上游等 | GitHub/GitLab Webhook 通常在 Job/SCM 插件配置 |
| `environment` | 声明环境变量和凭据引用 | Secrets 仍需谨慎，尽量在最小 stage 绑定 |
| `tools` | 使用 Jenkins 全局工具配置 | 也可使用容器锁定工具链 |
| `stages` | 可视化的阶段层级 | 阶段应表达业务边界，不要每条命令都建 stage |
| `post` | 按结果执行收尾动作 | 报告和清理通常放 `always`/`cleanup` |

## 4. Stage 和 Steps

`stage` 是展示和控制边界，`steps` 是实际执行的 Pipeline Step：

```groovy
stage('Build') {
    steps {
        echo 'start build'
        sh(
            label: 'compile',
            script: './gradlew clean assemble'
        )
    }
}
```

常见内置步骤：`echo`、`error`、`unstable`、`readFile`、`writeFile`、`fileExists`、`dir`、`pwd`、`deleteDir`、`sleep`、`input`、`timeout`、`retry`、`catchError`、`parallel`、`stash`、`unstash`、`archiveArtifacts` 和 `junit`。完整参数以 Jenkins 的 Pipeline Steps Reference 和当前插件版本为准。

## 5. `script {}` 的边界

声明式 Pipeline 中的 `script {}` 允许执行 Groovy：

```groovy
stage('Calculate version') {
    steps {
        script {
            def version = sh(
                script: 'git describe --tags --always',
                returnStdout: true
            ).trim()
            env.APP_VERSION = version
            currentBuild.description = "version=${version}"
        }
    }
}
```

避免把整个 Pipeline 包进 `script`。这样会损失声明式语法校验、可视化和治理能力。动态逻辑应沉淀到共享库，并为输入、返回值和异常定义契约。

## 6. Agent 形式

```groovy
pipeline {
    agent none
    stages {
        stage('Linux') {
            agent { label 'linux' }
            steps { sh 'make test' }
        }
        stage('Docker') {
            agent {
                docker {
                    image 'python:3.12-slim'
                    reuseNode true
                }
            }
            steps { sh 'python --version' }
        }
    }
}
```

`docker` agent 需要 Docker Pipeline 插件和可用的 Docker daemon。对不可信代码不要挂载宿主机 Docker socket。Kubernetes 动态 Pod 的完整用法见 [07_Docker_Kubernetes与云部署](07_Docker_Kubernetes与云部署.md) 和高级示例。

## 7. `options` 常用项

- `timeout`：防止挂死任务占满 Executor。
- `timestamps`：便于把日志与外部系统对齐。
- `disableConcurrentBuilds()`：避免同一资源被并发修改；部署可配合 `lock`。
- `disableConcurrentBuilds(abortPrevious: true)`：新提交到来时取消旧 CI，适合可丢弃的分支构建。
- `buildDiscarder(logRotator(...))`：限制构建和制品保留，控制磁盘。
- `skipDefaultCheckout(true)`：多 Agent 或需要自定义 checkout 时显式 checkout。
- `preserveStashes(buildCount: 5)`：允许重启后重用限定数量的 stash，不能代替制品库。
- `parallelsAlwaysFailFast()`：并行分支失败时尽快停止其他分支。
- `durabilityHint('MAX_SURVIVABILITY')`：可靠性和性能之间取舍，按 Controller 资源和恢复要求选择。

## 8. Groovy 与 Shell 常见坑

- Groovy 字符串插值和 Shell 插值是两层解析；不要把 Secret 放到双引号 Groovy 字符串中。
- `sh(returnStatus: true, script: '...')` 返回整数；`returnStdout: true` 返回带换行的字符串，通常要 `.trim()`。
- Pipeline 变量可能被 CPS 序列化；不要在 `@NonCPS` 中调用 Pipeline Step。
- `env` 的值都是字符串；布尔参数用 `params.FLAG`，在 Shell 中显式比较。
- Windows 使用 `bat`/`powershell`，路径和退出码规则与 `sh` 不同。
- 对用户参数使用 `choice` 或白名单校验，避免把未经处理的参数拼入 Shell 命令。

## 9. 质量门槛

- 在提交前使用 Jenkins Pipeline Linter 或 `declarative-linter` 检查语法。
- 用 Pipeline Syntax Snippet Generator 查插件步骤参数，但最终把结果简化并版本化。
- 为共享库写单元测试；为关键 Pipeline 使用一个真实仓库执行端到端测试。
- 一个阶段只负责一个可解释的生命周期边界，并给失败信息加上清晰 label。
