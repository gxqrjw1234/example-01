// Jenkinsfile 示例：安全扫描、SBOM、签名和质量门禁
// 工具版本/凭据 ID/Registry 按组织基线替换；示例不包含任何真实 Secret。

pipeline {
    agent { label 'linux-supply-chain' }

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 75, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '40', artifactNumToKeepStr: '25'))
    }

    environment {
        IMAGE = 'registry.example.com/team/catalog'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout and provenance') {
            steps {
                checkout scm
                sh '''
                    set -Eeuo pipefail
                    mkdir -p reports
                    git rev-parse --verify HEAD > source-sha.txt
                    git diff --check
                '''
            }
        }

        stage('SAST and dependency scan') {
            parallel {
                stage('Static analysis') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh 'sonar-scanner -Dsonar.projectVersion="$BUILD_NUMBER"'
                        }
                    }
                }
                stage('Dependency scan') {
                    steps {
                        sh '''
                            set -Eeuo pipefail
                            dependency-check.sh \\
                              --project catalog \\
                              --scan . \\
                              --format SARIF \\
                              --out reports/dependency-check
                        '''
                    }
                }
                stage('Secret scan') {
                    steps {
                        sh 'gitleaks detect --redact --no-banner --report-path reports/gitleaks.json'
                    }
                }
            }
        }

        stage('Build candidate') {
            steps {
                script {
                    env.IMAGE_REF = "${env.IMAGE}:${env.GIT_COMMIT}"
                    withCredentials([usernamePassword(
                        credentialsId: 'container-registry-push',
                        usernameVariable: 'REGISTRY_USER',
                        passwordVariable: 'REGISTRY_PASSWORD'
                    )]) {
                        sh '''
                            set -Eeuo pipefail
                            set +x
                            printf '%s' "$REGISTRY_PASSWORD" | docker login registry.example.com \\
                              --username "$REGISTRY_USER" --password-stdin
                            set -x
                            docker build --pull --tag "$IMAGE_REF" .
                            syft "$IMAGE_REF" -o cyclonedx-json=reports/sbom.json
                            trivy image --format sarif \\
                              --output reports/trivy.sarif \\
                              --severity HIGH,CRITICAL --exit-code 1 "$IMAGE_REF"
                            docker push "$IMAGE_REF"
                            docker image inspect "$IMAGE_REF" \\
                              --format '{{index .RepoDigests 0}}' > image-digest.txt
                        '''
                    }
                }
            }
        }

        stage('Quality gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Sign image and attest SBOM') {
            steps {
                withCredentials([
                    file(credentialsId: 'cosign-key', variable: 'COSIGN_KEY'),
                    string(credentialsId: 'cosign-password', variable: 'COSIGN_PASSWORD')
                ]) {
                    sh '''
                        set -Eeuo pipefail
                        set +x
                        DIGEST="$(cat image-digest.txt)"
                        cosign sign --yes --key "$COSIGN_KEY" "$DIGEST"
                        cosign attest --yes --key "$COSIGN_KEY" \\
                          --type cyclonedx \\
                          --predicate reports/sbom.json "$DIGEST"
                    '''
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'reports/**/*.xml', allowEmptyResults: true
            archiveArtifacts(
                artifacts: 'source-sha.txt,image-digest.txt,reports/**',
                fingerprint: true,
                allowEmptyArchive: true
            )
        }
        failure {
            echo "Supply-chain gate failed: ${env.BUILD_URL}"
        }
        cleanup {
            sh 'docker logout registry.example.com || true'
            sh 'docker image rm "$IMAGE_REF" || true'
            deleteDir()
        }
    }
}
