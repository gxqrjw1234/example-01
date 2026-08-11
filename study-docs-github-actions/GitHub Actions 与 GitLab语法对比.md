# GitHub Actions 与 GitLab CI/CD 语法对比

面向 GitLab CI/CD 熟手、GitHub Actions 初学者。

这份文档聚焦三件事：
1. 概念映射。
2. 语法对比。
3. 可直接套用的 sample code。

> **YAML 缩进说明（高频坑）**：GitHub Actions 的 step 里，`uses`、`run`、`with`、`env`、`if` 这几个字段全部与 `-` 对齐，不要多缩进。
> ```yaml
> steps:
>   - uses: actions/setup-node@v4   # ← uses 在 - 后 2 格
>     with:                         # ← with 与 uses 平级
>       node-version: '20'
>   - run: npm test
>     env:                          # ← env 与 run 平级
>       TOKEN: abc
> ```

---

## 1. 核心概念映射

| GitLab CI/CD | GitHub Actions | 说明 |
|---|---|---|
| `.gitlab-ci.yml` | `.github/workflows/*.yml` | GitLab 常见单文件，GitHub 常见多 workflow 文件 |
| pipeline | workflow | 一次完整执行 |
| stage | 无严格等价物 | GitHub 更偏向用 `needs` 描述依赖图 |
| job | job | 基本对应 |
| script | step 中的 `run` | GitHub 把 job 拆成更细的 steps |
| runner | runner | 两边都支持 hosted / self-hosted |
| `tags` | `runs-on` / self-hosted labels | 指定执行节点 |
| `only` / `except` | `on:`（部分对应） | `on:` 是 workflow 级触发器，不能完整表达 GitLab 的 job 级筛选 |
| `rules` | `on:` + job/step 级 `if:` | workflow 是否启动由 `on:` 决定；启动后 job/step 是否执行由 `if:` 决定 |
| `variables` | `env` / `vars` / `secrets` | GitHub 区分更细 |
| `artifacts` | `upload-artifact` / `download-artifact` | GitHub 需要显式上传下载 |
| `cache` | `actions/cache` 或 action 自带缓存 | 实现方式不同 |
| `include` + `extends` | reusable workflow / composite action | GitHub 的复用层次更多 |
| `when: manual` | `workflow_dispatch` + `environment` | `workflow_dispatch` 只是手动启动；受保护审批需另行配置 `environment` 的 Required reviewers |
| scheduled pipeline | `schedule` | cron 定时触发 |
| protected environment | `environment` + Settings | 审批规则在 GitHub 平台设置中配置 |
| `before_script` | step（无专属字段） | 用普通 step 模拟 |
| `after_script` | step + `if: always()`（近似） | 可模拟多数收尾逻辑，但取消、步骤状态和工作目录语义不完全相同 |
| `allow_failure` | `continue-on-error` | 允许失败继续 |
| `retry` | 无内置，靠脚本或第三方 action | — |
| `timeout` | `timeout-minutes` | job 或 step 级别超时 |
| `interruptible` | `concurrency`（近似） | `concurrency` 按 group 排队或取消运行，不是 job 级 `interruptible` 的一一对应 |
| `services` | `services` | 服务容器（数据库等） |

---

## 2. 最小可运行示例

### GitLab

```yaml
stages:
  - test

test_job:
  stage: test
  image: node:20
  script:
    - npm ci
    - npm test
```

### GitHub Actions

```yaml
name: basic-ci

on:
  push:
    branches: [main]

jobs:
  test_job:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm test
```

关键差异：

1. GitHub Actions **不会自动 checkout** 代码，必须显式写 `actions/checkout`。
2. job 下必须有 `steps` 字段，每个 step 用 `-` 列出。
3. 执行环境用 `uses: actions/setup-node` 准备，而不是 `image:`。

---

## 3. 触发条件（`on:` 与 `only` / `rules`）

### GitLab

```yaml
# 方式一：only（老写法）
build_main:
  script:
    - echo "build main"
  only:
    - main

# 方式二：rules（推荐写法）
build_main:
  script:
    - echo "build main"
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
```

### GitHub Actions

```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build_main:
    runs-on: ubuntu-latest
    steps:
      - run: echo "build main"
```

job 级条件（workflow 已触发，但跳过某个 job）：

```yaml
jobs:
  deploy:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: echo deploy
```

理解方式：

1. `on:` 决定 workflow 什么时候启动，只能部分对应 GitLab 的 `only/except/rules`。
2. `if:` 决定已启动 workflow 中某个 job 或 step 是否执行；它不是 workflow 级触发器。

---

## 4. 路径过滤（`changes:` 与 `paths:`）

### GitLab

```yaml
build:
  script:
    - npm run build
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
      changes:
        - src/**/*
        - package*.json
```

### GitHub Actions

```yaml
on:
  push:
    branches: [main]
    paths:
      - 'src/**'
      - 'package*.json'
      - '!**.md'           # paths 与 paths-ignore 不能同时使用；用否定模式排除 Markdown

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm run build
```

差异点：GitLab 的 `changes:` 在 job 级，GitHub 的 `paths:` 在 `on:` 级，影响整个 workflow。

---

## 5. Stage 与 `needs`（依赖图）

### GitLab

```yaml
stages:
  - lint
  - test
  - build

lint:
  stage: lint
  script:
    - echo lint

test:
  stage: test
  script:
    - echo test

build:
  stage: build
  script:
    - echo build
```

### GitHub Actions

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - run: echo lint

  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo test

  build:
    needs: [lint, test]
    runs-on: ubuntu-latest
    steps:
      - run: echo build
```

GitHub Actions 没有必须先声明 stage 的模型，更好的心智模型是 DAG（有向无环图）。

---

## 6. Runner 与执行环境

### GitLab

```yaml
test:
  tags:
    - docker
  image: node:20
  script:
    - node -v
```

### GitHub Actions

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: node -v
```

