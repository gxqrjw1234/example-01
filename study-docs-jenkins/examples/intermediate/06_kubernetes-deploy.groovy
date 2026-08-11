// Jenkinsfile 示例：使用 Helm 将已构建镜像部署到 Kubernetes
// 构建与部署分离：本 Pipeline 不重新构建镜像，只接收不可变 digest。

pipeline {
    agent none

    parameters {
        choice(name: 'TARGET_ENV', choices: ['dev', 'staging'], description: '目标环境')
        string(name: 'IMAGE_DIGEST', defaultValue: 'sha256:replace-me', description: '已扫描并签名的镜像 digest')
    }

    options {
        timestamps()
        timeout(time: 25, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Validate release input') {
            agent { label 'linux-kubectl' }
            steps {
                script {
                    if (!(params.TARGET_ENV in ['dev', 'staging'])) {
                        error 'Only dev and staging are allowed in this example'
                    }
                    if (!(params.IMAGE_DIGEST ==~ /sha256:[0-9a-f]{64}/)) {
                        error 'IMAGE_DIGEST must be a full sha256 digest'
                    }
                }
            }
        }

        stage('Helm lint and render') {
            agent { label 'linux-kubectl' }
            steps {
                checkout scm
                withEnv([
                    "DEPLOY_ENV=${params.TARGET_ENV}",
                    "IMAGE_DIGEST=${params.IMAGE_DIGEST}"
                ]) {
                    sh '''
                        set -Eeuo pipefail
                        helm lint charts/app
                        helm template app charts/app \\
                          --namespace "$DEPLOY_ENV" \\
                          --set image.digest="$IMAGE_DIGEST" \\
                          > rendered.yaml
                        kubectl apply --dry-run=client -f rendered.yaml
                    '''
                    stash name: 'deploy-assets', includes: 'charts/**,scripts/smoke-test.sh,rendered.yaml', useDefaultExcludes: false
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Deploy') {
            agent { label 'linux-kubectl' }
            steps {
                deleteDir()
                unstash 'deploy-assets'
                lock(resource: "kubernetes-${params.TARGET_ENV}") {
                    withCredentials([file(
                        credentialsId: "kubeconfig-${params.TARGET_ENV}",
                        variable: 'KUBECONFIG'
                    )]) {
                        withEnv([
                            "DEPLOY_ENV=${params.TARGET_ENV}",
                            "IMAGE_DIGEST=${params.IMAGE_DIGEST}"
                        ]) {
                            sh '''
                                set -Eeuo pipefail
                                helm upgrade --install app charts/app \\
                                  --namespace "$DEPLOY_ENV" \\
                                  --create-namespace \\
                                  --set image.digest="$IMAGE_DIGEST" \\
                                  --atomic --timeout 10m
                                kubectl -n "$DEPLOY_ENV" rollout status \\
                                  deployment/app --timeout=5m
                            '''
                        }
                    }
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Smoke test') {
            agent { label 'linux-kubectl' }
            steps {
                deleteDir()
                unstash 'deploy-assets'
                sh './scripts/smoke-test.sh "${TARGET_ENV}"'
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }
    }

    post {
        always {
            echo "environment=${params.TARGET_ENV}, digest=${params.IMAGE_DIGEST}"
        }
    }
}
