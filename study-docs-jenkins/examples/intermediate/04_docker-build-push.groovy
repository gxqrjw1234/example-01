// Jenkinsfile 示例：构建并推送 Docker 镜像
// 生产中将 registry.example.com/team/app、凭据 ID 和 Agent 标签替换为受控配置。

pipeline {
    agent { label 'linux-docker-trusted' }

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '15'))
    }

    environment {
        REGISTRY = 'registry.example.com'
        IMAGE_REPOSITORY = 'registry.example.com/team/app'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build, scan and push') {
            steps {
                script {
                    def commit = sh(
                        script: 'git rev-parse --verify HEAD',
                        returnStdout: true
                    ).trim()
                    if (!(commit ==~ /[0-9a-f]{40}/)) {
                        error 'Unexpected Git commit SHA'
                    }
                    env.IMAGE_REF = "${env.IMAGE_REPOSITORY}:${commit}"

                    withCredentials([usernamePassword(
                        credentialsId: 'container-registry-push',
                        usernameVariable: 'REGISTRY_USER',
                        passwordVariable: 'REGISTRY_PASSWORD'
                    )]) {
                        try {
                            sh '''
                                set -Eeuo pipefail
                                set +x
                                printf '%s' "$REGISTRY_PASSWORD" | docker login "$REGISTRY" \\
                                  --username "$REGISTRY_USER" --password-stdin
                                set -x
                                docker build --pull --tag "$IMAGE_REF" .
                                docker image inspect "$IMAGE_REF" >/dev/null
                                trivy image --exit-code 1 --severity HIGH,CRITICAL "$IMAGE_REF"
                                docker push "$IMAGE_REF"
                                docker image inspect "$IMAGE_REF" \\
                                  --format '{{index .RepoDigests 0}}' > image-digest.txt
                                test -s image-digest.txt
                            '''
                        } finally {
                            sh 'docker logout "$REGISTRY" || true'
                        }
                    }
                }
            }
        }

        stage('Publish metadata') {
            steps {
                sh '''
                    set -Eeuo pipefail
                    printf 'image=%s\\n' "$IMAGE_REF" > image-metadata.properties
                    printf 'digest=%s\\n' "$(cat image-digest.txt)" >> image-metadata.properties
                '''
                archiveArtifacts(
                    artifacts: 'image-metadata.properties,image-digest.txt',
                    fingerprint: true,
                    allowEmptyArchive: false
                )
            }
        }
    }

    post {
        failure {
            archiveArtifacts artifacts: 'docker-build.log', allowEmptyArchive: true
        }
        cleanup {
            sh 'docker image rm "$IMAGE_REF" || true'
            deleteDir()
        }
    }
}