自托管 runner：

```yaml
jobs:
  test:
    runs-on: [self-hosted, linux, x64]
    steps:
      - run: node -v
```

GitHub Hosted Runner 预装了 Node、Java、Python、Docker 等常用工具，不需要像 GitLab 那样指定 image 才能使用。

---

## 7. `before_script` / `after_script` 对比

### GitLab

```yaml
test:
  before_script:
    - npm ci
  script:
    - npm test
  after_script:
    - echo "done, cleanup"
```

### GitHub Actions

GitHub Actions 没有专属的 `before_script` / `after_script` 字段，用 steps 表达：

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # 等价 before_script
      - name: Install deps
        run: npm ci

      # 等价 script
      - name: Run tests
        run: npm test

      # 等价 after_script（无论前面成功还是失败都执行）
      - name: Cleanup
        if: always()
        run: echo "done, cleanup"
```

关键点：`if: always()` 只是对 `after_script` 的近似。它通常会在前面步骤失败后继续执行，但取消、超时以及失败步骤产生的工作目录和状态语义可能不同；需要清理临时资源时应明确验证这些边界。

---

## 8. 变量、密钥、上下文

### GitLab

```yaml
variables:
  APP_ENV: production

deploy:
  script:
    - echo $APP_ENV
    - echo $CI_COMMIT_SHA
    - echo $CI_COMMIT_BRANCH
```

### GitHub Actions

```yaml
env:
  APP_ENV: production

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "$APP_ENV"
          echo "${{ github.sha }}"
          echo "${{ github.ref_name }}"
