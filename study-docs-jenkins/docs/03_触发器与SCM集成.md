# 03. 触发器与 SCM 集成

## 1. 触发器心智模型

GitHub Actions 的 `on`、GitLab 的 `rules`/Schedule，在 Jenkins 中拆成三层：

1. **SCM 事件**：代码 push、Pull Request、分支或标签更新。
2. **Jenkins Job 触发器**：Webhook、轮询、cron、上游 Job、手动参数化构建。
3. **Pipeline 内条件**：`when`、`input` 和 `post` 决定已启动的 Build 是否进入某个阶段。

触发 Build 不等于执行部署。部署还应检查分支、标签、变更来源、审批、凭据和并发锁。

## 2. GitHub 集成

推荐安装并配置 GitHub Branch Source 插件：

1. 在 GitHub 创建 App 或使用受限的访问凭据。
2. 在 Jenkins 创建 GitHub Organization Folder 或 Multibranch Pipeline。
3. 配置仓库发现、分支发现、Pull Request 发现和信任策略。
4. 在 GitHub Webhook 中发送 push、Pull Request 等事件，或让 GitHub App 管理 Webhook。
5. 让 Jenkins 从仓库读取 `Jenkinsfile`，由 Branch Source 自动创建和更新子 Job。

Branch Source 负责发现和 checkout，Pipeline 的 `when { changeRequest() }` 可识别 PR 构建。GitHub Checks/Status 通常由 Branch Source 或对应通知插件发布；不要在每个 Jenkinsfile 中重复实现一套状态 API。

### Fork PR 安全

来自 Fork 的 PR 是不可信代码：

- 默认不要向其暴露生产凭据、SSH 私钥或云长期密钥。
- 不要在拥有高权限凭据的 Controller 或静态 Agent 上执行未经审查的 Jenkinsfile。
- 使用 Branch Source 的 Trust 策略、受限 Agent、只读凭据和人工审核。
- 需要发布评论或合并的动作拆到可信的后续 Job。

## 3. GitLab 集成

GitLab Branch Source 插件可发现 Group/Project、分支和 Merge Request。也可以通过 GitLab Plugin 配置 Push/MR Webhook 和状态回写。配置时确认：

- Jenkins URL 可从 GitLab 访问，且使用 HTTPS。
- Webhook Token 或签名校验开启，不接受任意匿名请求。
- GitLab Token 只授予读取代码和需要的状态写权限。
- MR 来源分支的信任策略与 Fork PR 一样严格。

## 4. 其他触发方式

### 手动和参数化构建

`parameters {}` 定义输入，用户在 Build with Parameters 页面或 API 中提供值。手动输入是入口，不是审批；生产审批使用 `input` 和权限控制。

### 定时调度

```groovy
triggers {
    // H 让 Jenkins 在小时内分散任务，避免所有 Job 同时启动
    cron('H 3 * * 1-5')
}
```

避免所有任务使用 `0 0 * * *`。cron 使用 Jenkins 服务器时区；大规模实例优先使用 `H` 分散负载。

### SCM 轮询

```groovy
triggers {
    pollSCM('H/10 * * * *')
}
```

Webhook 可用时不要使用高频轮询。轮询会消耗 Controller 和 SCM API 配额；轮询只适合无法配置 Webhook 的受限网络。

### 上游 Job

```groovy
triggers {
    upstream(
        upstreamProjects: 'build-base-image',
        threshold: hudson.model.Result.SUCCESS
    )
}
```

复杂的参数和制品传递通常显式使用 `build` Step，而不是只依赖上游触发器。

### 外部系统

Generic Webhook Trigger、GitHub/GitLab API、Jenkins Remote Access API、定时器和消息队列都可以启动 Build。Webhook 接收端必须校验签名/Token、限制来源、限制请求体大小并记录审计。

## 5. 分支、标签、路径过滤

### 分支和 PR

在 Multibranch 配置中做分支发现，在 Jenkinsfile 中做执行条件：

```groovy
when {
    anyOf {
        branch 'main'
        branch pattern: 'release-*', comparator: 'GLOB'
        changeRequest target: 'main'
    }
}
```

### 标签

```groovy
when {
    buildingTag()
}
```

也可以用 `tag pattern: 'v*', comparator: 'GLOB'`。生产发布使用已签名、不可变的版本标签，并验证标签指向的提交。

### 路径变更

Declarative Pipeline 可以使用 `changeset`：

```groovy
when {
    changeset pattern: 'infra/**', comparator: 'GLOB'
}
```

这与 GitHub Actions 的 workflow 级 `paths` 不完全相同：Jenkins Job 可能已经启动，只是跳过某个 stage。若要阻止整个 Build，需要在 SCM/分支策略、共享库或早期脚本中实现，并保留清晰的原因。

## 6. 触发去重与幂等

同一次 push 可能同时触发分支构建、PR 构建和外部 Webhook。最佳实践：

- 统一由一种 SCM 集成负责 Webhook，避免重复触发器。
- 使用 `disableConcurrentBuilds` 或 `concurrency` 类插件控制同一资源。
- 部署操作必须幂等，重试不能创建重复资源或覆盖错误环境。
- 用提交 SHA、镜像 digest 和环境名作为发布记录的唯一键。
- 取消旧 CI 时使用 `abortPrevious`，但生产部署通常排队而不是取消。

## 7. 触发器排错

1. 看 SCM Webhook 的投递记录、HTTP 状态和 Jenkins 系统日志。
2. 检查 Job 是否被禁用、分支是否被发现、Jenkinsfile 路径是否正确。
3. 检查 Queue 中是否因标签、Executor 或凭据等待。
4. 区分“未触发”“已触发但 skipped”“已运行但 stage 失败”。
5. 检查时区、`H` 计算、Webhook 重试和 GitHub/GitLab API 速率限制。

## 8. 本章检查清单

- [ ] 能为 GitHub 或 GitLab 配置安全的 Multibranch Webhook。
- [ ] 能解释 `cron`、`pollSCM`、上游触发器和手动参数构建的差异。
- [ ] 能用 `when` 实现分支、PR、标签和变更集过滤。
- [ ] 能说明为什么 Fork PR 不应访问生产 Secrets。
- [ ] 能检查重复触发、Webhook 签名、Queue 和 SCM API 限流。
