# 高级示例 04：安全扫描、SBOM、签名和质量门禁

## 文件

- Pipeline：[04_secure-supply-chain.groovy](04_secure-supply-chain.groovy)

## 覆盖知识点

- SAST、依赖漏洞扫描、Secret Scan 并行执行。
- Docker 镜像构建、Trivy 阻断式扫描和 Registry 凭据。
- Syft 生成 CycloneDX SBOM。
- Cosign 使用密钥签名镜像并 attestation SBOM。
- SonarQube `waitForQualityGate`、超时和失败传播。
- provenance：源代码 SHA、Build Number、镜像 digest、扫描结果和签名。
- 只在最小阶段绑定 Registry/Cosign 凭据。

## 前置条件

- Agent 标签 `linux-supply-chain`，安装 Docker/BuildKit、Sonar Scanner、Dependency-Check、Gitleaks、Syft、Trivy、Cosign。
- SonarQube Server 配置名称和 Webhook 已完成，才能使 `waitForQualityGate` 返回。
- Credentials：`container-registry-push`、`cosign-key`、`cosign-password`。
- Pipeline 账号只能推送指定镜像仓库、使用指定签名凭据和读取必要的质量服务。
- `reports/` 目录允许工具写入，测试项目需额外生成 JUnit XML 才能展示测试结果。

## 关键实践

- 示例展示的是安全链路骨架，不是完整合规方案；需要根据组织阈值、漏洞例外、签名验证策略和供应链标准落地。
- 扫描工具、基础镜像和 Jenkins 插件同样属于供应链，固定版本并定期升级。
- `image-digest.txt` 是后续部署的唯一产物坐标；部署时验证签名和 attestation，不能只相信标签。
- Cosign 私钥应优先托管在 KMS/HSM/Keyless OIDC 流程中；示例的 Secret File 仅用于学习。
- `set +x` 只覆盖 Secret 操作，日志/报告/构建描述仍不得包含密码或私钥。
- Scanner 不可用和发现漏洞应分开处理，工具故障不能被无条件标记为成功。
