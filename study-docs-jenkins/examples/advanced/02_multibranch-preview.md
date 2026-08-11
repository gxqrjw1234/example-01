# 高级示例 02：Multibranch PR 预览环境

## 文件

- Pipeline：[02_multibranch-preview.groovy](02_multibranch-preview.groovy)

## 覆盖知识点

- Multibranch Pipeline 的 `changeRequest()`、`branch` 和 PR 元数据。
- PR 不可信 Agent 与受信部署 Agent 分离。
- 动态 namespace/URL 命名和输入验证。
- Helm 部署、滚动状态、Lockable Resources 和自动清理。
- 主分支生产审批、生产凭据和并发锁。
- `post { cleanup }` 的临时环境回收。

## 前置条件

- GitHub/GitLab Branch Source 已配置 PR/MR 发现和信任策略。
- Agent 标签：`linux-pr-untrusted`、`linux-docker-untrusted`、`trusted-preview-deployer`、`trusted-production-deployer`。
- Lockable Resources：`preview-cluster`、`production-cluster`。
- Credentials：`kubeconfig-preview`、`kubeconfig-production`，按 Folder/Job 最小授权。
- 仓库包含 `charts/catalog`、测试脚本和 Deployment `catalog`。
- PR 构建的镜像推送/部署需通过实际项目的可信后续流程完成；本示例的候选镜像命令仅用于说明隔离边界。

## 关键实践

- Jenkinsfile 来自 PR 分支，因此 PR 代码可以改变 Pipeline 逻辑；不能只依赖 `changeRequest()` 就认为它可信。
- `CHANGE_ID` 必须做数字校验；namespace、域名和镜像标签使用白名单生成，不能直接使用 PR 标题/分支名。
- Preview 清理由 `post cleanup` 触发，但如果 Jenkins/Agent 在清理前崩溃仍可能残留，应增加 TTL Controller 或定期回收任务。
- 生产发布使用受保护分支、独立 Agent、独立 kubeconfig、人工审批和资源锁。
- 生产真正部署应使用构建后保存的 digest，而不是让部署阶段用 `GIT_COMMIT` 重新猜测或重新构建。
- 真实项目可把 PR 评论和预览 URL 交给单独的可信 Job/API 集成，避免在不可信代码上下文中授予写权限。
