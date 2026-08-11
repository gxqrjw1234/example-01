# GitHub Actions 与 GitLab CI/CD 语法对比

面向 GitLab CI/CD 熟手、GitHub Actions 初学者。

这份文档重点解决三件事：

1. GitLab 与 GitHub Actions 的概念映射。
2. 常见语法的逐项对比。
3. 可直接复制的 sample code。

## 1. 核心概念映射

| GitLab CI/CD | GitHub Actions | 说明 |
|---|---|---|
| `.gitlab-ci.yml` | `.github/workflows/*.yml` | GitLab 常见单文件，GitHub 通常拆成多个 workflow 文件 |
| pipeline | workflow | 一次完整执行 |
| stage | 无严格等价物 | GitHub 更偏向用 `needs` 描述依赖图 |
| job | job | 基本对应 |
| script | step 中的 `run` | GitHub 会把 job 拆成多个 steps |
| runner | runner | 都支持托管和自托管 |
| `tags` | `runs-on` / self-hosted labels | 指定执行节点 |
| `only` / `except` | `on` | workflow 触发条件 |
| `rules` | `if` | job / step 条件执行 |
| `variables` | `env` / `vars` / `secrets` | GitHub 区分更细 |
| `artifacts` | `upload-artifact` / `download-artifact` | GitHub 需要显式上传和下载 |
| `cache` | `actions/cache` 或 setup action 自带 cache | 缓存实现方式不同 |
| `include` | reusable workflow / action | GitHub 的复用方式更多 |
| `when: manual` | `workflow_dispatch` | 手动触发 |
| schedule | `schedule` | cron 定时触发 |
| protected environment | `environment` + Settings | 审批和保护多在平台设置里完成 |

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

1. GitHub Actions 默认不会自动 checkout 代码。
2. GitHub Actions 的 job 必须显式写 `steps`。
3. GitHub Actions 更依赖 `uses:` 去调用已有 action。

## 3. 触发条件语法对比

### GitLab

```yaml
build_main:
  script:
    - echo "build main"
  only:
    - main
```

或：

```yaml
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

jobs:
  build_main:
    runs-on: ubuntu-latest
    steps:
      - run: echo "build main"
```

如果 workflow 已经启动，但只想让某个 job 在主分支执行：

```yaml
on:
  push:
    branches: ['*']

jobs:
  build_main:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: echo "build main"
```

理解方式：

1. `on:` 决定 workflow 什么时候启动。
2. `if:` 决定 job 或 step 要不要执行。

## 4. Stage 与 `needs`

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

GitHub Actions 没有必须先声明 stage 的模型。更实用的心智模型是 DAG，也就是依赖图。

## 5. Runner 与执行环境

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

经验上这样理解最稳：

1. GitLab 常以 `image:` 为核心。
2. GitHub Hosted Runner 更像预装很多工具的虚拟机。
3. GitHub 也支持容器 job，但很多项目直接 `runs-on + setup-*` 就够了。

## 6. 变量、密钥、上下文

### GitLab

```yaml
variables:
  APP_ENV: production

deploy:
  script:
    - echo $APP_ENV
    - echo $CI_COMMIT_SHA
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
```

常见变量映射：

| GitLab | GitHub Actions |
|---|---|
| `$CI_COMMIT_SHA` | `${{ github.sha }}` |
| `$CI_COMMIT_BRANCH` | `${{ github.ref_name }}` |
| `$CI_PROJECT_PATH` | `${{ github.repository }}` |
| `$CI_PIPELINE_ID` | `${{ github.run_id }}` |
| `$GITLAB_USER_LOGIN` | `${{ github.actor }}` |

GitHub Actions 里常见三类变量来源：

1. `env`: workflow / job / step 级环境变量。
2. `vars`: 仓库级或 environment 级普通变量。
3. `secrets`: 加密变量。

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

## 7. Artifact 对比

### GitLab

```yaml
build:
  script:
    - npm run build
  artifacts:
    paths:
      - dist/
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

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: dist
          path: dist/
```

重点差异：`needs` 只表达依赖关系，不负责传文件。

## 8. Cache 对比

### GitLab

```yaml
cache:
  key: npm-cache
  paths:
    - .npm/

test:
  script:
    - npm ci --cache .npm
```

### GitHub Actions

```yaml
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-node@v4
    with:
      node-version: '20'
      cache: 'npm'
  - run: npm ci
```

或显式使用 `actions/cache`：

```yaml
steps:
  - uses: actions/cache@v4
    with:
      path: ~/.npm
      key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
      restore-keys: |
        ${{ runner.os }}-npm-
```

