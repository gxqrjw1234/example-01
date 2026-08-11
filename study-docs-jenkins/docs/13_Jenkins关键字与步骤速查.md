# 13. Jenkins 关键字与步骤速查

> 这是学习速查，不是固定的完整 API。Pipeline Step 来自 Jenkins 核心和插件，参数会随版本变化。以 Jenkins 的 Pipeline Syntax、Snippet Generator 和插件文档为准。

## 1. Declarative 顶层

| 关键字 | 作用 | 常见示例 |
|---|---|---|
| `pipeline` | 声明式 Pipeline 根节点 | `pipeline { ... }` |
| `agent` | 选择执行节点/容器 | `agent any`、`agent none`、`agent { label 'linux' }` |
| `options` | 超时、并发、日志、保留策略 | `timeout`、`timestamps`、`buildDiscarder` |
| `parameters` | 构建参数 | `choice`、`string`、`booleanParam` |
| `triggers` | 定时、轮询、上游 | `cron`、`pollSCM`、`upstream` |
| `environment` | Pipeline/stage 环境变量 | `APP_ENV = 'ci'` |
| `tools` | Jenkins 全局工具 | `jdk`、`maven`、`nodejs` |
| `stages` | 阶段集合 | `stage('Test') { ... }` |
| `post` | 结果后处理 | `always`、`failure`、`cleanup` |
| `when` | stage 条件 | `branch`、`changeRequest`、`expression` |
| `matrix` | 静态多轴构建 | `axes`、`excludes` |
| `input` | 人工确认/输入 | `message`、`submitter` |
| `parallel` | 并行 stage/分支 | `parallel { ... }` |

## 2. 常用 `options`

```groovy
options {
    skipDefaultCheckout(true)
    timestamps()
    timeout(time: 45, unit: 'MINUTES')
    disableConcurrentBuilds(abortPrevious: true)
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    preserveStashes(buildCount: 5)
    parallelsAlwaysFailFast()
}
```

| 选项 | 目的 |
|---|---|
| `skipDefaultCheckout` | 自定义 checkout 或多 Agent 时避免重复检出 |
| `timestamps` | 给 Console Log 添加时间戳 |
| `timeout` | 防止 Build 长时间占用资源 |
| `disableConcurrentBuilds` | 同一 Job 串行或取消旧 Build |
| `buildDiscarder` | 控制 Build/Artifact 保留 |
| `preserveStashes` | 有限的重启恢复 |
| `parallelsAlwaysFailFast` | 并行失败时快速停止 |
| `disableResume` | 明确接受 Jenkins 重启后不可恢复；谨慎使用 |

## 3. 常用 Pipeline Steps

### 文件和目录

```groovy
pwd()
dir('subdir') { sh 'pwd' }
fileExists('file.txt')
readFile('file.txt')
writeFile file: 'metadata.txt', text: 'version=1.0\n'
deleteDir()
```

### Shell 和工具

```groovy
sh 'make test'
sh(returnStatus: true, script: './check.sh')
def output = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
bat 'mvnw.cmd test'
powershell '$PSVersionTable'
```

### SCM

```groovy
checkout scm
git branch: 'main', credentialsId: 'git-read', url: 'https://git.example.com/a/b.git'
```

`checkout scm` 最适合 Multibranch/SCM Pipeline；手写 `git` 时要明确凭据、分支、深度和信任边界。

### 控制流

```groovy
timeout(time: 10, unit: 'MINUTES') { retry(2) { sh './test.sh' } }
catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') { sh './lint.sh' }
error('stop')
unstable('quality warning')
input(message: 'Continue?', submitter: 'release-team')
lock(resource: 'staging') { sh './deploy.sh' }
```

### 数据和产物

```groovy
stash name: 'dist', includes: 'dist/**'
unstash 'dist'
archiveArtifacts artifacts: 'dist/**', fingerprint: true
junit testResults: 'reports/**/*.xml', allowEmptyResults: false
copyArtifacts projectName: 'producer', selector: specific('42'), filter: 'dist/**'
```

### 凭据

```groovy
withCredentials([string(credentialsId: 'api-token', variable: 'API_TOKEN')]) {
    sh 'curl --fail-with-body -H "Authorization: Bearer $API_TOKEN" https://api.example.com/health'
}
```

Shell 中避免 Groovy 插值；凭据只在最小闭包内出现。

## 4. Declarative `when`

