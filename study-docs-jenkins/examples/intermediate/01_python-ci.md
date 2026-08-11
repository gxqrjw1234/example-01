# 示例 01：Python CI

## 文件

- Pipeline：[01_python-ci.groovy](01_python-ci.groovy)

## 难度

中级。适合第一个可运行的 Jenkinsfile。

## 覆盖知识点

- Declarative Pipeline、Agent label、SCM checkout。
- Python 虚拟环境、依赖安装、编译检查和 pytest。
- JUnit XML、Coverage XML、`archiveArtifacts`、`stash`。
- `timeout`、`disableConcurrentBuilds`、`buildDiscarder`、时间戳。
- `post { always }` 报告和 `cleanup` 工作区清理。
- 把临时文件、报告和正式产物分开管理。

## 前置条件

1. Jenkins 安装 Pipeline、Git、Credentials Binding、JUnit 等插件。
2. Agent 标签为 `linux-python`，且存在 Python 3 和 `venv`。
3. 仓库有 `requirements.txt`、可选的 `requirements-dev.txt`、`src/` 和 `tests/`。
4. 开发依赖包含 `pytest`、`pytest-cov`；生产项目建议固定版本并使用锁文件。

## 运行方式

1. 创建 Pipeline 或 Multibranch Pipeline Job。
2. 将文件内容保存为仓库根目录 `Jenkinsfile`，或者在 Pipeline Job 中选择 Pipeline script。
3. 第一次运行确认 Agent 能联网安装依赖，并查看 JUnit/Artifact 页面。
4. 当前工作区中的 Python POC 没有 `tests/` 和 pytest 依赖，直接套用时应先补充测试，或把测试命令替换为项目实际命令。

## 关键实践

- 用 `checkout scm` 让 Multibranch 使用当前提交，而不是硬编码分支。
- `set -Eeuo pipefail` 让失败正确传播；不要使用 `|| true` 吞掉测试失败。
- 产物使用 `stash` 只用于同一次 Build 的后续 Agent；长期发布应上传制品库。
- `archiveArtifacts` 不允许把 `.env`、私钥或临时凭据路径纳入匹配范围。
- `post { always }` 负责报告，最终 Build 是否成功仍由测试命令退出码决定。
