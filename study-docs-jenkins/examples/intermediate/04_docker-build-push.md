# 示例 04：Docker 构建、扫描与推送

## 文件

- Pipeline：[04_docker-build-push.groovy](04_docker-build-push.groovy)

## 难度

中级。对应 GitHub Actions 的 Docker login、build/push、Trivy 扫描、metadata 和 Registry 凭据。

## 覆盖知识点

- Docker BuildKit、镜像构建和 Registry 登录。
- `withCredentials(usernamePassword)` 和 `--password-stdin`。
- 以完整 Git SHA 作为不可变镜像标签。
- Trivy HIGH/CRITICAL 扫描门禁。
- 获取 RepoDigest、生成 metadata、归档和 fingerprint。
- `try/finally` 登出、`timeout`、并发限制和工作区清理。

## 前置条件

- Agent 标签 `linux-docker-trusted`，可访问 Docker daemon。
- Registry 凭据 ID 为 `container-registry-push`，只有该 Job/Folder 可使用。
- Agent 安装 Docker CLI、Registry 访问能力和 Trivy。
- 允许 Agent 访问 `registry.example.com`，并将示例镜像名替换为真实仓库。

## 运行方式

1. 将 `.groovy` 内容作为仓库根目录 `Jenkinsfile`。
2. 修改 Registry、镜像仓库和 Credentials ID。
3. 运行后查看 `image-metadata.properties` 和 `image-digest.txt`。
4. 后续部署使用 metadata 中的 digest，不重新构建镜像。

## 关键实践

- `docker login` 使用标准输入，不把密码放入命令行参数；`set +x` 只在 Secret 操作附近关闭跟踪。
- `IMAGE_REF` 使用完整提交 SHA，避免 `latest` 造成不可追踪发布。
- 推送后保存 digest；仅使用本地 image ID 不能证明 Registry 中的最终对象。
- 不要把 Docker socket 暴露给不可信 PR。更强的隔离方案是 Kaniko、BuildKit rootless 或云构建服务。
- 示例中的 `trivy` 是阻断式扫描；生产需要定义漏洞例外、误报和到期责任人。
- `docker logout` 和 `deleteDir()` 只减少残留风险，不能代替短生命周期 Agent 和节点隔离。