```groovy
when { branch 'main' }
when { buildingTag() }
when { changeRequest(target: 'main') }
when { changeset 'src/**' }
when { triggeredBy 'TimerTrigger' }
when { expression { return params.RUN_DEPLOY } }
when {
    allOf {
        branch 'main'
        not { changeRequest() }
    }
}
```

`when` 只决定 stage。整个 Job 是否被 SCM/Webhook/参数触发由 Job/Branch Source/Trigger 配置决定。

## 5. `post` 条件

```groovy
post {
    always { junit testResults: 'reports/*.xml', allowEmptyResults: true }
    success { echo 'success' }
    failure { echo 'failure' }
    unstable { echo 'unstable' }
    aborted { echo 'aborted' }
    changed { echo 'result changed' }
    cleanup { deleteDir() }
}
```

报告和清理要放在正确条件中。`cleanup` 放最后执行，适合删除临时文件和退出外部会话。

## 6. 环境变量和上下文

```groovy
environment { APP_ENV = 'ci' }
steps {
    withEnv(['FEATURE_X=true']) {
        echo "${env.JOB_NAME} #${env.BUILD_NUMBER}"
        echo "${params.TARGET_ENV}"
        echo "${currentBuild.currentResult}"
    }
}
```

| Jenkins | 说明 |
|---|---|
| `env.X` | 环境变量 |
| `params.X` | 构建参数 |
| `currentBuild.X` | Build 状态和元数据 |
| `scm` | 当前 Pipeline 的 SCM 定义 |
| `BRANCH_NAME` | Multibranch 分支 |
| `CHANGE_ID` | PR/MR 标识 |
| `TAG_NAME` | 标签构建 |

## 7. 触发器速查

```groovy
triggers {
    cron('H 2 * * *')
    pollSCM('H/15 * * * *')
    upstream(upstreamProjects: 'compile', threshold: hudson.model.Result.SUCCESS)
    githubPush()
}
```

GitHub/GitLab Webhook 和 Branch Source 触发器通常需要在 Job/SCM 插件中配置。Webhook 优先，轮询作为受限环境的后备方案。

## 8. Agent 速查

```groovy
agent any
agent none
agent { label 'linux-docker' }
agent { docker { image 'maven:3.9-eclipse-temurin-21' } }
```

Kubernetes Agent 通常由 Kubernetes Plugin 的 `podTemplate`/模板配置提供。Agent 必须按工具、资源和信任分层，不要默认所有项目共用一个拥有生产权限的节点。

## 9. 插件对应能力

| 能力 | 常见插件/机制 |
|---|---|
| Git/PR/分支发现 | Git、GitHub Branch Source、GitLab Branch Source |
| Pipeline | Pipeline、Pipeline: Declarative、Workflow Basic Steps |
| 凭据 | Credentials、Credentials Binding |
| Docker | Docker Pipeline、Docker API/Registry 插件 |
| Kubernetes | Kubernetes Plugin |
| 测试报告 | JUnit、Coverage、HTML Publisher |
| 质量/警告 | Warnings Next Generation、SonarQube Scanner |
| 通知 | Email Extension、Slack、Teams/Webhook |
| 权限 | Matrix Authorization、Role-based Authorization |
| 配置 | Configuration as Code、Job DSL |
| 制品复制 | Copy Artifact、外部 Nexus/Artifactory/S3/Azure Blob |
| 并发/资源 | Lockable Resources、Throttle Concurrent Builds |

插件名称只是类别提示，安装前核对当前 Jenkins LTS、插件版本、依赖、安全公告和维护状态。

## 10. 与 GitHub Actions 选择机制速记

```text
GitHub on                 → Jenkins SCM/Webhook/cron/triggers
GitHub job needs           → Jenkins stage 顺序/parallel/build
GitHub step run            → Jenkins sh/bat/powershell
GitHub uses Action         → Jenkins Plugin/Pipeline Step/Shared Library
GitHub env/vars            → Jenkins environment/withEnv/params
GitHub secrets             → Jenkins Credentials + withCredentials
GitHub job outputs         → Jenkins return value/env/metadata file
GitHub artifact            → Jenkins stash/archive/copyArtifacts/制品库
GitHub matrix              → Jenkins matrix/parallel
GitHub permissions          → Jenkins RBAC + Credential + Agent + 云 RBAC
GitHub environment review  → Jenkins input + RBAC + lock + 审计
```
