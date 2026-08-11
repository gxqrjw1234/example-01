# 高级示例 03：跨 Job 编排和产物传播

## 文件

- Pipeline：[03_cross-job-orchestration.groovy](03_cross-job-orchestration.groovy)

## 覆盖知识点

- `build` Step 触发下游 Job。
- `wait`、`propagate: false` 和显式结果检查。
- 上游参数传递和完整 SHA 校验。
- Copy Artifact 按明确 Build Number 复制产物。
- `fingerprintArtifacts`、`stash/unstash` 和下游部署。
- 动态 `parallel` 执行独立验证 Job。
- 构建摘要归档和失败传播。

## 前置条件

创建下列 Job，并让 `service-build` 归档这些文件：

```text
dist/**
release-metadata.properties
image-digest.txt
```

需要的 Job：

- `service-build`：构建一次并归档不可变制品。
- `service-unit-test`、`service-integration-test`：接受 `UPSTREAM_BUILD` 参数，测试指定产物。
- 当前 Job 的 Jenkins 账号需要触发和读取这些 Job 的权限。
- 安装 Pipeline Build Step、Copy Artifact、Pipeline Utility/基础 Pipeline 插件。

## 关键实践

- `needs`/stage 顺序不会自动共享 workspace；跨 Job 必须使用 Copy Artifact、外部制品库或参数。
- 生产不使用 `lastSuccessful()` 这类移动选择器；使用明确的 Build Number、commit SHA 或 digest。
- `propagate: false` 只用于让编排器收集结果；示例随后显式失败，不能把失败吞掉。
- 动态 parallel 闭包复制循环变量，避免所有分支引用同一个 Job 名。
- 下游 Job 接收的是经过校验的元数据，不要把原始用户输入直接变成 Shell 代码或 Job 名。
- 复杂系统可把制品坐标存放在 Nexus/Artifactory/Registry，Jenkins 只传递坐标和 provenance。
