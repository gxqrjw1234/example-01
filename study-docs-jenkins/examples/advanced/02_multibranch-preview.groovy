// Jenkinsfile 示例：Multibranch Pipeline 的 PR 预览环境和主分支发布
// 仅在受信任的部署 Job/Agent 中使用部署凭据；PR 构建阶段不读取生产凭据。

pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '25', artifactNumToKeepStr: '10'))
    }

    environment {
        IMAGE_REPOSITORY = 'registry.example.com/team/catalog'
        PREVIEW_BASE_DOMAIN = 'preview.example.com'
    }

    stages {
        stage('Checkout and test') {
            agent { label 'linux-pr-untrusted' }
            steps {
                checkout scm
                sh './scripts/test.sh'
                stash name: 'source', includes: '**/*', useDefaultExcludes: false
            }
            post {
                always {
                    junit testResults: 'reports/junit.xml', allowEmptyResults: true
                }
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Build candidate') {
            agent { label 'linux-docker-untrusted' }
            steps {
                deleteDir()
                unstash 'source'
                sh '''
                    set -Eeuo pipefail
                    test -n "$GIT_COMMIT"
                    docker build --tag "$IMAGE_REPOSITORY:$GIT_COMMIT" .
                    # PR 示例只构建候选镜像；推送动作应由受信任的后续 Job 完成。
                '''
                stash name: 'source-and-manifests', includes: 'charts/**,dist/**', allowEmpty: true
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Create preview environment') {
            when { changeRequest() }
            agent { label 'trusted-preview-deployer' }
            steps {
                deleteDir()
                unstash 'source-and-manifests'
                script {
                    def id = env.CHANGE_ID
                    if (!(id ==~ /[0-9]+/)) {
                        error 'Invalid CHANGE_ID'
                    }
                    env.PREVIEW_NAME = "pr-${id}"
                    env.PREVIEW_URL = "https://${env.PREVIEW_NAME}.${env.PREVIEW_BASE_DOMAIN}"
                }
                lock(resource: 'preview-cluster') {
                    withCredentials([file(
                        credentialsId: 'kubeconfig-preview',
                        variable: 'KUBECONFIG'
                    )]) {
                        sh '''
                            set -Eeuo pipefail
                            kubectl create namespace "$PREVIEW_NAME" \\
                              --dry-run=client -o yaml | kubectl apply -f -
                            helm upgrade --install catalog ./charts/catalog \\
                              --namespace "$PREVIEW_NAME" \\
                              --set image.tag="$GIT_COMMIT" \\
                              --set ingress.host="$PREVIEW_NAME.$PREVIEW_BASE_DOMAIN" \\
                              --atomic --timeout 10m
                            kubectl -n "$PREVIEW_NAME" rollout status \\
                              deployment/catalog --timeout=5m
                        '''
                    }
                }
                echo "Preview URL: ${env.PREVIEW_URL}"
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Production approval') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            agent none
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input(
                        message: "Deploy ${env.GIT_COMMIT?.take(12)} to production?",
                        ok: 'Approve production',
                        submitter: 'release-team'
                    )
                }
            }
        }

        stage('Deploy production') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            agent { label 'trusted-production-deployer' }
            steps {
                deleteDir()
                unstash 'source-and-manifests'
                lock(resource: 'production-cluster') {
                    withCredentials([file(
                        credentialsId: 'kubeconfig-production',
                        variable: 'KUBECONFIG'
                    )]) {
                        sh '''
                            set -Eeuo pipefail
                            helm upgrade --install catalog ./charts/catalog \\
                              --namespace production --create-namespace \\
                              --set image.tag="$GIT_COMMIT" \\
                              --atomic --timeout 10m
                        '''
                    }
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }
    }

    post {
        cleanup {
            // 只清理 PR 创建的临时环境；主分支不会执行该分支。
            script {
                if (env.CHANGE_ID ==~ /[0-9]+/ && env.PREVIEW_NAME) {
                    node('trusted-preview-deployer') {
                        withCredentials([file(
                            credentialsId: 'kubeconfig-preview',
                            variable: 'KUBECONFIG'
                        )]) {
                            try {
                                sh 'kubectl delete namespace "$PREVIEW_NAME" --ignore-not-found=true'
                            } finally {
                                deleteDir()
                            }
                        }
                    }
                }
            }
        }
    }
}