Node 项目一般优先用 `actions/setup-node` 的内置缓存。

## 9. 矩阵并行对比

### GitLab

```yaml
test:
  parallel:
    matrix:
      - NODE_VERSION: [18, 20, 22]
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
      fail-fast: false
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
      - run: node -v
      - run: npm test
```

GitHub Actions 的矩阵变量读取方式是 `${{ matrix.xxx }}`。

## 10. 条件执行对比

### GitLab

```yaml
deploy_prod:
  script:
    - ./deploy.sh
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
      when: manual
```

### GitHub Actions

```yaml
on:
  workflow_dispatch:

jobs:
  deploy_prod:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: ./deploy.sh
```

常用 GitHub 表达式：

```yaml
if: github.ref == 'refs/heads/main'
if: github.event_name == 'pull_request'
if: startsWith(github.ref, 'refs/tags/v')
if: contains(github.event.head_commit.message, '[deploy]')
if: failure()
if: always()
```

## 11. 手动触发对比

### GitLab

```yaml
deploy_prod:
  script:
    - ./deploy.sh prod
  when: manual
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

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy to ${{ inputs.environment }}"
```

GitHub Actions 的手动触发天然支持参数输入。

## 12. 定时任务对比

### GitLab

GitLab 常在平台界面中配置 Pipeline Schedule。

### GitHub Actions

```yaml
on:
  schedule:
    - cron: '0 2 * * *'

jobs:
  nightly:
    runs-on: ubuntu-latest
    steps:
      - run: echo nightly job
```

GitHub Actions 在这块更偏配置即代码。

## 13. 复用能力对比

### GitLab

```yaml
include:
  - local: .gitlab/common.yml

.base_job:
  image: node:20
  before_script:
    - npm ci

test:
  extends: .base_job
  script:
    - npm test
```

### GitHub Actions

#### Composite Action

适合复用一组 steps。

```yaml
steps:
  - uses: ./.github/actions/setup-node-cached
    with:
      node-version: '20'
```

#### Reusable Workflow

适合复用整段 job 或整段部署流程。

调用方：

```yaml
jobs:
  deploy_prod:
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: production
    secrets:
      deploy_token: ${{ secrets.DEPLOY_TOKEN }}
```

被调用方：

```yaml
on:
  workflow_call:
    inputs:
      environment:
        type: string
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy to ${{ inputs.environment }}"
```

#### Marketplace Action

```yaml
steps:
  - uses: docker/login-action@v3
  - uses: docker/build-push-action@v5
```

## 14. Environment 与审批

### GitLab

```yaml
deploy_prod:
  environment:
    name: production
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

GitHub Actions 的审批能力主要来自仓库 Settings 中的 environment protection rules。

## 15. 完整对照 Sample

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

test:
  stage: test
  image: node:20
  script:
    - npm ci
    - npm test

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

env:
  APP_ENV: production

jobs:
  lint:
    runs-on: ubuntu-latest
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
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npm test

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
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: dist
          path: dist/
      - run: ./deploy.sh
```

## 16. 从 GitLab 迁移到 GitHub 最常见的坑

1. 忘记写 `actions/checkout`。
2. 以为 `needs` 会自动传 artifact，实际上不会。
3. 把 `workflow_dispatch` 写到 job 下面，而不是 `on` 下面。
4. 把 shell 变量写法和 GitHub expression 写法混用。
5. 在 `if:` 里继续沿用 `$CI_*` 思维。
6. 过度依赖 stage，而没有把 workflow 写成依赖图。
7. 不区分 `env`、`vars`、`secrets`。

## 17. 在这个仓库里继续学习

可以直接对照这些样例：

1. [github-actions-vs-gitlab-ci.md](github-actions-vs-gitlab-ci.md)：这份语法对照文档。
2. [.github/workflows/ci.yml](.github/workflows/ci.yml)：CI、矩阵、artifact。
3. [.github/workflows/cd.yml](.github/workflows/cd.yml)：CD、environment、reusable workflow。
4. [.github/workflows/reusable-deploy.yml](.github/workflows/reusable-deploy.yml)：`workflow_call`。
5. [.github/actions/setup-node-cached/action.yml](.github/actions/setup-node-cached/action.yml)：composite action。

如果你下一步希望更贴近迁移实战，我可以继续补两类内容：

1. GitLab `.gitlab-ci.yml` 到 GitHub Actions 的逐段迁移模板。
2. Node.js、Java Maven、Docker 三套 GitHub Actions 常用模板。