```

常见变量映射：

| GitLab | GitHub Actions | 说明 |
|---|---|---|
| `$CI_COMMIT_SHA` | `${{ github.sha }}` | 提交 SHA |
| `$CI_COMMIT_BRANCH` | `${{ github.ref_name }}` | 分支名 |
| `$CI_PROJECT_PATH` | `${{ github.repository }}` | org/repo |
| `$CI_PIPELINE_ID` | `${{ github.run_id }}` | 流水线 ID |
| `$CI_JOB_ID` | 无直接等价物 | `github.job` 是当前 job 的标识，不是全局唯一的 job 执行 ID |
| `$GITLAB_USER_LOGIN` | `${{ github.actor }}` | 触发人 |
| `$CI_REGISTRY_IMAGE` | `ghcr.io/${{ github.repository }}` | 容器镜像地址 |
| `$CI_JOB_TOKEN` | `${{ secrets.GITHUB_TOKEN }}`（用途近似） | 两者权限模型不同；GitHub 必须通过 `permissions` 显式限制权限 |

不要把以下三个 GitHub context 混为一谈：`github.job` 是 workflow 文件中的 job 标识，`github.run_id` 是一次 workflow run 的标识，`github.run_attempt` 是该 run 的重试/重新运行次数。它们都不是 `$CI_JOB_ID` 的直接映射。

GitHub Actions 三类变量来源：

| 来源 | 写法 | 说明 |
|---|---|---|
| 环境变量 | `env:` | workflow / job / step 级，明文 |
| 仓库变量 | `${{ vars.XXX }}` | Settings → Variables，明文 |
| 密钥 | `${{ secrets.XXX }}` | Settings → Secrets，加密，日志自动屏蔽 |

### GitHub Actions Importer 的默认变量映射

如果使用 GitHub Actions Importer 迁移 GitLab CI/CD，它会把一批 GitLab 默认环境变量自动转换为“最接近的 GitHub Actions 等效项”。

注意两点：

1. 这些映射是 Importer 的**默认近似映射**，目标是帮助自动迁移，不代表人工手写 workflow 时一定是最佳表达。
2. 某些 GitLab 变量在 GitHub 中并不存在真正一一对应的语义，Importer 只能退化为“最接近可用字段”。

| GitLab | GitHub Actions |
|---|---|
| `CI_API_V4_URL` | `${{ github.api_url }}` |
| `CI_BUILDS_DIR` | `${{ github.workspace }}` |
| `CI_COMMIT_BRANCH` | `${{ github.ref }}` |
| `CI_COMMIT_REF_NAME` | `${{ github.ref }}` |
| `CI_COMMIT_REF_SLUG` | `${{ github.ref }}` |
| `CI_COMMIT_SHA` | `${{ github.sha }}` |
| `CI_COMMIT_SHORT_SHA` | `${{ github.sha }}` |
| `CI_COMMIT_TAG` | `${{ github.ref }}` |
| `CI_JOB_ID` | `${{ github.job }}` |
| `CI_JOB_MANUAL` | `${{ github.event_name == 'workflow_dispatch' }}` |
| `CI_JOB_NAME` | `${{ github.job }}` |
| `CI_JOB_STATUS` | `${{ job.status }}` |
| `CI_JOB_URL` | `${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}` |
| `CI_JOB_TOKEN` | `${{ github.token }}` |
| `CI_NODE_INDEX` | `${{ strategy.job-index }}` |
| `CI_NODE_TOTAL` | `${{ strategy.job-total }}` |
| `CI_PIPELINE_ID` | `${{ github.repository }}/${{ github.workflow }}` |
| `CI_PIPELINE_IID` | `${{ github.workflow }}` |
| `CI_PIPELINE_SOURCE` | `${{ github.event_name }}` |
| `CI_PIPELINE_TRIGGERED` | `${{ github.actions }}` |
| `CI_PIPELINE_URL` | `${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}` |
| `CI_PROJECT_DIR` | `${{ github.workspace }}` |
| `CI_PROJECT_ID` | `${{ github.repository }}` |
| `CI_PROJECT_NAME` | `${{ github.event.repository.name }}` |
| `CI_PROJECT_NAMESPACE` | `${{ github.repository_owner }}` |
| `CI_PROJECT_PATH_SLUG` | `${{ github.repository }}` |
| `CI_PROJECT_PATH` | `${{ github.repository }}` |
| `CI_PROJECT_ROOT_NAMESPACE` | `${{ github.repository_owner }}` |
| `CI_PROJECT_TITLE` | `${{ github.event.repository.full_name }}` |
| `CI_PROJECT_URL` | `${{ github.server_url }}/${{ github.repository }}` |
| `CI_REPOSITORY_URL` | `${{ github.event.repository.clone_url }}` |
| `CI_RUNNER_EXECUTABLE_ARCH` | `${{ runner.os }}` |
| `CI_SERVER_HOST` | `${{ github.server_url }}` |
| `CI_SERVER_URL` | `${{ github.server_url }}` |
| `CI_SERVER` | `${{ github.actions }}` |
| `GITLAB_CI` | `${{ github.actions }}` |
| `GITLAB_USER_EMAIL` | `${{ github.actor }}` |
| `GITLAB_USER_ID` | `${{ github.actor }}` |
| `GITLAB_USER_LOGIN` | `${{ github.actor }}` |
| `GITLAB_USER_NAME` | `${{ github.actor }}` |
| `TRIGGER_PAYLOAD` | `${{ github.event_path }}` |
| `CI_MERGE_REQUEST_ASSIGNEES` | `${{ github.event.pull_request.assignees }}` |
| `CI_MERGE_REQUEST_ID` | `${{ github.event.pull_request.number }}` |
| `CI_MERGE_REQUEST_IID` | `${{ github.event.pull_request.number }}` |
| `CI_MERGE_REQUEST_LABELS` | `${{ github.event.pull_request.labels }}` |
| `CI_MERGE_REQUEST_MILESTONE` | `${{ github.event.pull_request.milestone }}` |
| `CI_MERGE_REQUEST_PROJECT_ID` | `${{ github.repository }}` |
| `CI_MERGE_REQUEST_PROJECT_PATH` | `${{ github.repository }}` |
| `CI_MERGE_REQUEST_PROJECT_URL` | `${{ github.server_url }}/${{ github.repository }}` |
| `CI_MERGE_REQUEST_REF_PATH` | `${{ github.ref }}` |
| `CI_MERGE_REQUEST_SOURCE_BRANCH_NAME` | `${{ github.event.pull_request.head.ref }}` |
| `CI_MERGE_REQUEST_SOURCE_BRANCH_SHA` | `${{ github.event.pull_request.head.sha }}` |
| `CI_MERGE_REQUEST_SOURCE_PROJECT_ID` | `${{ github.event.pull_request.head.repo.full_name }}` |
| `CI_MERGE_REQUEST_SOURCE_PROJECT_PATH` | `${{ github.event.pull_request.head.repo.full_name }}` |
| `CI_MERGE_REQUEST_SOURCE_PROJECT_URL` | `${{ github.event.pull_request.head.repo.url }}` |
| `CI_MERGE_REQUEST_TARGET_BRANCH_NAME` | `${{ github.event.pull_request.base.ref }}` |
| `CI_MERGE_REQUEST_TARGET_BRANCH_SHA` | `${{ github.event.pull_request.base.sha }}` |
| `CI_MERGE_REQUEST_TITLE` | `${{ github.event.pull_request.title }}` |
| `CI_EXTERNAL_PULL_REQUEST_IID` | `${{ github.event.pull_request.number }}` |
| `CI_EXTERNAL_PULL_REQUEST_SOURCE_REPOSITORY` | `${{ github.event.pull_request.head.repo.full_name }}` |
| `CI_EXTERNAL_PULL_REQUEST_TARGET_REPOSITORY` | `${{ github.event.pull_request.base.repo.full_name }}` |
| `CI_EXTERNAL_PULL_REQUEST_SOURCE_BRANCH_NAME` | `${{ github.event.pull_request.head.ref }}` |
| `CI_EXTERNAL_PULL_REQUEST_SOURCE_BRANCH_SHA` | `${{ github.event.pull_request.head.sha }}` |
| `CI_EXTERNAL_PULL_REQUEST_TARGET_BRANCH_NAME` | `${{ github.event.pull_request.base.ref }}` |
| `CI_EXTERNAL_PULL_REQUEST_TARGET_BRANCH_SHA` | `${{ github.event.pull_request.base.sha }}` |

使用建议：如果你是**手工编写** GitHub Actions，优先使用前面那张“常见变量映射”表里的更自然写法，例如分支名通常更适合用 `${{ github.ref_name }}`，而不是完整 ref `${{ github.ref }}`。

示例：

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - run: curl -H "Authorization: Bearer $TOKEN" "$DEPLOY_URL"
        env:
          TOKEN: ${{ secrets.DEPLOY_TOKEN }}
          DEPLOY_URL: ${{ vars.DEPLOY_URL_PROD }}
```

---

## 9. Artifact 对比

### GitLab

```yaml
build:
  script:
    - npm run build
  artifacts:
    paths:
      - dist/
    expire_in: 1 week

deploy:
  stage: deploy
  dependencies:
    - build           # 自动下载 build 的 artifact
  script:
    - ls dist/
```

### GitHub Actions

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run build
      - uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/
          retention-days: 7

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4   # 必须显式下载
        with:
          name: dist
          path: dist/
      - run: ls dist/
```

重点差异：GitLab 通过 `dependencies:` 自动传文件，GitHub Actions 必须显式调用 `download-artifact`，`needs:` 只管依赖关系。

---

## 10. Cache 对比

### GitLab

```yaml
cache:
  key: $CI_COMMIT_REF_SLUG-npm
  paths:
    - .npm/

test:
  script:
    - npm ci --cache .npm
    - npm test
```

### GitHub Actions

```yaml
# 方式一：setup-node 内置 cache（推荐 Node 项目）
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci && npm test
```

```yaml
# 方式二：actions/cache 显式控制（适合自定义场景）
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/cache@v4
        with:
          path: ~/.npm
          key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-npm-
      - run: npm ci && npm test
