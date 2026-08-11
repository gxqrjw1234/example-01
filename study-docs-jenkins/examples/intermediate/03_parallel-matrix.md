# 示例 03：并行检查与矩阵测试

## 文件

- Pipeline：[03_parallel-matrix.groovy](03_parallel-matrix.groovy)

## 难度

中级到中高级。对应 GitHub Actions 的 `parallel` job、`strategy.matrix`、`fail-fast` 和并发控制。

## 覆盖知识点

- `agent none` 和 stage 级 Agent。
- 静态 `parallel`：lint、unit test、安全审计同时执行。
- Declarative `matrix`：Python 版本 × 数据库轴。
- `excludes` 排除不支持组合。
- `stash/unstash` 在不同 Agent 间传递源码。
- `catchError`、`parallelsAlwaysFailFast` 和 JUnit 报告。
- 并行工作区、报告路径和 Executor 容量的设计。

## 前置条件

- 标签：`linux`、`linux-python`、`linux-security`、`linux-matrix`。
- `ruff`、`pytest`、`pip-audit` 和项目所需 Python 版本已在 Agent/镜像中提供。
- 仓库存在 `tests/unit`、`scripts/test-integration.sh`，脚本接受 Python 版本和数据库名参数。
- 如果没有 Postgres 测试服务，需要在 Agent 镜像、Pod sidecar 或外部测试环境中提供。

## 关键实践

- 并行会消耗真实 Executor；在 Jenkins 全局、节点标签、Throttle 插件或 Kubernetes request/limit 上设置容量上限。
- 每个分支应使用独立工作区或在开始时 `deleteDir()`，避免并行写文件互相覆盖。
- 轴值只是数据，不应直接当作可执行 Shell 片段；必要时先映射到白名单命令。
- `stash` 适合小型源码和同一次 Build；大型源码/产物请使用外部存储。
- `catchError` 只能用于明确的非阻断检查；最终质量门禁仍应阻止不合格发布。
