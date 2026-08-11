# 高级示例 01：版本化 Shared Library

## 文件

- Pipeline：[01_shared-library-pipeline.groovy](01_shared-library-pipeline.groovy)

## 覆盖知识点

- `@Library('name@version')` 固定共享库版本。
- Shared Library 的参数契约、输入校验和统一报告。
- 共享 CI、镜像构建/扫描、部署策略。
- 函数返回值传递镜像 digest，而不是共享工作区。
- 可信/不可信库边界、凭据最小化和 Jenkinsfile 可读性。
- 将团队标准沉淀为库，将服务差异保留在 Jenkinsfile 参数中。

## 前置条件

共享库 `company-ci` 至少提供以下全局步骤：

- `runStandardCi(Map)`：可选 checkout，执行 lint/test，校验 `testCommand`，生成报告。
- `buildAndScanImage(Map)`：构建、扫描并推送镜像，返回 `[digest: 'sha256:...']`。
- `deployWithPolicy(Map)`：校验环境、使用 kubeconfig、获取 Lock、按 digest 部署。
- `publishStandardReports(Map)`：在安全路径读取 JUnit/覆盖率报告。

建议结构：

```text
company-ci/
├── vars/runStandardCi.groovy
├── vars/buildAndScanImage.groovy
├── vars/deployWithPolicy.groovy
├── vars/publishStandardReports.groovy
├── src/org/example/ci/Validation.groovy
└── test/
```

## 使用方式

1. 在 Jenkins Global Shared Libraries 中添加 SCM 仓库 `company-ci`。
2. 为默认版本、允许的版本标签和检出凭据配置受控策略。
3. 先在预生产 Controller 验证 `v3.4.0`，再让项目引用。
4. 将 `.groovy` 内容作为仓库根目录 `Jenkinsfile`。

## 设计要点

- 生产不要引用 `@main` 或未审核的浮动分支；使用签名/保护的版本标签。
- 共享库必须对 `service`、命令、环境、凭据 ID 和路径做格式/白名单校验。
- `buildAndScanImage` 返回 digest，部署阶段只消费 digest，保证构建一次、发布多次。
- 共享库不要静默替换失败、自动访问任意 Credential ID 或绕过 `input`/RBAC。
- Pipeline Step 放在 `vars`，纯计算放在 `src`；不要在 `@NonCPS` 中调用 `sh`、`echo` 等 Step。
- 为共享库写 JenkinsPipelineUnit 测试，并至少在真实 Agent 上做一次端到端测试。