```

---

## 11. 矩阵并行对比

### GitLab

```yaml
test:
  parallel:
    matrix:
      - NODE_VERSION: ['18', '20', '22']
  image: node:$NODE_VERSION
  script:
    - node -v
    - npm test
```

### GitHub Actions

```yaml
jobs:
  test:
    strategy:
      matrix:
        node-version: ['18', '20', '22']
        os: [ubuntu-latest, windows-latest]   # 多维矩阵
      fail-fast: false
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
      - run: node -v && npm test
```

矩阵变量读取：`${{ matrix.xxx }}`，字段名自定义。

---

## 12. 条件执行对比（`rules` 与 `if`）

### GitLab

```yaml
deploy_prod:
  script:
    - ./deploy.sh
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
      when: on_success
    - when: never

notify_failure:
  script:
    - ./notify.sh
  when: on_failure
```

### GitHub Actions

```yaml
jobs:
  deploy_prod:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: ./deploy.sh

  notify_failure:
    needs: deploy_prod
    if: failure()
    runs-on: ubuntu-latest
    steps:
      - run: ./notify.sh
```

常用 `if:` 表达式：

| 表达式 | 等价 GitLab |
|---|---|
| `github.ref == 'refs/heads/main'` | `$CI_COMMIT_BRANCH == "main"` |
| `github.event_name == 'pull_request'` | MR 触发 |
| `startsWith(github.ref, 'refs/tags/v')` | `$CI_COMMIT_TAG =~ /^v/` |
| `contains(github.event.head_commit.message, '[skip ci]')` | `[skip ci]` |
| `failure()` | `when: on_failure` |
| `success()` | `when: on_success` |
| `always()` | `when: always` |

---

## 13. `allow_failure` 与 `continue-on-error`

### GitLab

```yaml
lint:
  script:
    - npm run lint
  allow_failure: true
```

### GitHub Actions

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    continue-on-error: true      # job 级：等价 allow_failure: true
    steps:
      - uses: actions/checkout@v4
      - run: npm run lint
```

也可以在 step 级控制：

```yaml
steps:
  - run: npm run lint
    continue-on-error: true      # 这一步失败不中断后续 step
```

---

## 14. `timeout` 对比

### GitLab

```yaml
test:
  timeout: 30 minutes
  script:
    - npm test
```

### GitHub Actions

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 30          # job 级超时
    steps:
      - run: npm run e2e
        timeout-minutes: 10      # step 级超时
```

---

## 15. `retry` 对比

### GitLab

```yaml
flaky_test:
  script:
    - npm run e2e
  retry:
    max: 2
    when:
      - script_failure
```

### GitHub Actions

GitHub Actions **没有内置 retry**，常见方案：

```yaml
steps:
  # 方案一：Marketplace action
  - uses: nick-fields/retry@v3
    with:
      max_attempts: 3
      command: npm run e2e
      timeout_minutes: 10

  # 方案二：shell 循环（不依赖第三方 action）
  - run: |
      for i in 1 2 3; do
        if npm run e2e; then
          exit 0
        fi
        echo "Attempt $i failed, retrying..."
        sleep 5
      done
      exit 1
```

---

## 16. 并发控制（`interruptible` 与 `concurrency`）

### GitLab

```yaml
build:
  interruptible: true
  script:
    - npm run build
```

### GitHub Actions

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true     # 近似 interruptible: true，不是一一等价

on:
  push:
    branches: ['**']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: npm run build
```

`concurrency` 的行为由 group 决定：同一 group 中通常只保留一个运行；`cancel-in-progress: true` 会取消正在运行的旧 run，`false` 则让新的 run 排队。因此它只是对 GitLab `interruptible` 的近似，不能自动复制 GitLab 的 job 级取消语义。

job 级并发（部署排队不取消）：

```yaml
jobs:
  deploy:
    concurrency:
      group: deploy-${{ github.ref }}
      cancel-in-progress: false   # 排队等待，不取消
    runs-on: ubuntu-latest
    steps:
      - run: ./deploy.sh
```

---

## 17. 服务容器（`services`）

### GitLab

```yaml
test:
  image: node:20
  services:
    - name: postgres:15
      alias: db
  variables:
    POSTGRES_DB: testdb
    POSTGRES_USER: user
    POSTGRES_PASSWORD: pass
  script:
    - npm test
```

### GitHub Actions

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: user
          POSTGRES_PASSWORD: pass
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - run: npm test
        env:
          DATABASE_URL: postgresql://user:pass@localhost:5432/testdb
```

---

## 18. Job Outputs（跨 Job 传值）

### GitLab

```yaml
build:
  script:
    - echo "VERSION=1.2.3" >> build.env
  artifacts:
    reports:
      dotenv: build.env

deploy:
  stage: deploy
  dependencies:
    - build
  script:
    - echo "Deploy $VERSION"
```

### GitHub Actions

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.get-version.outputs.version }}
    steps:
      - id: get-version
        run: echo "version=1.2.3" >> $GITHUB_OUTPUT

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploy ${{ needs.build.outputs.version }}"
```

---

## 19. `GITHUB_TOKEN` 权限（无 GitLab 等价物）

GitHub Actions 有内置 `GITHUB_TOKEN`，每次运行自动注入，无需手动配置 Secrets。

它与 GitLab 的 `CI_JOB_TOKEN` 只是用途相近，不是同一个权限模型。应在 workflow 或 job 级声明最小 `permissions`；一旦声明了 `permissions`，未列出的权限通常会被设为 `none`。Fork 发起的 PR 还会受到更严格的 token 和 secrets 限制，不能把它当作可信部署凭据。

```yaml
permissions:
  contents: read
  packages: write       # 推送镜像到 GHCR
  pull-requests: write  # 在 PR 上写评论

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
```

默认权限建议在 workflow 顶层或具体 job 显式声明最小权限（安全最佳实践）。

