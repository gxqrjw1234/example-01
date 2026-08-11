// Jenkinsfile 示例：并行检查 + Declarative Matrix
// Matrix 的每个轴组合需要对应可用的 Agent/工具；下面的标签和命令按实际环境替换。

pipeline {
    agent none

    options {
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '10'))
        parallelsAlwaysFailFast()
    }

    stages {
        stage('Checkout') {
            agent { label 'linux' }
            steps {
                checkout scm
                stash name: 'source', includes: '**/*', useDefaultExcludes: false
            }
            post {
                cleanup {
                    deleteDir()
                }
            }
        }

        stage('Fast checks in parallel') {
            parallel {
                stage('Lint') {
                    agent { label 'linux-python' }
                    steps {
                        deleteDir()
                        unstash 'source'
                        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                            sh 'python -m ruff check .'
                        }
                    }
                }
                stage('Unit tests') {
                    agent { label 'linux-python' }
                    steps {
                        deleteDir()
                        unstash 'source'
                        sh 'python -m pytest tests/unit --junitxml=reports/unit.xml'
                    }
                    post {
                        always {
                            junit testResults: 'reports/unit.xml', allowEmptyResults: false
                        }
                        cleanup {
                            deleteDir()
                        }
                    }
                }
                stage('Dependency audit') {
                    agent { label 'linux-security' }
                    steps {
                        deleteDir()
                        unstash 'source'
                        sh 'pip-audit --strict'
                    }
                }
            }
        }

        stage('Runtime matrix') {
            matrix {
                axes {
                    axis {
                        name 'PYTHON_VERSION'
                        values '3.11', '3.12'
                    }
                    axis {
                        name 'DATABASE'
                        values 'sqlite', 'postgres'
                    }
                }
                excludes {
                    // 学习示例：仅保留受支持的组合，实际排除规则按产品兼容矩阵调整。
                    exclude {
                        axis {
                            name 'PYTHON_VERSION'
                            values '3.11'
                        }
                        axis {
                            name 'DATABASE'
                            values 'postgres'
                        }
                    }
                }
                stages {
                    stage('Integration test') {
                        agent { label 'linux-matrix' }
                        steps {
                            deleteDir()
                            unstash 'source'
                            withEnv([
                                "PYTHON_VERSION=${PYTHON_VERSION}",
                                "DATABASE=${DATABASE}"
                            ]) {
                                sh '''
                                    set -Eeuo pipefail
                                    echo "runtime=$PYTHON_VERSION database=$DATABASE"
                                    ./scripts/test-integration.sh "$PYTHON_VERSION" "$DATABASE"
                                '''
                            }
                        }
                        post {
                            always {
                                junit testResults: 'reports/**/*.xml', allowEmptyResults: true
                            }
                            cleanup {
                                deleteDir()
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "matrix pipeline result=${currentBuild.currentResult}"
        }
    }
}
