GitHub Actions 的官方 YAML 关键字参考文档可以在 GitHub 官方文档库中找到：

👉 **[GitHub Actions 的 Workflow 语法文档 (Workflow syntax for GitHub Actions)](https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions)**

---

### GitHub Actions vs GitLab CI 核心关键字映射

为了方便从 GitLab CI 转换，以下是常用关键字的直接对照：

| 功能 / 概念 | GitLab CI (`.gitlab-ci.yml`) | GitHub Actions (`.github/workflows/*.yml`) |
| --- | --- | --- |
| **流水线名称** | （顶部直接写 Job） | `name: My Workflow` |
| **触发条件** | `workflow:rules` / `only` / `except` | `on:` (如 `on: push`, `on: pull_request`) |
| **指定执行节点/环境** | `tags:` / `image:` | `runs-on:` (如 `runs-on: ubuntu-latest`) |
| **阶段与依赖** | `stages:` / `needs:` | `jobs.<job_id>.needs:` |
| **执行步骤/命令** | `script:` | `steps:` 下的 `- run:` |
| **复用组件/Action** | `include:` / `template:` | `steps:` 下的 `- uses:` (如 `uses: actions/checkout@v4`) |
| **环境变量** | `variables:` | `env:` (全局或 Job/Step 级别) |
| **机密信息/密钥** | GitLab CI/CD Variables | `secrets.` (如 `${{ secrets.MY_TOKEN }}`) |
| **构建矩阵 (Matrix)** | `parallel: matrix` | `jobs.<job_id>.strategy.matrix:` |
| **条件判断** | `rules:if` | `if:` (如 `if: github.ref == 'refs/heads/main'`) |
| **产物与缓存** | `artifacts:` / `cache:` | `actions/upload-artifact@v4` / `actions/cache@v4` |

---

### 快速查找常用 YAML 结构示例

```yaml
name: CI Example                   # 流水线名称

on:                                # [触发条件] 对应 GitLab 的 rules/only
  push:
    branches: [ "main" ]

env:                               # [全局环境变量] 对应 GitLab 的 variables
  GLOBAL_VAR: "hello"

jobs:                              # [任务列表]
  build-job:
    runs-on: ubuntu-latest         # [执行环境] 对应 GitLab 的 image/tags
    
    steps:                         # [步骤列表]
      - name: Check out code
        uses: actions/checkout@v4  # [复用 Action] GitLab 无直接对应，需手动 git clone 或靠 runner 自动处理

      - name: Run script
        run: echo "Build starting" # [执行命令] 对应 GitLab 的 script
        env:
          STEP_VAR: "world"

```