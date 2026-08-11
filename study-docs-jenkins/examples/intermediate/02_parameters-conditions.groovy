// Jenkinsfile 示例：参数、条件、审批和环境保护

pipeline {
    agent none

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'TARGET_ENV',
            choices: ['dev', 'staging', 'prod'],
            description: '部署目标，只允许白名单环境'
        )
        booleanParam(
            name: 'RUN_E2E',
            defaultValue: true,
            description: '是否执行端到端测试'
        )
        string(
            name: 'IMAGE_TAG',
            defaultValue: '',
            description: '可选的不可变镜像标签，例如 sha-abc123'
        )
    }

    stages {
        stage('Validate parameters') {
            agent { label 'linux' }
            steps {
                script {
                    if (!(params.TARGET_ENV in ['dev', 'staging', 'prod'])) {
                        error 'TARGET_ENV is not supported'
                    }
                    if (params.IMAGE_TAG &&
                        !(params.IMAGE_TAG ==~ /[A-Za-z0-9][A-Za-z0-9_.-]{0,127}/)) {
                        error 'IMAGE_TAG contains unsupported characters'
                    }
                    echo "target=${params.TARGET_ENV}, runE2E=${params.RUN_E2E}"
                }
            }
        }

        stage('Unit tests') {
            agent { label 'linux' }
            steps {
                checkout scm
                stash name: 'source', includes: '**/*', useDefaultExcludes: false
                sh './scripts/test-unit.sh'
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('E2E tests') {
            when {
                expression { return params.RUN_E2E }
            }
            agent { label 'linux-e2e' }
            steps {
                deleteDir()
                unstash 'source'
                sh './scripts/test-e2e.sh'
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Deploy to dev or staging') {
            when {
                anyOf {
                    expression { return params.TARGET_ENV == 'dev' }
                    expression { return params.TARGET_ENV == 'staging' }
                }
            }
            agent { label 'trusted-deployer' }
            steps {
                unstash 'source'
                lock(resource: "deploy-${params.TARGET_ENV}") {
                    withEnv(["DEPLOY_ENV=${params.TARGET_ENV}"]) {
                        sh './scripts/deploy.sh "$DEPLOY_ENV" "$IMAGE_TAG"'
                    }
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Approve production') {
            when {
                expression { return params.TARGET_ENV == 'prod' }
            }
            agent none
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input(
                        message: "确认发布 ${params.IMAGE_TAG ?: '当前构建'} 到生产环境？",
                        ok: '批准发布',
                        submitter: 'release-team'
                    )
                }
            }
        }

        stage('Deploy to production') {
            when {
                expression { return params.TARGET_ENV == 'prod' }
            }
            agent { label 'trusted-production-deployer' }
            steps {
                unstash 'source'
                lock(resource: 'production-environment') {
                    withEnv(["DEPLOY_ENV=${params.TARGET_ENV}"]) {
                        sh './scripts/deploy.sh "$DEPLOY_ENV" "$IMAGE_TAG"'
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
        always {
            echo "result=${currentBuild.currentResult}, target=${params.TARGET_ENV}"
        }
    }
}
