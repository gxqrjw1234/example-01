# 示例 06：Kubernetes/Helm 部署

## 文件

- Pipeline：[06_kubernetes-deploy.groovy](06_kubernetes-deploy.groovy)

## 难度

中级到中高级。对应 GitHub Actions 的 Kubernetes context、manifest bake/deploy、环境选择和并发保护。

## 覆盖知识点

- 参数化环境和不可变镜像 digest 校验。
- Helm lint、template、Kubernetes dry-run。
- Jenkins File Credentials 绑定 kubeconfig。
- Lockable Resources 防止同一环境并发部署。
- `helm upgrade --install --atomic`、rollout status 和 smoke test。
- 构建与部署分离、`post` 清理和最小部署 Agent。

## 前置条件

- Agent 标签 `linux-kubectl`，安装 Helm、kubectl 和访问 Registry 的能力。
- 凭据 ID `kubeconfig-dev`、`kubeconfig-staging`，只能被相应 Folder/Job 使用。
- Lockable Resources 中创建 `kubernetes-dev`、`kubernetes-staging`。
- 仓库包含 `charts/app`，Deployment 名称为 `app`，以及 `scripts/smoke-test.sh`。
- Kubeconfig 对目标 namespace 最小授权，生产环境不要复用本示例的 dev/staging 配置。

## 关键实践

- Pipeline 只接收已构建、扫描、签名的 digest，不在部署阶段重新 build。
- `--dry-run` 是渲染验证，不是完整的 Admission/运行时验证；生产还应使用策略检查和真实集群 smoke test。
- `--atomic` 对可回滚的 Kubernetes 对象有效，数据库迁移、外部 DNS 等不可逆操作要单独设计。
- kubeconfig 是 Secret file，禁止打印、归档、写入镜像或持久化到工作区。
- 生产部署应增加受保护 Job、审批、变更窗口和回滚演练；本示例只开放 dev/staging。
