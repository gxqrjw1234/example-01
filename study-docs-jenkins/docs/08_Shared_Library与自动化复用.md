# 08. Shared Library 与自动化复用

## 1. 复用层次

| 需求 | Jenkins 机制 | 适用范围 |
|---|---|---|
| 当前 Jenkinsfile 内复用 | Groovy 方法、`script` | 小范围、低复杂度 |
| 多仓库复用流水线逻辑 | Shared Library | 团队/组织级标准能力 |
| 封装复杂插件或外部系统 | 自定义插件 | 强类型、长期维护、需要平台能力 |
| 批量生成 Job | Job DSL / JCasC / Organization Folder | 平台配置和 Job 生命周期 |
| 复用工具链 | Docker/Pod Template、工具镜像 | Agent 运行时 |

GitHub Actions 的 Reusable Workflow、Composite Action、Marketplace Action，在 Jenkins 中通常分别对应 Shared Library、共享步骤/函数、插件或外部脚本。

## 2. Shared Library 目录

标准目录：

```text
(root)
├── vars/                         # 暴露为全局 Pipeline Step
│   ├── ciPipeline.groovy
│   └── ciPipeline.txt            # 可选帮助文档
├── src/org/example/ci/           # 普通 Groovy 类，建议纯函数/领域逻辑
│   └── Image.groovy
├── resources/                    # 模板、脚本和静态资源
│   └── org/example/ci/summary.md
└── test/                         # 单元测试（JenkinsPipelineUnit 等）
```

配置 Global Shared Library 时：

- 指定 SCM 仓库和默认版本。
- 生产库建议使用 `@Library('ci-library@v3.4.0')` 固定版本或受控分支。
- 可信库可调用更多 Jenkins 内部 API，必须由平台团队审查。
- 不可信库只使用安全的 Pipeline Step，避免任意文件/系统访问。

## 3. 全局变量步骤

`vars/ciPipeline.groovy`：

```groovy
def call(Map config = [:]) {
    def required = ['service', 'testCommand']
    required.each { key ->
        if (!config[key]) {
            error "ciPipeline requires '${key}'"
        }
    }

    pipeline {
        // vars/ 中的全局变量不能在任意位置嵌套完整 pipeline；
        // 更常见的方式是提供 stage 函数或返回配置。
    }
}
```

上面的片段故意展示接口校验，但不要在普通 `vars` Step 中随意嵌套完整 Declarative `pipeline {}`。更常见、可维护的模式是：

```groovy
// vars/runCi.groovy
def call(Map config = [:]) {
    if (!config.service || !config.testCommand) {
        error 'runCi requires service and testCommand'
    }

    stage('Checkout') {
        checkout scm
    }
    stage('Test') {
        sh(config.testCommand.toString())
    }
    stage('Archive') {
        archiveArtifacts artifacts: config.artifacts ?: 'dist/**', allowEmptyArchive: false
    }
}
```

Jenkinsfile：

```groovy
@Library('ci-library@v3.4.0') _

node('linux') {
    runCi(
        service: 'catalog',
        testCommand: './mvnw -B test',
        artifacts: 'target/*.jar'
    )
}
```

如果需要在声明式 Pipeline 中复用阶段，可让共享库返回配置、提供在 `steps` 中调用的函数，或使用 `src` 类辅助纯逻辑；不要让库隐藏关键审批、生产发布和凭据边界。

## 4. 普通 Groovy 类和 `@NonCPS`

`src/org/example/ci/Image.groovy`：

```groovy
package org.example.ci

class Image implements Serializable {
    private final String repository

    Image(String repository) {
        this.repository = repository
    }

    String tagFor(String commit) {
        if (!(commit ==~ /[0-9a-fA-F]{7,64}/)) {
            throw new IllegalArgumentException('invalid commit')
        }
        return "${repository}:${commit}"
    }
}
```

普通类不应直接调用 Pipeline Step。把 `sh`、`archiveArtifacts`、`withCredentials` 等 Step 保留在脚本层，通过闭包或接口注入，便于测试和避免 CPS 序列化问题。

`@NonCPS` 只用于不调用 Pipeline Step 且输入/输出可序列化的纯计算。不要把它当成“解决所有 Pipeline 问题”的注解。

## 5. 设计 Shared Library API

- 参数使用 `Map` 时先做必填、类型、格式和默认值校验。
- 使用枚举/白名单限制环境、区域、部署策略和凭据 ID。
- 失败信息包含服务名、阶段、版本和修复建议，但不包含 Secret。
- 不把平台策略隐藏在一个不可见的大函数中；关键边界仍在 Jenkinsfile 中可读。
- Shared Library 版本和变更日志可追踪，破坏性变更升级主版本。
- 把外部系统调用做成幂等，并设置超时、重试和可观测日志。

## 6. 共享库测试

推荐 JenkinsPipelineUnit 或等价测试框架：

```groovy
class RunCiTest extends BasePipelineTest {
    @Test
    void 'runs test and archives artifacts'() {
        helper.registerAllowedMethod('checkout', [Map], null)
        helper.registerAllowedMethod('sh', [Map], 'ok')
        helper.registerAllowedMethod('archiveArtifacts', [Map], null)

        runCi(service: 'catalog', testCommand: './mvnw test')

        assertThat(helper.callStack*.methodName,
            hasItems('checkout', 'sh', 'archiveArtifacts'))
    }
}
```

测试重点：参数校验、when-like 条件、失败传播、凭据闭包、重试/超时调用、产物路径和并行分支。测试不能替代一次真实 Jenkins Agent 上的端到端验证。

## 7. 共享库和安全

Jenkins Groovy Sandbox 会限制部分 API；管理员批准脚本签名会改变整个实例的能力边界。不要为了快速通过运行而批准宽泛签名。

- 普通项目库保持不受信任，使用沙箱和最少 Step。
- 平台可信库单独仓库、CODEOWNERS、强制评审、版本标签和审计。
- 禁止共享库读取任意凭据、遍历所有 Job 或修改 Jenkins 内部配置，除非确有平台需求。
- Pipeline 中调用库时固定版本，避免未审查的默认分支改变生产行为。

## 8. Job DSL、JCasC 和 Shared Library 的边界

- **Shared Library**：流水线执行逻辑。
- **Job DSL**：通过 Groovy 声明 Job、View、Trigger 和 SCM。
- **JCasC**：Controller 全局配置、凭据引用、授权、系统设置。
- **插件**：长期维护的平台能力和新的 Pipeline Step。

不要把所有配置塞进一个 Groovy 文件。用 Git 管理 JCasC/Job DSL/Shared Library，分别做变更审批和测试。

## 9. 本章检查清单

- [ ] 能解释 `vars`、`src`、`resources`、`test` 的作用。
- [ ] 能写一个参数校验明确的共享步骤。
- [ ] 能区分 Pipeline Step、普通 Groovy 类和 `@NonCPS`。
- [ ] 能为共享库设计版本、信任、测试和回滚策略。
- [ ] 能区分 Shared Library、Job DSL 和 JCasC 的职责。
