// Jenkinsfile 示例：跨 Job 编排、结果传播和 Copy Artifact
// 适用：需要把 Build/Test/Deploy 生命周期拆成独立 Job 的组织。

pipeline {
    agent none

    parameters {
        string(name: 'SOURCE_SHA', defaultValue: '', description: '要构建的完整 commit SHA')
        string(name: 'BUILD_NUMBER_TO_DEPLOY', defaultValue: '', description: '可选：指定上游 build 编号')
    }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '20'))
    }

    stages {
        stage('Validate request') {
            agent { label 'linux' }
            steps {
                script {
                    if (params.SOURCE_SHA &&
                        !(params.SOURCE_SHA ==~ /[0-9a-fA-F]{40}/)) {
                        error 'SOURCE_SHA must be a full 40-character SHA'
                    }
                    if (params.BUILD_NUMBER_TO_DEPLOY &&
                        !(params.BUILD_NUMBER_TO_DEPLOY ==~ /[0-9]+/)) {
                        error 'BUILD_NUMBER_TO_DEPLOY must be numeric'
                    }
                    if (params.BUILD_NUMBER_TO_DEPLOY) {
                        // 指定已有构建时跳过重新构建，并让后续验证使用同一个版本。
                        env.BUILD_JOB_NUMBER = params.BUILD_NUMBER_TO_DEPLOY
                    }
                }
            }
        }

        stage('Build upstream job') {
            when {
                expression { return !params.BUILD_NUMBER_TO_DEPLOY }
            }
            agent { label 'linux-orchestrator' }
            steps {
                script {
                    def parametersForBuild = []
                    if (params.SOURCE_SHA) {
                        parametersForBuild << string(name: 'SOURCE_SHA', value: params.SOURCE_SHA)
                    }
                    def child = build(
                        job: 'service-build',
                        parameters: parametersForBuild,
                        wait: true,
                        propagate: false
                    )
                    env.BUILD_JOB_NUMBER = child.number.toString()
                    if (child.result != 'SUCCESS') {
                        error "service-build #${child.number} result=${child.result}"
                    }
                }
            }
        }

        stage('Copy immutable build output') {
            agent { label 'linux-orchestrator' }
            steps {
                script {
                    def sourceBuild = params.BUILD_NUMBER_TO_DEPLOY ?: env.BUILD_JOB_NUMBER
                    copyArtifacts(
                        projectName: 'service-build',
                        selector: specific(sourceBuild),
                        filter: 'dist/**,release-metadata.properties,image-digest.txt',
                        fingerprintArtifacts: true
                    )
                    sh 'test -s release-metadata.properties'
                    stash name: 'release-output', includes: 'dist/**,release-metadata.properties,image-digest.txt'
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Independent verification jobs') {
            agent { label 'linux-orchestrator' }
            steps {
                script {
                    def branches = [:]
                    ['unit-test', 'integration-test'].each { jobName ->
                        def currentJob = jobName
                        branches[currentJob] = {
                            def child = build(
                                job: "service-${currentJob}",
                                parameters: [
                                    string(name: 'UPSTREAM_BUILD', value: env.BUILD_JOB_NUMBER)
                                ],
                                wait: true,
                                propagate: false
                            )
                            if (child.result != 'SUCCESS') {
                                error "${currentJob} #${child.number} result=${child.result}"
                            }
                        }
                    }
                    parallel branches
                }
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Deploy') {
            agent { label 'trusted-deployer' }
            steps {
                unstash 'release-output'
                sh '''
                    set -Eeuo pipefail
                    cat release-metadata.properties
                    ./scripts/deploy-from-artifact.sh release-metadata.properties dist
                '''
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
            script {
                // 顶层 agent 为 none，摘要和归档需要显式工作节点。
                node('linux-orchestrator') {
                    writeFile(
                        file: 'orchestration-summary.txt',
                        text: """Job: ${env.JOB_NAME}
Build: ${env.BUILD_NUMBER}
Upstream build: ${env.BUILD_JOB_NUMBER ?: 'provided'}
Result: ${currentBuild.currentResult}
"""
                    )
                    archiveArtifacts artifacts: 'orchestration-summary.txt', fingerprint: true
                    deleteDir()
                }
            }
        }
    }
}
