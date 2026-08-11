// Jenkinsfile 示例：Kubernetes Plugin 动态 Pod Agent
// 这是 Scripted Pipeline，展示 podTemplate/node/container 的动态 Agent 模式。
// Kubernetes 集群、Namespace、ServiceAccount 和镜像仓库需按组织安全基线配置。

def podYaml = '''
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins-build
spec:
  serviceAccountName: jenkins-builder
  automountServiceAccountToken: false
  securityContext:
    runAsNonRoot: true
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: builder
    image: ghcr.io/example/ci-builder:3.12
    command: ["sleep"]
    args: ["99d"]
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
    resources:
      requests:
        cpu: "500m"
        memory: "1Gi"
      limits:
        cpu: "2"
        memory: "4Gi"
  - name: kubectl
    image: bitnami/kubectl:1.31.3
    command: ["sleep"]
    args: ["99d"]
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
  volumes:
  - name: workspace
    emptyDir: {}
'''

podTemplate(yaml: podYaml, idleMinutes: 0) {
    node(POD_LABEL) {
        timeout(time: 40, unit: 'MINUTES') {
            stage('Checkout') {
                checkout scm
            }

            stage('Build and test') {
                container('builder') {
                    sh '''
                        set -Eeuo pipefail
                        python --version
                      mkdir -p reports
                      # builder 镜像应预装项目依赖；不要在受限构建 Pod 中依赖公网安装。
                        python -m pytest tests/ --junitxml=reports/junit.xml
                        python src/build.py
                    '''
                    stash name: 'pod-dist', includes: 'dist/**,reports/**', allowEmpty: false
                }
            }

            stage('Policy check') {
                container('kubectl') {
                    sh '''
                        set -Eeuo pipefail
                        kubectl version --client=true
                        # 生产项目可在这里调用 kubeconform/conftest/OPA，
                        # 不要把 kubeconfig 或云凭据放进不可信 PR Pod。
                    '''
                }
            }

            stage('Publish') {
                container('builder') {
                    unstash 'pod-dist'
                    archiveArtifacts artifacts: 'dist/**,reports/**', fingerprint: true
                }
            }
        }
    }
}
