// Jenkinsfile 示例：使用版本化 Shared Library
// 共享库 company-ci@v3.4.0 应由平台团队维护、测试并发布不可变版本。

@Library('company-ci@v3.4.0') _

pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '15'))
    }

    environment {
        SERVICE_NAME = 'catalog'
        IMAGE_REPOSITORY = 'registry.example.com/team/catalog'
    }

    stages {
        stage('CI') {
            agent { label 'linux-standard' }
            steps {
                script {
                    // Shared Library 对参数、命令、凭据和报告路径进行统一校验。
                    runStandardCi([
                        service: env.SERVICE_NAME,
                        checkout: true,
                        testCommand: './scripts/test.sh',
                        lintCommand: './scripts/lint.sh',
                        junitPattern: 'reports/junit.xml',
                        artifactPattern: 'dist/**'
                    ])
                }
            }
            post {
                always {
                    publishStandardReports(junitPattern: 'reports/junit.xml')
                }
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Build signed candidate') {
            agent { label 'linux-docker-trusted' }
            steps {
                script {
                    checkout scm
                    def result = buildAndScanImage([
                        repository: env.IMAGE_REPOSITORY,
                        credentialsId: 'container-registry-push',
                        dockerfile: 'Dockerfile',
                        scanSeverity: 'HIGH,CRITICAL'
                    ])
                    if (!result?.digest || !(result.digest ==~ /sha256:[0-9a-f]{64}/)) {
                        error 'Shared Library returned an invalid image digest'
                    }
                    env.IMAGE_DIGEST = result.digest
                    writeFile(
                        file: 'release-metadata.properties',
                        text: "service=${env.SERVICE_NAME}\\nimage=${env.IMAGE_REPOSITORY}\\ndigest=${env.IMAGE_DIGEST}\\n"
                    )
                    archiveArtifacts artifacts: 'release-metadata.properties', fingerprint: true
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Deploy staging') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            agent { label 'trusted-staging-deployer' }
            steps {
                script {
                    deployWithPolicy([
                        environment: 'staging',
                        imageDigest: env.IMAGE_DIGEST,
                        kubeconfigCredentialId: 'kubeconfig-staging',
                        lockResource: 'kubernetes-staging'
                    ])
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
        always {
            echo "Shared Library pipeline result=${currentBuild.currentResult}"
        }
    }
}