---

## 20. 手动触发（`when: manual` 与 `workflow_dispatch`）

### GitLab

```yaml
deploy_prod:
  script:
    - ./deploy.sh prod
  when: manual
  environment:
    name: production
```

### GitHub Actions

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: target env
        type: choice
        options: [staging, production]
        default: staging
        required: true
      dry_run:
        description: dry run mode
        type: boolean
        default: false

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "deploy to ${{ inputs.environment }}"
          echo "dry_run: ${{ inputs.dry_run }}"
```

支持三种输入类型：`choice`、`boolean`、`string`。

注意：`workflow_dispatch` 只提供手动启动入口，不等同于 GitLab `when: manual` 的受保护审批。若部署必须经过审批，应给 job 配置 `environment`，并在仓库 Settings → Environments 中设置 Required reviewers；输入参数本身不会提供审批保护。

---

## 21. 定时任务对比

### GitLab

GitLab 一般在平台界面（Settings → CI/CD → Schedules）配置，YAML 不体现。

### GitHub Actions

```yaml
on:
  schedule:
    - cron: '0 2 * * 1-5'    # UTC，周一到周五凌晨 2 点

jobs:
  nightly:
    runs-on: ubuntu-latest
    steps:
      - run: echo "nightly build"
```

GitHub Actions 调度规则在仓库里版本化管理，更偏"配置即代码"。

---

## 22. 复用能力对比

### GitLab

```yaml
include:
  - local: .gitlab/common.yml
  - project: 'org/templates'
    file: '/ci/deploy.yml'

.base_job:
  image: node:20
  before_script:
    - npm ci

test:
  extends: .base_job
  script:
    - npm test
```

### GitHub Actions — 三种方式

#### Composite Action（最接近 `extends`）

```yaml
steps:
  - uses: ./.github/actions/setup-node-cached
    with:
      node-version: '20'
```

#### Reusable Workflow（跨 job 复用整段部署流）

调用方：

```yaml
jobs:
  deploy_prod:
    uses: ./.github/workflows/reusable-deploy.yml
    # uses: org/repo/.github/workflows/deploy.yml@main  # 跨仓库
    with:
      environment: production
    secrets:
      deploy_token: ${{ secrets.DEPLOY_TOKEN }}
```

被调用方 skeleton：

```yaml
on:
  workflow_call:
    inputs:
      environment:
        type: string
        required: true
    secrets:
      deploy_token:
        required: true
    outputs:
      deploy_url:
        value: ${{ jobs.deploy.outputs.url }}

jobs:
  deploy:
    runs-on: ubuntu-latest
    outputs:
      url: ${{ steps.deploy.outputs.url }}
    steps:
      - id: deploy
        run: echo "url=https://example.com" >> $GITHUB_OUTPUT
```

#### Marketplace Action

```yaml
steps:
  - uses: docker/login-action@v3
  - uses: docker/build-push-action@v5
    with:
      push: true
      tags: ghcr.io/${{ github.repository }}:latest
```

---

## 23. Environment 与审批

### GitLab

```yaml
deploy_prod:
  environment:
    name: production
    url: https://example.com
  script:
    - ./deploy.sh
```

### GitHub Actions

```yaml
jobs:
  deploy_prod:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://example.com
    steps:
      - run: ./deploy.sh
```

YAML 写法两者相似，审批能力靠平台：**Settings → Environments → production** 中配置 Required Reviewers，workflow 跑到此 job 时会暂停等待审批。

---

## 24. 完整对照 Sample

### GitLab 版本

```yaml
stages:
  - lint
  - test
  - build
  - deploy

variables:
  APP_ENV: production

lint:
  stage: lint
  image: node:20
  script:
    - npm ci
    - npm run lint
  allow_failure: true

test:
  stage: test
  image: node:20
  services:
    - postgres:15
  variables:
    POSTGRES_DB: testdb
    POSTGRES_USER: user
    POSTGRES_PASSWORD: pass
  script:
    - npm ci
    - npm test
  artifacts:
    paths:
      - coverage/
    expire_in: 7 days
  retry:
    max: 1

build:
  stage: build
  image: node:20
  script:
    - npm ci
    - npm run build
  artifacts:
    paths:
      - dist/

deploy_prod:
  stage: deploy
  only:
    - main
  when: manual
  environment:
    name: production
    url: https://example.com
  script:
    - ./deploy.sh
```

### GitHub Actions 版本

```yaml
name: ci-cd

on:
  push:
    branches: [main]
  workflow_dispatch:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read
  packages: write

env:
  APP_ENV: production

jobs:
  lint:
    runs-on: ubuntu-latest
    continue-on-error: true      # 对应 allow_failure: true
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npm run lint

  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: user
          POSTGRES_PASSWORD: pass
        ports:
          - 5432:5432
        options: --health-cmd pg_isready --health-interval 10s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npm test
        env:
          DATABASE_URL: postgresql://user:pass@localhost:5432/testdb
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: coverage
          path: coverage/
          retention-days: 7

  build:
    needs: [lint, test]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npm run build
      - uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/

  deploy_prod:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://example.com
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: dist
          path: dist/
      - run: ./deploy.sh
