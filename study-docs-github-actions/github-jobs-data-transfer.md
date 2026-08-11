# GitHub Actions 不同 Job 之间如何传递信息

GitHub Actions 中，不同 job 通常运行在不同的 runner 上，因此不能直接共享工作目录。需要根据传递内容的类型，选择不同机制。

## 1. 传递字符串：Job Outputs

适合传递：

- 版本号
- 镜像 tag
- 镜像 digest
- 部署 URL
- 环境名称
- 布尔判断结果

### 示例

```yaml
name: Job Outputs Demo

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.version.outputs.version }}
      image-tag: ${{ steps.version.outputs.image-tag }}
    steps:
      - name: Calculate version
        id: version
        run: |
          echo "version=1.2.${GITHUB_RUN_NUMBER}" >> "$GITHUB_OUTPUT"
          echo "image-tag=ghcr.io/${GITHUB_REPOSITORY}:sha-${GITHUB_SHA}" >> "$GITHUB_OUTPUT"

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Read outputs from build
        run: |
          echo "Version: ${{ needs.build.outputs.version }}"
          echo "Image: ${{ needs.build.outputs.image-tag }}"
```

数据流：

```text
step 写入 $GITHUB_OUTPUT
        ↓
job.outputs 暴露输出
        ↓
needs.build.outputs.<name> 读取
```

关键点：

```yaml
jobs:
  build:
    outputs:
      version: ${{ steps.version.outputs.version }}
```

其中：

- `steps.version` 是 step 的 `id`。
- `steps.version.outputs.version` 是该 step 写入的输出。
- 下游 job 必须声明 `needs: build`，才能读取 `needs.build.outputs.version`。

## 2. 传递文件：Artifacts

适合传递：

- `dist/` 构建目录
- 测试报告
- coverage 报告
- 安装包
- 日志文件
- Docker 构建上下文

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run build

      - name: Upload build artifact
        uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/
          retention-days: 7
          if-no-files-found: error

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Download build artifact
        uses: actions/download-artifact@v4
        with:
          name: dist
          path: dist/

      - name: Inspect artifact
        run: ls -la dist/
```

GitLab 对照：

```text
GitLab artifacts + dependencies
        ≈
GitHub upload-artifact + needs + download-artifact
```

`needs` 只表示 job 依赖关系，不会自动传递文件。

## 3. 同一个 Job 内传递信息：GITHUB_ENV

`GITHUB_ENV` 只适用于同一个 job 中，当前 step 之后的后续 steps。

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Set environment variable
        run: echo "APP_VERSION=1.2.3" >> "$GITHUB_ENV"

      - name: Read environment variable
        run: echo "Version is $APP_VERSION"
```

它不会自动跨 job：

```text
Step A --GITHUB_ENV--> Step B       可以
Job A  --GITHUB_ENV--> Job B        不可以
```

跨 job 传递字符串时，应使用 Job Outputs。

## 4. 同一个 Job 内传递 Step 输出：GITHUB_OUTPUT

如果只是当前 job 的 step 之间传值，可以使用 step output：

```yaml
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Generate release name
        id: release
        run: echo "name=release-${GITHUB_RUN_NUMBER}" >> "$GITHUB_OUTPUT"

      - name: Use release name
        run: echo "${{ steps.release.outputs.name }}"
```

区别：

| 作用范围 | 写入方式 | 读取方式 |
|---|---|---|
| 当前 job 后续 steps | `$GITHUB_ENV` | `$APP_VERSION` |
| 当前 job 的 step 输出 | `$GITHUB_OUTPUT` | `${{ steps.<id>.outputs.<name> }}` |
| 不同 job 的字符串 | `$GITHUB_OUTPUT` + job `outputs` | `${{ needs.<job>.outputs.<name> }}` |
| 不同 job 的文件 | `upload-artifact` | `download-artifact` |

## 5. 使用 Cache：用于加速，不用于传递业务结果

Cache 适合缓存依赖和中间文件，例如 npm 缓存：

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
      - run: npm ci
```

也可以显式使用 `actions/cache`：

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-npm-
```

不要使用 Cache 传递必须部署的 `dist/`，因为 Cache 的目标是加速，不能替代构建产物管理。需要可靠传递的文件应使用 Artifact。

## 6. 传递 Secret

Secrets 不应该写入 outputs、artifacts 或日志。应通过 `secrets` 或 `env` 注入到需要它的 job/step：

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy
        run: ./deploy.sh
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
```

对于 reusable workflow，需要在调用方显式传递 secret：

```yaml
jobs:
  deploy:
    uses: ./.github/workflows/reusable-deploy.yml
    secrets:
      deploy_token: ${{ secrets.DEPLOY_TOKEN }}
