# 示例 05：GitHub Pull Request 检查

## 文件

- Pipeline：[05_github-pr-ci.groovy](05_github-pr-ci.groovy)

## 难度

中级到中高级。对应 GitHub Actions 的 `pull_request`、上下文、检查状态、分支过滤和不可信 Fork 代码处理。

## 覆盖知识点

- GitHub Branch Source、Multibranch Pipeline、SCM checkout。
- `changeRequest()`、`changeRequest(target: 'main')` 和 `branch`。
- `CHANGE_ID`、`CHANGE_BRANCH`、`CHANGE_TARGET` 上下文变量。
- PR 检查、JUnit 报告、stash 和失败日志。
- PR Agent 隔离、无生产凭据和并发取消。

## 前置条件

1. 安装 GitHub Branch Source、Pipeline、Git、JUnit 和 Credentials Binding 插件。
2. 使用 GitHub App 或最小权限凭据配置 Organization Folder/Multibranch Pipeline。
3. 开启分支和 Pull Request 发现，配置 Fork Trust 策略。
4. Agent 标签 `linux-pr-untrusted` 不应挂载生产 Secret、Docker socket 或生产云身份。
5. 仓库提供 `scripts/lint.sh`、`scripts/test.sh`、`scripts/build.sh` 和测试报告路径。

## 关键实践

- 建议让 Branch Source 负责 Webhook 和 Checks/Commit Status。`githubPush()` 或第三方 PR Builder 触发器只在明确需要时使用，避免重复触发。
- PR 代码可修改 Jenkinsfile，因此不要让它在可信 Controller 或高权限 Agent 上运行。
- Jenkins 的 `changeRequest` 是 stage 条件，不等同于 GitHub Webhook 触发器；Job 是否发现 PR 由 Branch Source 配置决定。
- 需要发表评论、打标签或触发发布时，使用独立的可信后续 Job，并显式限制 API Token 权限。