```

---

## 25. 从 GitLab 迁移到 GitHub 最常见的坑

| 坑 | 说明 |
|---|---|
| 忘记写 `actions/checkout` | GitLab 自动 clone，GitHub Actions 不会 |
| `with:` 缩进多了两格 | `with:` 应与 `uses:` 平级 |
| `needs:` 传不了文件 | 需要额外调用 `download-artifact` |
| `workflow_dispatch` 写到 job 里 | 它属于 `on:`，不属于 job |
| 变量用 `$CI_*` 格式 | GitHub 用 `${{ github.* }}` |
| 过度依赖 stage | 应该用 `needs:` 描述依赖图 |
| 不区分 `env`、`vars`、`secrets` | 三类来源、作用域、加密方式不同 |
| 找不到 `retry:` | 无内置，用 shell 循环或第三方 action |
| 找不到 `allow_failure` | 用 `continue-on-error: true` |
| 忘记声明 `permissions` | 默认权限较宽，建议显式声明最小权限 |

---

## 26. 结合这个仓库继续学习

| 文件 | 覆盖特性 |
|---|---|
| [.github/workflows/ci.yml](.github/workflows/ci.yml) | 矩阵、artifact、concurrency |
| [.github/workflows/cd.yml](.github/workflows/cd.yml) | environment、reusable workflow、GHCR |
| [.github/workflows/reusable-deploy.yml](.github/workflows/reusable-deploy.yml) | `workflow_call`、outputs |
| [.github/workflows/scheduled-cleanup.yml](.github/workflows/scheduled-cleanup.yml) | schedule、workflow_dispatch |
| [.github/actions/setup-node-cached/action.yml](.github/actions/setup-node-cached/action.yml) | composite action |
| [github-actions-non-syntax-differences.md](github-actions-non-syntax-differences.md) | 平台行为、权限边界、心智模型 |
| [.github/actions/custom-actions-examples/](.github/actions/custom-actions-examples/) | Composite、JavaScript、Docker 三种自定义 Action |

---

## 27. GitLab 专家快速迁移清单

### 事件、contexts 与表达式

- GitHub 的 `on:` 定义 workflow 级事件，例如 `push`、`pull_request`、`workflow_dispatch`、`schedule` 和 `workflow_call`；它决定 workflow 是否启动。
- `github`、`runner`、`matrix`、`needs`、`inputs`、`secrets` 等是运行时 contexts。job/step 的 `if:` 读取这些 context，并不负责启动 workflow。
- `${{ ... }}` 是 GitHub Actions 表达式，在 step 执行前由 Actions 求值；shell 中的 `$VAR`（PowerShell 通常为 `$env:VAR`）是进程启动后由 shell 展开的变量。需要在 shell 中使用 context 时，优先通过 `env:` 传入：

```yaml
steps:
  - name: Show ref
    run: echo "ref=$GIT_REF"
    env:
      GIT_REF: ${{ github.ref_name }}
```

不要把 `${{ }}`、shell 的 `$VAR` 和 GitLab 的 `$CI_*` 混写；跨 step 传值使用 `$GITHUB_OUTPUT`，跨 step 设置环境变量使用 `$GITHUB_ENV`。

### 权限、Fork PR 与安全边界

- 为 workflow 或 job 写最小 `permissions`，例如只读代码使用 `contents: read`；不要为了修复权限错误而盲目使用 `write-all`。
- 来自 Fork 的 `pull_request` 通常不会获得仓库 secrets，`GITHUB_TOKEN` 也通常是只读的。不要把部署、发布或云端凭据注入不受信任的 PR 代码；私有仓库策略可能不同，应以仓库设置为准。
- `pull_request` 适合检查合并请求的合并结果，但执行 PR 提交中的代码时要按不可信代码处理。`pull_request_target` 在目标分支上下文运行，可能拥有基础分支的 secrets 和写权限；不要在其中 checkout 并执行未审查的 PR 代码，也不要把它当作普通 `pull_request` 的安全替代品。

### Docker `container:` 与 GitLab `image:`

GitLab job 的 `image:` 更接近 GitHub job 的 `container:`，而不是 `runs-on`。`runs-on` 先选择宿主 runner；`container:` 再让 job 的 steps 在指定容器中运行。需要 Docker 的服务容器时，可继续使用 job 级 `services:`；若要在 job 中运行 Docker 命令，还要确认 runner 和容器模式满足 Docker-in-Docker 或 Docker socket 的要求。

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: node:20
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm test
```

### 调试方法

1. 先看 Actions UI 中 workflow、job 和 step 的日志，确认事件、分支、跳过原因、runner 与权限错误。
2. 使用 `$GITHUB_STEP_SUMMARY` 写 Job Summary，把测试结果或关键诊断信息显示在 job 概览中，避免把长日志全部打印到控制台。
3. 在仓库或组织设置中临时启用 Actions 的 `ACTIONS_STEP_DEBUG`，复现后及时关闭；不要在 debug 日志中输出 secrets。
4. 提交前用 `actionlint` 检查 workflow YAML 和表达式。它能发现许多缩进、字段位置和 context 错误，但不能替代 GitHub 权限、事件和 runner 环境的实际验证。

### 迁移清单

- [ ] 将 GitLab `only/except/rules` 拆分为 workflow 级 `on:` 与 job/step 级 `if:`，并核对 `push`、`pull_request`、标签、路径和 schedule 语义。
- [ ] 将 `stages`/隐式顺序改成 `needs` 依赖图；确认失败、跳过和 `always()` 的行为。
- [ ] 将 `image:`、runner tags、services 分别映射为 `container:`、`runs-on`/labels、`services:`。
- [ ] 将变量、产物、缓存、dotenv 和 job token 分别改成 `env`/`vars`/`secrets`、artifact、cache、`$GITHUB_OUTPUT`/`$GITHUB_ENV` 与 `GITHUB_TOKEN`。
- [ ] 为每个 workflow/job 复核最小 `permissions`，特别检查 Fork PR 是否会接触 secrets、写权限或部署步骤。
- [ ] 将 `when: manual` 与受保护环境审批拆开验证：`workflow_dispatch` 负责入口，`environment` 负责审批和环境保护。
- [ ] 重新设计 `interruptible`、retry、timeout、artifact 保留期和并发 group，不要假设存在一一对应字段。
- [ ] 用 `actionlint`、Actions UI 和实际的分支/PR/手动触发各跑一次，确认 YAML、表达式、权限、路径过滤和部署保护均符合预期。

---

## 28. GitLab 专家看 GitHub Actions 的非语法差异