```

不要这样做：

```yaml
# 不要把 secret 写入 output 或日志
- run: echo "token=${{ secrets.DEPLOY_TOKEN }}" >> "$GITHUB_OUTPUT"
```

## 7. 失败 Job 后继续传递结果

默认情况下，如果 `build` 失败，依赖它的 `deploy` 会被跳过。若要让下游 job 在失败后仍执行，需要使用 `always()`，并明确判断依赖结果：

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: npm run build

  report:
    needs: build
    if: ${{ always() }}
    runs-on: ubuntu-latest
    steps:
      - name: Show build result
        run: echo "Build result: ${{ needs.build.result }}"
```

常见结果值：

- `success`
- `failure`
- `cancelled`
- `skipped`

生产部署通常不应使用无条件 `always()` 绕过失败依赖；应先检查：

```yaml
if: ${{ needs.build.result == 'success' }}
```

## 8. 多个上游 Job 的输出

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    outputs:
      status: ${{ steps.result.outputs.status }}
    steps:
      - id: result
        run: echo "status=passed" >> "$GITHUB_OUTPUT"

  test:
    runs-on: ubuntu-latest
    outputs:
      status: ${{ steps.result.outputs.status }}
    steps:
      - id: result
        run: echo "status=passed" >> "$GITHUB_OUTPUT"

  summary:
    needs: [lint, test]
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Lint: ${{ needs.lint.outputs.status }}"
          echo "Test: ${{ needs.test.outputs.status }}"
```

## 9. 不同 Workflow 之间传递信息

`needs` 只能用于同一个 workflow 内的 job，不能直接跨 workflow 使用。

### 9.1 `workflow_run` 能提供什么，不能提供什么

`workflow_run` 事件触发时，`github.event.workflow_run` 只包含上游 workflow run 的元数据：

| 字段 | 含义 |
|---|---|
| `github.event.workflow_run.id` | run ID |
| `github.event.workflow_run.head_sha` | 触发 commit SHA |
| `github.event.workflow_run.conclusion` | `success` / `failure` / `cancelled` |
| `github.event.workflow_run.name` | 工作流名称 |
| `github.event.workflow_run.head_branch` | 分支名 |

**不包含**：上游 workflow 的 job outputs、step outputs、环境变量。这些数据不会出现在事件 payload 里。

原因是两个 workflow 运行在完全独立的 runner 上，runner 进程结束后内存状态即销毁。`workflow_run` 只是一个触发信号，不是进程间通信通道。

因此，**跨 workflow 传递数据不能依赖 job outputs，只能使用外部持久化存储**：Artifact、Package Registry、API 等。

### 9.2 `workflow_run` 触发时的简化方案

当 CD 由 CI 完成后自动触发时，`run_id` 直接可用，不需要 API 查找：

```yaml
# CD workflow
on:
  workflow_run:
    workflows: ['CI']
    types: [completed]

jobs:
  pull-image:
    runs-on: ubuntu-latest
    if: github.event.workflow_run.conclusion == 'success'
    steps:
      - name: Download artifact from CI
        uses: actions/download-artifact@v4
        with:
          name: image-meta-${{ github.event.workflow_run.head_sha }}
          path: image-meta/
          github-token: ${{ secrets.GITHUB_TOKEN }}
          repository: ${{ github.repository }}
          run-id: ${{ github.event.workflow_run.id }}   # 直接可用
```

### 9.3 `workflow_dispatch` 手动触发的复杂性

手动触发时没有关联的上游 run，必须主动查找。这是额外复杂度的来源，不是架构本身的问题：

```yaml
- name: Find successful CI run for this commit
  id: ci_run
  if: github.event_name == 'workflow_dispatch'
  uses: actions/github-script@v7
  with:
    script: |
      const sha = process.env.CI_SHA;          // 从环境变量读，防止注入
      const { data } = await github.rest.actions.listWorkflowRunsForRepo({
        owner: context.repo.owner,
        repo: context.repo.repo,
        event: 'push',
        head_sha: sha,
        status: 'success',
        per_page: 100
      });
      const run = data.workflow_runs.find(r =>
        r.name === 'CI' && r.head_branch === 'main'
      );
      if (!run) { core.setFailed('No CI run found'); return; }
      core.setOutput('run_id', String(run.id));
  env:
    CI_SHA: ${{ env.CI_SHA }}
```

注意 `CI_SHA` 通过 `env` 传入脚本而不是直接嵌入表达式，是为了防止脚本注入：

```javascript
// 安全：通过环境变量读取
const sha = process.env.CI_SHA;

