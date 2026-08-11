# 示例 02：参数、条件、审批和环境锁

## 文件

- Pipeline：[02_parameters-conditions.groovy](02_parameters-conditions.groovy)

## 难度

中级。对应 GitHub Actions 的 `workflow_dispatch.inputs`、`if`、Environment 审批和 `concurrency`。

## 覆盖知识点

- `parameters`：choice、boolean、string。
- 用户输入的白名单和正则校验。
- `when`、`anyOf`、表达式条件。
- `input` 人工审批、`timeout` 和 `submitter`。
- Lockable Resources 的环境互斥。
- `withEnv` 的临时变量和 `post` 收尾。
- dev/staging/prod 的 Agent 和权限隔离。

## 前置条件

- Agent 标签：`linux`、`linux-e2e`、`trusted-deployer`、`trusted-production-deployer`。
- Lockable Resources 插件，并创建 `deploy-dev`、`deploy-staging`、`production-environment`。
- 仓库提供 `scripts/test-unit.sh`、`scripts/test-e2e.sh`、`scripts/deploy.sh`。
- 生产审批组在 Jenkins RBAC 中配置为 `release-team`。

## 关键实践

- 手动参数只是输入入口，不是授权。真正的生产保护还需要 Folder/Job 权限、凭据最小授权、受信 Agent 和审计。
- 不把用户输入直接拼到 Groovy 或 Shell；先做白名单/格式校验，再通过环境变量传入脚本。
- 部署使用不可变的 `IMAGE_TAG`，脚本应该按 digest 发布而不是重新构建。
- 开发和生产使用不同 Agent/凭据/Lock，避免 PR 或普通 CI 触及生产资源。
- `disableConcurrentBuilds` 保护同一 Job；`lock` 保护跨 Job 共享环境，两者不要混为一谈。