如果说前面的章节解决的是“字段怎么写”，这一节解决的是“平台到底怎么运作”。对 GitLab 专家来说，迁移到 GitHub Actions 时最容易低估的，往往不是 YAML，而是产品模型、权限边界和协作方式。

### 28.1 产品定位不同

- GitLab CI/CD 更像集成在 DevOps 平台里的流水线系统，默认心智模型是 pipeline。
- GitHub Actions 更像挂在仓库事件上的自动化平台，默认心智模型是 repository automation。
- 所以在 GitHub 中，CI 只是 Actions 的一个子集；同一套机制也经常用于 PR 评论、Issue 自动化、Release、Package 发布和仓库治理。

### 28.2 执行模型不同

- GitLab 更容易按 `pipeline -> stage -> job` 理解。
- GitHub 更适合按 `event -> workflow -> job DAG -> step/action` 理解。
- `stage` 在 GitHub 里不是一等概念，真正的一等概念是事件触发、job 依赖图和 action 复用。
- 所以从 GitLab 迁移时，不要先问“这个 stage 放哪”，而要先问“这个 workflow 由什么事件触发、哪些 job 可以并行、哪些 job 必须显式 `needs`”。

### 28.3 运行环境不同

- GitLab 常见写法是 `image + runner + services`。
- GitHub 要拆成三层：`runs-on` 选择宿主 runner，`container` 决定 job 是否在容器内运行，`services` 提供数据库等依赖服务。
- 因此 GitLab 的 `image:` 更接近 GitHub 的 `container:`，而不是 `runs-on`。
- 如果直接把 `image` 心智映射成 `runs-on`，很容易误解宿主机工具、Docker 可用性和文件系统行为。

### 28.4 触发器、条件和审批是三件事

- GitLab 的 `rules` 容易让人把“触发”和“执行条件”放在一个思维层面上。
- GitHub 需要明确拆开：`on:` 决定 workflow 是否启动，`if:` 决定 job/step 是否执行，`environment` protection 决定部署是否需要审批。
- `workflow_dispatch` 只是手动入口，不提供审批保护。
- 真正的生产审批，一般要靠 Settings → Environments 中的 Required reviewers、wait timer 和 secret scope。

### 28.5 权限与安全边界不同

- GitLab 里常见关注点是 protected branches、protected variables、runner scope 和 `CI_JOB_TOKEN`。
- GitHub 里更关键的是 `GITHUB_TOKEN`、`permissions`、Fork PR 的 secrets 限制，以及 `pull_request` 与 `pull_request_target` 的执行边界。
- 一旦为 workflow 或 job 显式声明 `permissions`，未列出的权限通常会被收窄；这和很多 GitLab 用户习惯的“默认可用能力”不同。
- 对来自 Fork 的 PR，应默认视为不可信代码，不要把部署凭据或高权限 token 注入其执行路径。

### 28.6 数据传递与状态管理不同

- GitLab 中，很多人习惯用 `artifacts`、`dependencies` 和 dotenv report 统一解决跨 job 数据传递。
- GitHub 中要拆成多条机制：`needs` 管顺序，artifact 传文件，job outputs 传字符串值，`GITHUB_ENV` 传后续 step 环境变量，`GITHUB_STEP_SUMMARY` 负责结果展示。
- 也就是说，GitHub 更显式，也更细粒度；换来的代价是需要你先想清楚“你到底在传什么”。

### 28.7 复用与生态不同

- GitLab 复用更常见的是 `include`、`extends` 和内部模板。
- GitHub 复用通常拆成三层：composite action 复用 steps，reusable workflow 复用 jobs，Marketplace action 复用现成能力。
- GitHub 的生态优势很明显，但也带来供应链治理问题：来源可信度、版本固定、权限范围、维护状态都需要审查。

### 28.8 协作与可观察性不同

- GitLab 更偏向 pipeline/stage/job 视角。
- GitHub 更强调与 PR Checks、review 流、annotations、job summary 的集成。
- 这意味着在 GitHub 中，CI/CD 的目标不只是“跑完”，还包括“让评审者快速看到失败位置、测试摘要和部署状态”。

### 28.9 最值得记住的 4 句话

1. GitLab 的核心心智模型是 stage 流水线；GitHub 的核心心智模型是事件驱动的 workflow + DAG。
2. GitLab 的 `image` 更接近 GitHub 的 `container`，不是 `runs-on`。
3. `workflow_dispatch` 只是手动入口，不是审批机制；审批通常靠 `environment` protection。
4. `needs` 只管顺序，不负责传文件和变量；文件靠 artifact，值靠 outputs，环境变量靠 `GITHUB_ENV`。

---

## 29. GitLab 概念 → GitHub 正确心智模型速查表

这张表不只告诉你“字段对应什么”，更强调“最容易产生的错误直觉是什么”。

