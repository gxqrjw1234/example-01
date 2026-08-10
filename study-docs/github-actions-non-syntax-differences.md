# GitHub Actions 与 GitLab CI/CD：非语法差异速览

面向 GitLab CI/CD 熟手、GitHub Actions 初学者。

这份文档不讨论 YAML 写法本身，而是回答一个更关键的问题：

> GitHub Actions 和 GitLab CI/CD 除了语法不同，运行方式、权限边界、审批模型和工程组织方式到底有什么不同？

## 一句话结论

GitLab CI/CD 更像“集成在 DevOps 平台里的流水线系统”；GitHub Actions 更像“绑定在仓库事件上的自动化平台”。

所以迁移时，真正要重建的不是字段名，而是下面这些心智模型：

1. 从 `pipeline/stage` 转向 `event/workflow/job DAG`。
2. 从 `image` 转向 `runs-on + container + services` 三层模型。
3. 从“规则写在 job 上”转向“workflow 是否启动”和“job/step 是否执行”分层思考。
4. 从“依赖关系顺带传数据”转向“顺序、文件、字符串值、环境变量分别用不同机制处理”。

## 关键差异总览

| 维度 | GitLab CI/CD | GitHub Actions | 迁移提醒 |
|---|---|---|---|
| 产品定位 | 内建在平台中的 CI/CD 子系统 | 仓库事件驱动自动化平台 | GitHub 不只做 CI/CD，也天然覆盖 PR、Issue、Release、Package 自动化 |
| 执行模型 | `pipeline -> stage -> job` | `event -> workflow -> job DAG -> step/action` | 不要强行把 GitHub 写成单一 stage 流水线 |
| 触发模型 | `rules`、`only/except` 常在 job 级表达 | `on:` 决定 workflow 是否启动，`if:` 决定 job/step 是否执行 | 触发条件和运行时条件必须拆开思考 |
| 运行环境 | `runner + image + services` | `runs-on + container + services` | `image` 更接近 GitHub 的 `container:`，不是 `runs-on` |
| 权限模型 | `CI_JOB_TOKEN`、protected refs、变量控制 | `GITHUB_TOKEN`、`permissions`、Fork PR 限制、environment reviewers | GitHub 更强调最小权限和不可信 PR 隔离 |
| 数据传递 | `artifacts`、`dependencies`、dotenv | artifact、`needs`、`outputs`、`GITHUB_OUTPUT`、`GITHUB_ENV` | `needs` 不会自动传文件或变量 |
| 复用方式 | `include`、`extends`、模板 | reusable workflow、composite action、Marketplace action | GitHub 的复用粒度更细 |
| 审批与发布 | `when: manual`、protected environment | `workflow_dispatch` + `environment` protection | 手动触发不等于审批 |
| 观察与协作 | 更偏 pipeline/stage/job 视图 | 更偏 PR Checks、annotations、Job Summary | GitHub 与代码评审界面联动更紧 |
| 生态与供应链 | 以内建模板和平台特性为主 | 第三方 action 生态强 | 要重视 action 来源、版本固定和权限审计 |

## 最容易误判的 6 件事

### 1. `stage` 不是 GitHub 的核心模型

GitLab 倾向先设计 stage，再把 job 塞进去。GitHub Actions 更适合先思考：

- 谁触发 workflow。
- 哪些 job 可以并行。
- 哪些 job 之间有显式依赖。
- 哪些步骤应该沉淀成 reusable workflow 或 composite action。

换句话说，GitHub Actions 更偏 DAG，而不是 stage 队列。

### 2. `workflow_dispatch` 不是审批

它只是手动启动入口。真正的审批、等待 reviewer、限制部署环境，一般靠 `environment` 和仓库 Settings 中的 protection rules。

### 3. `needs` 不是 `dependencies`

`needs` 只表达依赖顺序和 outputs 读取关系，不会自动下载构建产物。文件要用 artifact，字符串值要用 job outputs，跨 step 环境变量要用 `GITHUB_ENV`。

### 4. `GITHUB_TOKEN` 不是 `CI_JOB_TOKEN` 的一比一替代

两者用途接近，但权限模型不同。GitHub 里更关键的是：

- 显式写最小 `permissions`
- 了解 Fork PR 的 token/secrets 限制
- 区分 `pull_request` 和 `pull_request_target` 的安全边界

### 5. `runs-on` 不是 `image`

`runs-on` 先选择宿主 runner。若你想让 job 在某个容器镜像里运行，应使用 `container:`。所以 GitLab 的：

```yaml
image: node:20
```

在 GitHub 的正确心智模型更接近：

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: node:20
```

### 6. GitHub Actions 的“平台行为”比 YAML 更重要

很多关键能力并不只写在 YAML 里，例如：

- environment reviewers
- Fork PR secret 限制
- workflow 运行权限
- action 允许来源策略
- hosted runner 与 self-hosted runner 策略

所以学 GitHub Actions，不能只背 YAML 字段，还要同步理解仓库与组织设置。

## GitLab 专家的迁移建议

1. 先按事件拆 workflow，再在 workflow 内设计 `needs` 依赖图。
2. 先区分“触发器”“运行时条件”“审批”“权限”四个维度，再写 YAML。
3. 把“文件”“字符串值”“环境变量”三种跨步骤/跨 job 传递方式分开设计。
4. 默认把来自 Fork 的 PR 当作不可信代码处理。
5. 对第三方 action 做版本固定和来源审计，不要直接使用浮动版本。

## 推荐阅读

- [README.md](README.md)：主文档，涵盖语法对比、迁移清单、完整 sample。
- [github-actions-vs-gitlab-ci.md](github-actions-vs-gitlab-ci.md)：已有对照文档。
