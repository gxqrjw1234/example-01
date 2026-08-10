# GitHub Actions vs GitLab CI/CD 关键词对照

面向 GitLab CI/CD 熟手、GitHub Actions 初学者。

> GitHub Actions 和 GitLab CI/CD 不是完全同构的配置模型。下表中的“对应”分为：
> - **直接对应**：概念和作用基本一致。
> - **近似对应**：可以实现相似效果，但作用范围或行为不同。
> - **无直接等价物**：需要组合多个能力，或只能通过脚本、Action 或平台设置实现。
>
> 参考文档：
> - [GitHub Actions workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
> - [GitLab CI/CD YAML syntax](https://docs.gitlab.com/ci/yaml/)

## 1. 文件和整体模型

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `.github/workflows/*.yml` | `.gitlab-ci.yml` | 近似对应 | GitHub 通常按 CI、CD、定时任务拆分多个 workflow；GitLab 常见单入口配置文件。 |
| `workflow` | `pipeline` | 近似对应 | GitHub workflow 由事件触发；GitLab pipeline 由配置和 pipeline source 触发。 |
| `jobs` | jobs | 直接对应 | 两者都由多个 job 组成。 |
| `jobs.<job_id>` | 自定义 job 名称 | 直接对应 | job ID 用于依赖、输出和状态引用。 |
| `steps` | `script`、`before_script`、`after_script` | 近似对应 | GitHub 将 job 拆成有顺序的 steps；GitLab 常用脚本列表表达执行过程。 |
| `name` | pipeline/job 显示名称 | 近似对应 | GitHub 的 `name` 是 workflow 名称；job 还可以有自己的 `name`。 |
| `run-name` | 无直接等价物 | 无直接等价物 | GitHub 可为每次 run 动态设置显示名称。 |

## 2. Workflow 触发和 GitLab 全局配置

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `on` | `workflow`、`rules`、`only/except` | 近似对应 | GitHub 的 `on` 主要决定 workflow 是否启动；GitLab 的 `workflow:rules` 可控制 pipeline 是否创建。 |
| `on.<event_name>.types` | `rules` 中的 pipeline/MR 条件 | 近似对应 | GitHub 按事件 activity type 过滤；GitLab 通常按 predefined variables 判断。 |
| `on.push.branches` | `rules`、`only` | 近似对应 | 两者都可按分支过滤，但 GitHub 位于 workflow 事件层。 |
| `on.push.tags` | `rules`、`only: tags` | 近似对应 | 两者都可按 tag 触发。 |
| `branches-ignore` / `tags-ignore` | `except` 或 `rules` | 近似对应 | GitHub 与 GitLab 的过滤层级不同。 |
| `on.*.paths` / `paths-ignore` | `rules:changes` | 近似对应 | GitHub 通常影响整个 workflow；GitLab `changes` 可用于 job 规则。 |
| `on.schedule` | Pipeline Schedules | 近似对应 | GitHub cron 写在仓库 YAML 中；GitLab 常在平台页面配置 Schedule。 |
| `on.workflow_dispatch` | `when: manual`、手动 pipeline | 近似对应 | `workflow_dispatch` 是手动启动 workflow；`when: manual` 通常是 pipeline 中的手动 job。 |
| `on.workflow_dispatch.inputs` | pipeline variables、manual job variables | 近似对应 | 两者都支持手动输入，但 UI 和作用范围不同。 |
| `on.workflow_call` | `include`、`trigger`、模板复用 | 近似对应 | GitHub 用于调用 reusable workflow；GitLab 通常组合 include、extends 或 downstream pipeline。 |
| `on.workflow_run` | `trigger`、pipeline source 条件 | 近似对应 | GitHub 可在另一个 workflow 完成后触发；GitLab 常用 downstream pipeline 或 API。 |
| `permissions` | protected variables、job token、项目权限设置 | 无直接等价物 | GitHub 直接控制 `GITHUB_TOKEN` scope；GitLab 权限更多由项目、角色、Runner 和 token 模型决定。 |
| `env` | `variables` | 直接对应 | GitHub 支持 workflow/job/step 级；GitLab 支持全局和 job 级变量。 |
| `defaults` | `default` | 近似对应 | 两者都能设置默认值，但可配置字段不同。 |
| `defaults.run` | `default.before_script`、全局脚本配置 | 近似对应 | GitHub 主要设置 shell 和 working-directory；GitLab default 可设置更多 job 默认关键字。 |
| `defaults.run.shell` | `default`、Runner shell 配置 | 近似对应 | 都可以影响脚本使用的 shell，但配置入口不同。 |
| `defaults.run.working-directory` | `before_script: cd` 或 job 脚本 | 无直接等价物 | GitLab 没有完全一致的通用 working-directory 字段。 |
| `concurrency` | `resource_group`、`interruptible` | 近似对应 | GitHub 用 concurrency group 排队或取消；GitLab 分别控制资源互斥和冗余 job 取消。 |

## 3. Job 执行、依赖和失败控制

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `jobs.<job_id>.needs` | `needs` | 直接对应 | 都用于表达 job 依赖和 DAG。GitHub 的 `needs` 不会自动传 artifact。 |
| `jobs.<job_id>.if` | `rules`、`only/except`、`when` | 近似对应 | GitHub job 已进入 workflow 后再判断是否执行；GitLab `rules` 还决定 job 是否创建。 |
| `jobs.<job_id>.runs-on` | `tags` | 近似对应 | GitHub 选择 hosted runner 或 labels；GitLab tags 选择 Runner。 |
| `jobs.<job_id>.timeout-minutes` | `timeout` | 直接对应 | 两者都支持 job 级超时，单位和默认值处理不同。 |
| `jobs.<job_id>.continue-on-error` | `allow_failure` | 近似对应 | 两者都允许失败不阻断整体流程，但状态传播行为并不完全相同。 |
| `jobs.<job_id>.strategy` | `parallel` | 近似对应 | GitHub strategy 统一处理 matrix、fail-fast 和并发；GitLab parallel 支持数量和 matrix。 |
| `jobs.<job_id>.strategy.matrix` | `parallel:matrix` | 直接对应 | 都能生成多组参数化 job。 |
| `strategy.matrix.include` | `parallel:matrix` 额外组合 | 近似对应 | GitHub 可追加组合和变量；GitLab 语法和组合规则不同。 |
| `strategy.matrix.exclude` | `parallel:matrix` 排除组合 | 近似对应 | 两者都能排除矩阵组合，但表达方式不同。 |
| `strategy.fail-fast` | `interruptible`、失败策略 | 无直接等价物 | GitHub 控制矩阵失败后是否取消其他组合；GitLab 没有完全相同的矩阵字段。 |
| `strategy.max-parallel` | Runner 并发、项目并发限制 | 无直接等价物 | GitHub 在 workflow matrix 内限制并行数；GitLab 通常依赖 Runner 或项目级并发设置。 |
| `jobs.<job_id>.continue-on-error` | `allow_failure` | 近似对应 | 推荐明确区分 job 级容错和 step 级容错。 |
| `jobs.<job_id>.steps[*].continue-on-error` | `allow_failure` 或脚本 `|| true` | 近似对应 | GitHub 可直接在 step 级配置；GitLab 常在脚本中处理。 |
| `jobs.<job_id>.steps[*].timeout-minutes` | `timeout` 或脚本超时 | 近似对应 | GitHub 支持 step 级 timeout；GitLab 主要是 job 级 timeout。 |

## 4. 环境变量、输出和数据传递

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| workflow `env` | 全局 `variables` | 直接对应 | 为所有相关 jobs/steps 提供环境变量。 |
| job `env` | job 级 `variables` | 直接对应 | 只作用于当前 job。 |
| step `env` | script 前的变量导出、job variables | 近似对应 | GitHub 可直接限制到单个 step。 |
| `jobs.<job_id>.outputs` | dotenv artifact、pipeline/job variables | 近似对应 | GitHub 适合传少量字符串；GitLab 常用 dotenv artifact。 |
| `$GITHUB_OUTPUT` | `echo KEY=value >> file`、dotenv 文件 | 近似对应 | GitHub step 输出需要先写入 `$GITHUB_OUTPUT`。 |
| `$GITHUB_ENV` | `export`、dotenv artifact | 近似对应 | GitHub 只影响当前 job 后续 steps，不会自动跨 job。 |
| `needs.<job_id>.outputs.<name>` | dotenv 变量、依赖 job 变量 | 近似对应 | GitHub 下游 job 通过 `needs` 读取上游 job outputs。 |
| `secrets` | `secrets`、protected CI/CD variables | 近似对应 | 两者都支持敏感配置，但权限、继承和保护规则不同。 |
| `vars` | 非敏感 CI/CD variables | 近似对应 | GitHub 的仓库/环境变量与 GitLab variables 作用域不同。 |
| `GITHUB_TOKEN` | `CI_JOB_TOKEN` | 近似对应 | 用途相近，但权限模型和 API scope 不同。 |
| `github.*` contexts | predefined variables，如 `$CI_COMMIT_SHA` | 近似对应 | GitHub 使用 contexts 和表达式；GitLab 使用预定义环境变量。 |

## 5. Artifacts、Cache 和 Pages

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `actions/upload-artifact` | `artifacts` | 近似对应 | GitHub 必须显式上传 artifact。 |
| `actions/download-artifact` | `dependencies`、默认 artifact 传递 | 近似对应 | GitHub 必须显式下载；`needs` 本身不传文件。 |
| artifact retention 配置 | `expire_in` | 近似对应 | 两者都能设置产物保留时间。 |
| `actions/cache` | `cache` | 直接对应 | 两者主要用于加速依赖或中间文件，不应替代正式 artifact。 |
| `actions/upload-pages-artifact` | `pages` job | 近似对应 | 都用于准备 Pages 部署内容。 |
| `actions/deploy-pages` | GitLab Pages 发布机制 | 近似对应 | GitHub 通常拆成上传 artifact 和部署 action；GitLab 常由 Pages job 约定完成。 |

## 6. Runner、镜像、容器和服务

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `jobs.<job_id>.runs-on` | `tags` | 近似对应 | 选择执行节点。 |
| `jobs.<job_id>.container` | `image` | 近似对应 | GitLab `image` 更接近 GitHub job `container`，不是 `runs-on`。 |
| `jobs.<job_id>.container.image` | `image.name` 或 `image` | 近似对应 | 指定 job 使用的容器镜像。 |
| `container.credentials` | `image` 私有 registry 凭据 | 近似对应 | 都可支持私有镜像认证。 |
| `container.env` | job variables | 近似对应 | 设置 job 容器环境变量。 |
| `container.ports` | image/service 网络配置 | 无直接等价物 | GitHub 明确声明 job 容器端口；GitLab 依赖容器网络和服务配置。 |
| `container.volumes` | volumes、Runner 配置 | 无直接等价物 | 两者挂载模型不同。 |
| `container.options` | Docker Runner 参数 | 无直接等价物 | GitHub 可传 Docker 参数；GitLab 通常由 Runner executor 配置。 |
| `jobs.<job_id>.services` | `services` | 直接对应 | 都用于提供数据库、缓存等服务容器。 |
| `services.<service_id>.image` | `services` image | 直接对应 | 指定服务镜像。 |
| `services.<service_id>.credentials` | 私有 service registry 凭据 | 近似对应 | 用于拉取私有服务镜像。 |
| `services.<service_id>.env` | service/job `variables` | 近似对应 | 设置服务容器环境变量。 |
| `services.<service_id>.ports` | service alias/network | 近似对应 | GitHub 显式端口映射；GitLab 常通过服务别名访问。 |
| `services.<service_id>.volumes` | Runner volumes | 无直接等价物 | 容器卷配置方式不同。 |
| `services.<service_id>.options` | service health check / Docker options | 近似对应 | GitHub 可配置健康检查和 Docker 参数。 |
| `services.<service_id>.command` | service command | 近似对应 | 覆盖服务默认命令。 |
| `services.<service_id>.entrypoint` | service entrypoint | 近似对应 | 覆盖服务默认入口。 |

## 7. Step、Action 和 GitLab 脚本

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `jobs.<job_id>.steps` | `before_script` + `script` + `after_script` | 近似对应 | GitHub steps 是一等结构；GitLab 通过多个脚本段组成 job。 |
| `steps[*].id` | 脚本变量、job 名称 | 无直接等价物 | GitHub 用 step ID 引用 outputs。 |
| `steps[*].if` | `rules`、脚本条件 | 近似对应 | GitHub 可在单个 step 上条件执行。 |
| `steps[*].name` | script 注释、job log | 近似对应 | GitHub 在 UI 中显示 step 名称。 |
| `steps[*].uses` | `include`、模板、公共脚本 | 近似对应 | GitHub 调用 Action；GitLab 通常复用配置或脚本。 |
| `steps[*].run` | `script` | 直接对应 | 都执行 shell 命令。 |
| `steps[*].working-directory` | `cd`、脚本工作目录 | 近似对应 | GitHub 直接指定 step 工作目录。 |
| `steps[*].shell` | Runner shell、`image` | 近似对应 | GitHub step 可指定 shell；GitLab 更多由 Runner/executor 决定。 |
| `steps[*].with` | include/template variables、脚本变量 | 近似对应 | GitHub 向 Action 传输入参数。 |
| `steps[*].with.args` | Docker command arguments | 近似对应 | 为 Docker Action 传命令行参数。 |
| `steps[*].with.entrypoint` | Docker entrypoint 配置 | 近似对应 | 覆盖 Docker Action 的入口。 |
| `steps[*].env` | job variables、脚本导出 | 近似对应 | 设置单个 step 环境变量。 |
| `steps[*].timeout-minutes` | job `timeout` | 近似对应 | GitHub 可精确控制 step 超时。 |
| `steps[*].background` | 后台脚本、服务、异步命令 | 无直接等价物 | 需要结合脚本或服务机制实现。 |
| `steps[*].wait` | 脚本等待逻辑 | 无直接等价物 | 通常通过 shell 循环或健康检查实现。 |
| `steps[*].wait-all` | 多进程等待逻辑 | 无直接等价物 | 通常由脚本管理后台任务。 |
| `steps[*].cancel` | `interruptible`、取消策略 | 无直接等价物 | GitHub 该类能力应以当前官方语法为准。 |
| `steps[*].parallel` | `parallel` | 近似对应 | GitLab 的 parallel 主要是 job 级；GitHub 通常用 matrix。 |

## 8. Reusable Workflow 与 GitLab 复用

| GitHub Actions | GitLab CI/CD | 对应关系 | 说明 |
|---|---|---|---|
| `jobs.<job_id>.uses` | `include`、`trigger` | 近似对应 | GitHub 在 job 级调用 reusable workflow。 |
| `jobs.<job_id>.with` | trigger variables、模板 variables | 近似对应 | 向 reusable workflow 传普通输入。 |
| `jobs.<job_id>.with.<input_id>` | trigger variable | 近似对应 | 传递指定输入值。 |
| `jobs.<job_id>.secrets` | CI/CD variables、trigger secrets | 近似对应 | 向可复用流程传递敏感值。 |
| `jobs.<job_id>.secrets.inherit` | 变量继承、父子 pipeline 变量 | 近似对应 | GitHub 可以继承调用方 secrets，但边界不同。 |
| `jobs.<job_id>.secrets.<secret_id>` | 指定变量/secret | 近似对应 | 显式传递某个 secret。 |
| Composite Action | YAML anchor、`extends`、公共脚本 | 近似对应 | 复用多个 steps。 |
| Marketplace Action | GitLab CI template、社区组件 | 近似对应 | 第三方生态组件，必须审查来源和权限。 |

## 9. GitLab 独有或 GitHub 无直接字段的能力

| GitLab keyword | GitHub Actions 实现方式 | 说明 |
|---|---|---|
| `default` | `defaults`、workflow/job `env`、复用 Action | GitHub 的默认配置字段集合不同。 |
| `include` | Reusable Workflow、Composite Action、Marketplace Action | 需要根据复用粒度选择机制。 |
| `stages` | `needs` DAG | GitHub 没有必须声明 stage 顺序的顶层字段。 |
| `artifacts` | `upload-artifact` + `download-artifact` | 文件传递需要显式完成。 |
| `dependencies` | `needs` + `download-artifact` | `needs` 本身不下载 artifact。 |
| `coverage` | 测试工具输出、第三方报告 Action、Job Summary | GitHub 没有完全等价的内置 job keyword。 |
| `dast_configuration` | CodeQL、第三方 DAST/SAST Action | 依赖安全工具或平台集成。 |
| `identity` | OIDC：`id-token: write` + 云登录 Action | GitHub 使用身份 token 和云厂商 federation。 |
| `inherit` | `env`、`permissions`、reusable workflow 参数 | 需要显式定义继承范围。 |
| `manual_confirmation` | `workflow_dispatch` inputs、environment reviewers | 手动输入与审批需要分开设计。 |
| `release` | GitHub Release Action 或 GitHub CLI | 通常使用 `gh release create` 或 Release Action。 |
| `resource_group` | `concurrency.group` | 都能限制同一资源并发，但锁定语义不同。 |
| `retry` | shell 重试循环或 retry Action | GitHub 没有通用 job-level retry keyword。 |
| `rules` | `on` + job/step `if` | GitHub 将 workflow 触发和 job/step 条件分层。 |
| `script` | `run` | GitHub 通过 step 级 `run` 执行命令。 |
| `start_in` + `when: delayed` | shell `sleep`、定时 workflow、外部调度 | GitHub 没有完全等价的延迟 job 字段。 |
| `tags` | `runs-on` labels | 两者都选择 Runner，但标签管理方式不同。 |
| `trigger` | reusable workflow、`workflow_run`、repository dispatch、API | 根据是否复用、串联或跨流程触发选择方案。 |
| `when` | `on`、`if`、`environment`、`always()` | GitHub 将运行时机拆成多个机制。 |
| `pages` | Pages artifact + deploy-pages | GitHub Pages 通常显式上传和部署。 |

## 10. 最重要的迁移结论

| GitLab 心智模型 | GitHub Actions 心智模型 |
|---|---|
| `pipeline -> stages -> jobs` | `event -> workflow -> jobs DAG -> steps/actions` |
| `image` 选择 job 环境 | `runs-on` 选择宿主 runner，`container` 选择 job 容器 |
| `rules` 同时影响 job 是否创建和执行 | `on` 控制 workflow 启动，`if` 控制 job/step 执行 |
| `dependencies` 获取上游 artifacts | `needs` 控制依赖，artifact 需要显式 upload/download |
| dotenv 传递变量 | job `outputs` 传递字符串，`GITHUB_ENV` 只影响当前 job 后续 steps |
| `when: manual` 常表示 pipeline 中的手动 job | `workflow_dispatch` 是手动入口，environment protection 才是审批 |
| CI/CD variables 较集中 | `env`、`vars`、`secrets`、contexts、`GITHUB_TOKEN` 分工更细 |
| Runner tags 选择执行节点 | `runs-on` 和 self-hosted labels 选择执行节点 |

## 11. 速记

```text
GitLab pipeline       ≈ GitHub workflow
GitLab stage          ≈ GitHub needs DAG 中的一层（不是字段）
GitLab image          ≈ GitHub container
GitLab script         ≈ GitHub step.run
GitLab artifacts      ≈ GitHub upload-artifact/download-artifact
GitLab dependencies   ≈ GitHub needs + download-artifact
GitLab dotenv         ≈ GitHub job outputs
GitLab variables      ≈ GitHub env / vars / secrets
GitLab rules          ≈ GitHub on + if
GitLab trigger        ≈ GitHub workflow_call / workflow_run / dispatch
```