| GitLab 概念 | 常见错误映射 | GitHub 正确心智模型 | 实战建议 |
|---|---|---|---|
| `pipeline` | 一个超大的 workflow 文件 | 一个仓库通常会拆成多个 workflow，按事件或职责分治 | 按 CI、CD、定时任务、复用部署流拆分 workflow |
| `stage` | GitHub 里也应该先定义阶段 | GitHub 更偏向 job DAG，核心是 `needs` | 先画依赖图，再写 job |
| `rules` / `only` / `except` | 全都映射成 `if:` | `on:` 决定 workflow 启动；`if:` 决定 job/step 执行 | 先区分触发器和运行时条件 |
| `image` | `runs-on` | `runs-on` 选宿主机，`container` 选 job 容器 | 需要固定镜像环境时优先考虑 `container:` |
| runner tags | `image` | `runs-on` 或 self-hosted labels | 把“在哪跑”和“在什么容器里跑”分开理解 |
| `dependencies` | `needs` | `needs` 只定义依赖顺序，不自动传文件 | 文件用 artifact，值用 outputs |
| dotenv artifact | 普通 `env` 就能跨 job 共享 | 跨 job 值传递依赖 `outputs`；跨 step 环境变量用 `GITHUB_ENV` | 不要指望环境变量自动跨 job 传播 |
| `when: manual` | `workflow_dispatch` 完全等价 | `workflow_dispatch` 只是入口；审批靠 `environment` protection | 把“手动启动”和“批准上线”拆成两层 |
| protected environment | YAML 中写 `environment` 就够了 | `environment` 只是引用；审批规则在仓库 Settings 里 | 同时检查 YAML 和平台配置 |
| `CI_JOB_TOKEN` | `GITHUB_TOKEN` 完全等价 | 用途相近，但 GitHub 权限由 `permissions` 和事件来源共同决定 | 显式声明最小权限，特别注意 Fork PR |
| `allow_failure` | GitHub 没这能力 | 用 `continue-on-error` 近似表达 | 区分 job 级和 step 级容错 |
| `retry` | GitHub 也有原生字段 | GitHub 无内置通用 `retry`，通常靠脚本或 action | 对 flaky 步骤单独设计重试 |
| `interruptible` | `concurrency` 完全等价 | `concurrency` 是运行组级的排队/取消机制 | 先设计 group，再决定是否取消旧 run |
| `include` / `extends` | 直接复制 workflow 文件 | GitHub 有 reusable workflow、composite action、Marketplace action 三层复用 | 复用 steps 用 composite，复用 jobs 用 reusable workflow |
| `artifacts` | `needs` 会顺带传产物 | GitHub artifact 需要显式上传和下载 | 始终把 artifact 上传/下载写清楚 |
| job logs | 只看控制台日志 | GitHub 还强调 annotations、Checks、summary | 用 `GITHUB_STEP_SUMMARY` 输出关键结论 |
| manual deploy | 只要 UI 上能点按钮就够了 | 手动入口、审批、部署凭据、环境隔离是四个独立问题 | 逐项校验 dispatch、environment、permissions、secrets |

建议记忆方式：凡是你想当然地把 GitLab 的一个字段，映射成 GitHub 的一个字段时，先停一下，问自己它到底属于这四类里的哪一类：触发器、运行时条件、执行环境、平台治理。

---

## 30. 如何编写自己的 GitHub Action

`actions/checkout@v4` 是 GitHub 官方维护的 Action。你也可以在自己的仓库中编写 Action，并通过本地路径调用：

```yaml
steps:
  - uses: ./.github/actions/custom-actions-examples/composite
    with:
      message: 'Hello from my repository'
```

当前仓库提供三种实现示例：

| 类型 | 主要文件 | 适合场景 |
|---|---|---|
| Composite Action | [composite/action.yml](.github/actions/custom-actions-examples/composite/action.yml) | YAML + shell，封装多个固定步骤，最容易维护 |
| JavaScript Action | [javascript/action.yml](.github/actions/custom-actions-examples/javascript/action.yml) + [index.js](.github/actions/custom-actions-examples/javascript/index.js) | Node.js 复杂逻辑、GitHub API、跨平台执行 |
| Docker Action | [docker/action.yml](.github/actions/custom-actions-examples/docker/action.yml) + [Dockerfile](.github/actions/custom-actions-examples/docker/Dockerfile) | 固定容器环境、跨语言工具 |

### 30.1 Composite Action

调用：

```yaml
- name: Run Composite Action
  id: composite
  uses: ./.github/actions/custom-actions-examples/composite
  with:
    message: 'Hello from Composite Action'

- run: echo "${{ steps.composite.outputs.result }}"
```

它的核心结构是：

```yaml
runs:
  using: composite
  steps:
    - shell: bash
      run: echo hello
```

可以把它理解为 GitLab 的公共 `script` 片段、YAML anchor 或简单 `extends` 模板。

### 30.2 JavaScript Action

调用：

```yaml
- name: Run JavaScript Action
  id: javascript
  uses: ./.github/actions/custom-actions-examples/javascript
  with:
    message: 'Hello from Node.js'

- run: echo "${{ steps.javascript.outputs.result }}"
```

JavaScript Action 的入口在 `action.yml` 中声明：

```yaml
runs:
  using: node20
  main: index.js
```

Action 输入会自动转换为环境变量。例如输入 `message` 在 Node.js 中读取为：

```javascript
process.env.INPUT_MESSAGE
```

输出通过 `$GITHUB_OUTPUT` 写回：

```javascript
fs.appendFileSync(process.env.GITHUB_OUTPUT, 'result=value\\n');
```

生产环境中，如果依赖第三方 npm 包，通常还需要 `package.json`、锁文件和打包后的 `dist/index.js`；本示例只使用 Node.js 内置模块，因此不需要安装依赖。

### 30.3 Docker Action

调用：

```yaml
- name: Count files with Docker Action
  id: files
  uses: ./.github/actions/custom-actions-examples/docker
  with:
    directory: src

- run: echo "Files: ${{ steps.files.outputs.file-count }}"
```

Docker Action 的结构是：

```text
docker/
├── action.yml
├── Dockerfile
└── entrypoint.sh
```

`action.yml` 指向 Dockerfile：

```yaml
runs:
  using: docker
  image: Dockerfile
```

它适合把 Python、Go、Shell 等工具放入固定容器环境。代价是每次运行通常有容器启动开销，而且需要注意 runner 的 Docker 能力。

### 30.4 三种 Action 如何选择

```text
多个固定 steps       → Composite Action
复杂 Node.js 逻辑    → JavaScript Action
特殊容器或跨语言工具 → Docker Action
```

与 GitLab 的复用能力对比：

| GitLab | GitHub Actions | 复用粒度 |
|---|---|---|
| YAML anchor / 简单 `extends` | Composite Action | 多个 steps |
| `include` + 公共 job 模板 | Reusable Workflow | 一个或多个 jobs |
| CI template / 社区组件 | Marketplace Action | 已发布的可复用 Action |

