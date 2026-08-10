# GitHub Actions 工作流关键字速查

> 本表将 GitHub Actions 工作流语法按原有组织结构简化为 `Keyword` 和 `Summary` 两列。
> 官方参考：[Workflow syntax for GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)

## Workflow-level keywords

| Keyword | Summary |
|---|---|
| `name` | 设置 workflow 在 Actions 页面显示的名称。 |
| `run-name` | 设置每次 workflow run 的显示名称，支持表达式。 |
| `on` | 定义触发 workflow 的事件、分支、标签、路径、计划和手动输入。 |
| `on.<event_name>.types` | 限定事件的 activity type，例如 issue 的 `opened` 或 `labeled`。 |
| `on.<pull_request\|pull_request_target>.<branches\|branches-ignore>` | 按 Pull Request 的目标分支过滤 workflow。 |
| `on.push.<branches\|tags\|branches-ignore\|tags-ignore>` | 按 push 事件涉及的分支或标签过滤 workflow。 |
| `on.<push\|pull_request\|pull_request_target>.<paths\|paths-ignore>` | 按变更文件路径过滤 workflow。 |
| `on.schedule` | 使用 POSIX cron 按计划触发 workflow。 |
| `on.workflow_call` | 声明可复用 workflow 的调用入口。 |
| `on.workflow_call.inputs` | 声明可复用 workflow 接收的输入参数。 |
| `on.workflow_call.inputs.<input_id>.type` | 设置可复用 workflow 输入的类型：`boolean`、`number` 或 `string`。 |
| `on.workflow_call.outputs` | 声明可复用 workflow 对调用方暴露的输出。 |
| `on.workflow_call.secrets` | 声明可复用 workflow 接收的 secrets。 |
| `on.workflow_call.secrets.<secret_id>` | 定义一个可复用 workflow secret 的说明和要求。 |
| `on.workflow_call.secrets.<secret_id>.required` | 指定可复用 workflow secret 是否必需。 |
| `on.workflow_run.<branches\|branches-ignore>` | 按分支过滤由另一个 workflow run 触发的 workflow。 |
| `on.workflow_dispatch` | 启用手动触发 workflow。 |
| `on.workflow_dispatch.inputs` | 为手动触发页面定义输入参数。 |
| `on.workflow_dispatch.inputs.<input_id>.required` | 指定手动输入是否必填。 |
| `on.workflow_dispatch.inputs.<input_id>.type` | 设置手动输入类型，例如 `boolean`、`choice`、`environment` 或 `string`。 |
| `permissions` | 为 `GITHUB_TOKEN` 设置 workflow 级默认权限。 |
| `env` | 定义 workflow 级环境变量。 |
| `defaults` | 设置 workflow 级默认配置。 |
| `defaults.run` | 设置所有 `run` step 的默认 shell 和工作目录。 |
| `defaults.run.shell` | 设置 `run` step 的默认 shell。 |
| `defaults.run.working-directory` | 设置 `run` step 的默认工作目录。 |
| `concurrency` | 限制同一并发组的运行数量，并可取消旧运行。 |

## Job-level keywords

| Keyword | Summary |
|---|---|
| `jobs` | 定义 workflow 中的 jobs 集合。 |
| `jobs.<job_id>` | 定义一个 job，并作为其他配置的作用域。 |
| `jobs.<job_id>.name` | 设置 job 在 Actions 页面显示的名称。 |
| `jobs.<job_id>.permissions` | 设置单个 job 的 `GITHUB_TOKEN` 权限。 |
| `jobs.<job_id>.needs` | 声明 job 依赖，使其等待指定 job 完成。 |
| `jobs.<job_id>.if` | 根据表达式决定 job 是否运行。 |
| `jobs.<job_id>.runs-on` | 指定 job 使用的 GitHub-hosted runner 或 self-hosted runner 标签。 |
| `jobs.<job_id>.snapshot` | 指定 job 使用的 snapshot 配置（按 GitHub 当前支持的 snapshot 能力使用）。 |
| `jobs.<job_id>.environment` | 指定 job 部署到的 environment，可关联 URL、secrets 和保护规则。 |
| `jobs.<job_id>.concurrency` | 为单个 job 设置并发组和取消策略。 |
| `jobs.<job_id>.outputs` | 声明供下游 jobs 通过 `needs` 读取的输出值。 |
| `jobs.<job_id>.env` | 定义 job 级环境变量。 |
| `jobs.<job_id>.defaults` | 设置 job 级默认配置。 |
| `jobs.<job_id>.defaults.run` | 设置该 job 中 `run` step 的默认 shell 和工作目录。 |
| `jobs.<job_id>.defaults.run.shell` | 设置该 job 的默认 shell。 |
| `jobs.<job_id>.defaults.run.working-directory` | 设置该 job 的默认工作目录。 |
| `jobs.<job_id>.steps` | 定义 job 按顺序执行的 steps。 |
| `jobs.<job_id>.timeout-minutes` | 设置 job 最大运行时间。 |
| `jobs.<job_id>.strategy` | 配置矩阵执行、失败策略和并行度。 |
| `jobs.<job_id>.strategy.matrix` | 定义矩阵变量，生成多个 job 副本。 |
| `jobs.<job_id>.strategy.matrix.include` | 向矩阵追加自定义组合或变量。 |
| `jobs.<job_id>.strategy.matrix.exclude` | 从矩阵中排除指定组合。 |
| `jobs.<job_id>.strategy.fail-fast` | 矩阵 job 失败时是否取消其他运行中的组合。 |
| `jobs.<job_id>.strategy.max-parallel` | 限制同时运行的矩阵 job 数量。 |
| `jobs.<job_id>.continue-on-error` | 允许 job 失败而不使 workflow 失败。 |
| `jobs.<job_id>.container` | 让 job 的 steps 在指定容器中运行。 |
| `jobs.<job_id>.container.image` | 指定 job 容器镜像。 |
| `jobs.<job_id>.container.credentials` | 为私有 job 容器镜像提供登录凭据。 |
| `jobs.<job_id>.container.env` | 设置 job 容器的环境变量。 |
| `jobs.<job_id>.container.ports` | 暴露 job 容器端口。 |
| `jobs.<job_id>.container.volumes` | 为 job 容器挂载卷。 |
| `jobs.<job_id>.container.options` | 传递额外的 Docker 容器启动参数。 |
| `jobs.<job_id>.services` | 定义供 job 使用的服务容器。 |
| `jobs.<job_id>.services.<service_id>.image` | 指定服务容器镜像。 |
| `jobs.<job_id>.services.<service_id>.credentials` | 为私有服务镜像提供登录凭据。 |
| `jobs.<job_id>.services.<service_id>.env` | 设置服务容器环境变量。 |
| `jobs.<job_id>.services.<service_id>.ports` | 映射服务容器端口。 |
| `jobs.<job_id>.services.<service_id>.volumes` | 为服务容器挂载卷。 |
| `jobs.<job_id>.services.<service_id>.options` | 传递额外的服务容器启动参数，例如健康检查。 |
| `jobs.<job_id>.services.<service_id>.command` | 覆盖服务容器默认命令。 |
| `jobs.<job_id>.services.<service_id>.entrypoint` | 覆盖服务容器默认 entrypoint。 |
| `jobs.<job_id>.uses` | 调用 reusable workflow；与 `steps` 不能同时使用。 |
| `jobs.<job_id>.with` | 向 reusable workflow 传递输入参数。 |
| `jobs.<job_id>.with.<input_id>` | 为 reusable workflow 的指定输入赋值。 |
| `jobs.<job_id>.secrets` | 向 reusable workflow 传递 secrets。 |
| `jobs.<job_id>.secrets.inherit` | 继承调用方可用的 secrets。 |
| `jobs.<job_id>.secrets.<secret_id>` | 向 reusable workflow 传递指定 secret。 |

