// Jenkinsfile 示例：GitHub Branch Source/Multibranch PR 检查
// 推荐由 GitHub Branch Source 管理 Webhook、checkout 和 Checks/Status；
// 不要在同一个 Job 中重复配置多个 GitHub Webhook 触发插件。

pipeline {
    agent { label 'linux-pr-untrusted' }

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('PR metadata') {
            when { changeRequest() }
            steps {
                echo "PR=${env.CHANGE_ID} source=${env.CHANGE_BRANCH} target=${env.CHANGE_TARGET}"
            }
        }

        stage('Lint and test') {
            steps {
                sh '''
                    set -Eeuo pipefail
                    ./scripts/lint.sh
                    ./scripts/test.sh
                '''
            }
        }

        stage('Build preview artifact') {
            when {
                anyOf {
                    changeRequest(target: 'main')
                    branch 'main'
                }
            }
            steps {
                sh './scripts/build.sh'
                stash name: 'preview', includes: 'dist/**', useDefaultExcludes: false
            }
        }
    }

    post {
        always {
            junit testResults: 'reports/junit.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'reports/**', allowEmptyArchive: true
        }
        failure {
            echo "PR/branch check failed: ${env.BUILD_URL}"
        }
        cleanup {
            deleteDir()
        }
    }
}
