# 高级示例 06：Jenkins Configuration as Code

## 文件

- 配置：[06_jcasc-baseline.yaml](06_jcasc-baseline.yaml)

## 覆盖知识点

- JCasC 管理 Controller 基线配置。
- Controller `0` Executor，禁止匿名读取，开启 CSRF Crumb 和 Remoting Security。
- 外部环境变量注入管理员账号、密码、Jenkins URL 和管理员地址。
- 全局 Shared Library 固定版本。
- JCasC、插件 schema、Job DSL、Credentials 和 Shared Library 的边界。

## 前置条件

- 安装 Configuration as Code 插件以及与配置相关的插件。
- 使用与当前 Jenkins LTS/插件兼容的 Java 和 JCasC 版本。
- 通过 `CASC_JENKINS_CONFIG` 指向 YAML 文件。
- 在运行环境中注入 `JENKINS_ADMIN_ID`、`JENKINS_ADMIN_PASSWORD`、`JENKINS_URL`、`JENKINS_ADMIN_EMAIL`；密码来自 Secret Manager，不来自 Git。
- 将 `git.example.com`、Shared Library 仓库、认证方式和 JCasC schema 替换为实际配置。

## 验证建议

1. 在一次性测试 Controller 加载 YAML，查看 JCasC export/validation 日志。
2. 检查匿名访问、登录、Folder/Job 权限、SCM 扫描和 Shared Library 解析。
3. 用真实 Jenkinsfile 验证 `@Library('company-ci@v3.4.0')` 和插件 Step。
4. 变更前导出/备份配置，变更后做恢复演练。

## 关键实践

- 该文件只展示结构，不能作为生产完整安全配置直接使用。
- JCasC 环境变量替换不等于 Secret 加密；运行时注入 Secret，并限制容器日志和进程环境访问。
- `loggedInUsersCanDoAnything` 只是学习用的粗粒度授权，企业应使用 Matrix/Role-based Authorization Strategy 和 Folder 权限。
- JCasC 管理 Controller 全局配置；Job DSL/Organization Folder 管理 Job；Shared Library 管理流水线逻辑，三者不要混为一体。
- 每次插件升级都要用配置 lint、启动测试和代表性 Pipeline 验证；未知字段应让发布失败，而不是静默忽略。