## Step-level keywords

| Keyword | Summary |
|---|---|
| `jobs.<job_id>.steps[*].id` | 为 step 设置唯一 ID，供 outputs 和表达式引用。 |
| `jobs.<job_id>.steps[*].if` | 根据表达式决定 step 是否运行。 |
| `jobs.<job_id>.steps[*].name` | 设置 step 在日志中显示的名称。 |
| `jobs.<job_id>.steps[*].uses` | 调用一个 Action，例如 `actions/checkout@v4` 或本地 Action。 |
| `jobs.<job_id>.steps[*].run` | 在 runner shell 中执行命令。 |
| `jobs.<job_id>.steps[*].working-directory` | 设置当前 step 的工作目录。 |
| `jobs.<job_id>.steps[*].shell` | 指定当前 `run` step 使用的 shell。 |
| `jobs.<job_id>.steps[*].with` | 向 Action 传递输入参数。 |
| `jobs.<job_id>.steps[*].with.args` | 为 Docker Action 设置命令行参数。 |
| `jobs.<job_id>.steps[*].with.entrypoint` | 为 Docker Action 覆盖 entrypoint。 |
| `jobs.<job_id>.steps[*].env` | 设置当前 step 的环境变量。 |
| `jobs.<job_id>.steps[*].continue-on-error` | 允许当前 step 失败后继续执行后续 steps。 |
| `jobs.<job_id>.steps[*].timeout-minutes` | 设置当前 step 的最大运行时间。 |
| `jobs.<job_id>.steps[*].background` | 配置 step 是否作为后台任务运行（按当前 GitHub Actions 支持情况使用）。 |
| `jobs.<job_id>.steps[*].wait` | 配置后台 step 的等待行为（按当前 GitHub Actions 支持情况使用）。 |
| `jobs.<job_id>.steps[*].wait-all` | 等待相关后台 step 全部完成（按当前 GitHub Actions 支持情况使用）。 |
| `jobs.<job_id>.steps[*].cancel` | 配置取消 workflow 时对相关 step 的处理（按当前 GitHub Actions 支持情况使用）。 |
| `jobs.<job_id>.steps[*].parallel` | 配置 step 的并行执行行为（按当前 GitHub Actions 支持情况使用）。 |

## Quick selection

| 需求 | 关键字 |
|---|---|
| 触发 workflow | `on` |
| 设置权限 | `permissions` |
| 设置环境变量 | `env` |
| 控制 job 顺序 | `jobs.<job_id>.needs` |
| 选择 runner | `jobs.<job_id>.runs-on` |
| 执行命令 | `jobs.<job_id>.steps[*].run` |
| 使用现成 Action | `jobs.<job_id>.steps[*].uses` |
| 传递 Action 参数 | `jobs.<job_id>.steps[*].with` |
| 传递 job 输出 | `jobs.<job_id>.outputs` |
| 矩阵并行 | `jobs.<job_id>.strategy.matrix` |
| 使用容器 | `jobs.<job_id>.container` |
| 使用数据库服务 | `jobs.<job_id>.services` |
| 复用 workflow | `jobs.<job_id>.uses` + `jobs.<job_id>.with` |