// 危险：直接嵌入可能带引号、换行等注入字符
const sha = '${{ env.CI_SHA }}';
```

### 9.4 跨 Workflow 常见方案对比

| 方案 | 适合传递 | 适用场景 | 限制 |
|---|---|---|---|
| **Artifact** | 文件、元数据 | CI 产物 → CD 下载 | 需要 `run_id`；有保留期限 |
| **Package Registry（GHCR）** | Docker 镜像、包 | 生产镜像发布 | 只适合产物，不适合元数据 |
| **Repository Dispatch** | 少量元数据（payload） | 外部系统触发 | payload 上限 65 KB |
| **workflow_call** | inputs + secrets | 复用 workflow 定义 | 调用方必须显式传参 |
| **Repository Variables（API 写入）** | 简单 key-value | 跨 run 共享配置 | 并发写入有竞争风险 |

### 方案一：使用 workflow Artifact（官方推荐）

适合由一个 workflow 生成文件、另一个 workflow 下载文件的场景。下载时需要指定 `run-id`，来源取决于触发方式：

```yaml
run-id: >-
  ${{ github.event_name == 'workflow_run'
      && github.event.workflow_run.id
      || steps.ci_run.outputs.run_id }}
```

### 方案二：使用 Package Registry

例如：

- Docker 镜像推送到 GHCR
- npm 包发布到 GitHub Packages

部署 workflow 再按 commit SHA 或 digest 拉取产物。这通常比跨 workflow 传 artifact 更适合生产发布，因为镜像本身就是产物，不需要额外传递文件。

### 方案三：使用 Repository Dispatch

一个 workflow 完成后，使用 GitHub API 触发另一个 workflow，并在事件 payload 中传递少量元数据：

```bash
curl -X POST \
  -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" \
  https://api.github.com/repos/${{ github.repository }}/dispatches \
  -d '{"event_type":"deploy","client_payload":{"sha":"${{ github.sha }}","image_tag":"ghcr.io/org/repo:sha-abc"}}'
```

接收方：

```yaml
on:
  repository_dispatch:
    types: [deploy]

jobs:
  deploy:
    steps:
      - run: echo "${{ github.event.client_payload.image_tag }}"
```

### 方案四：使用 workflow_call

`workflow_call` 适合复用 workflow 定义，由调用方通过 `with` 和 `secrets` 传入参数：

```yaml
# 调用方
jobs:
  deploy:
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: production
      image_tag: ${{ needs.build.outputs.image_tag }}
    secrets:
      deploy_token: ${{ secrets.DEPLOY_TOKEN }}
```

`workflow_call` 不是两个独立运行之间的通信通道，它本质上是 job 的复用，数据仍在同一 workflow run 的上下文内流转。

## 10. GitLab 与 GitHub Actions 对照

| GitLab | GitHub Actions | 用途 |
|---|---|---|
| `artifacts` | `upload-artifact` / `download-artifact` | 传递文件 |
| `dependencies` | `needs` + artifact actions | 等待依赖并传文件 |
| dotenv artifact | job outputs | 传递字符串和变量 |
| 全局变量 | `env` | workflow/job/step 环境变量 |
| `before_script` 变量 | `$GITHUB_ENV` | 当前 job 后续 steps |
| cache | `actions/cache` 或 setup action 内置 cache | 加速依赖安装 |
| CI/CD variables | `vars` / `secrets` | 普通配置和敏感配置 |
| trigger variables | `workflow_call.inputs` 或 dispatch payload | 跨流程传参数 |

## 11. 选择机制的速记表

```text
传一个字符串         → Job Outputs
传一个文件/目录      → Artifact
传当前 Job 的变量    → GITHUB_ENV
传当前 Step 的结果   → GITHUB_OUTPUT
加速依赖安装         → Cache
传密钥               → secrets + env
跨 Workflow 元数据   → dispatch payload / API
跨 Workflow 构建物   → Package Registry 或指定 run-id 的 Artifact
```

最重要的记忆方式：

> `needs` 负责依赖关系，`outputs` 负责字符串，`artifacts` 负责文件，`GITHUB_ENV` 负责当前 job 的环境变量，Cache 只负责加速。

跨 Workflow 的关键限制：

> `workflow_run` 事件只携带上游 run 的元数据（id、sha、conclusion），**不携带 job outputs 和环境变量**。跨 workflow 传递数据必须依赖 Artifact、Package Registry 或 dispatch payload 等外部持久化手段。`workflow_dispatch` 手动触发时额外需要通过 API 查找上游 run_id。
