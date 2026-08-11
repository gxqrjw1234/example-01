# 高级示例 05：Kubernetes 动态 Pod Agent

## 文件

- Pipeline：[05_kubernetes-pod-agent.groovy](05_kubernetes-pod-agent.groovy)

## 覆盖知识点

- Kubernetes Plugin 的 `podTemplate`、`node(POD_LABEL)` 和 `container`。
- 动态、短生命周期 Agent；Builder 和 kubectl sidecar 分工。
- Pod ServiceAccount、`automountServiceAccountToken`、非 root、seccomp、只读根文件系统和资源限制。
- 多容器共享 workspace、stash/unstash、测试报告和产物归档。
- Kubernetes Agent 的容量、镜像、网络和安全边界。

## 前置条件

- Jenkins Controller 安装 Kubernetes Plugin，并能访问目标 Kubernetes 集群。
- 配置 Pod Template 默认 namespace、Jenkins URL、连接方式和 Cloud。
- 集群存在受限 `jenkins-builder` ServiceAccount；示例没有授予部署权限。
- Agent 镜像 `ghcr.io/example/ci-builder:3.12` 包含 Python、pip 和项目工具；替换为内部固定 digest 镜像。
- `bitnami/kubectl` 和测试仓库路径按实际项目替换。

## 关键实践

- Agent Pod 不应挂载宿主 Docker socket；需要镜像构建时优先 Kaniko、BuildKit rootless 或云构建服务。
- `automountServiceAccountToken: false` 让只执行构建的 Pod 不自动获得 Kubernetes API Token；部署应使用独立的受信 Job/Pod。
- 生产镜像固定 digest，构建镜像使用非 root 用户、最小包和漏洞扫描。
- 为 Pod 设置 requests/limits、NetworkPolicy、Pod Security 和节点隔离，避免一个构建耗尽集群。
- 动态 Agent 可能在中断时消失；流程要允许从不可变源码/制品重新执行，而不是依赖 Pod 本地状态。
- 不可信 PR 使用完全不同的 Pod Template 和 ServiceAccount，不能沿用生产部署 Pod。
