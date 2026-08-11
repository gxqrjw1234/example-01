// Jenkinsfile 示例：Python CI
// 适用：Pipeline Job 或 Multibranch Pipeline
// 前置：linux-python Agent，项目提供 requirements.txt 和 tests/，并安装 pytest/pytest-cov。

pipeline {
    agent { label 'linux-python' }

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
    }

    environment {
        PYTHONUNBUFFERED = '1'
        PIP_DISABLE_PIP_VERSION_CHECK = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install dependencies') {
            steps {
                sh '''
                    set -Eeuo pipefail
                    python3 -m venv .venv
                    . .venv/bin/activate
                    python -m pip install --upgrade pip
                    python -m pip install -r requirements.txt
                    if [ -f requirements-dev.txt ]; then
                        python -m pip install -r requirements-dev.txt
                    fi
                '''
            }
        }

        stage('Lint and unit tests') {
            steps {
                sh '''
                    set -Eeuo pipefail
                    . .venv/bin/activate
                    mkdir -p reports
                    python -m compileall -q src
                    python -m pytest tests/ \\
                        --junitxml=reports/junit.xml \\
                        --cov=src --cov-report=xml:reports/coverage.xml
                '''
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -Eeuo pipefail
                    . .venv/bin/activate
                    python src/build.py
                    test -f dist/app.py
                    sha256sum dist/app.py > dist/SHA256SUMS
                '''
                stash name: 'python-dist', includes: 'dist/**', useDefaultExcludes: false
            }
        }
    }

    post {
        always {
            junit testResults: 'reports/junit.xml', allowEmptyResults: false
            archiveArtifacts(
                artifacts: 'reports/**,dist/**',
                fingerprint: true,
                allowEmptyArchive: true
            )
        }
        cleanup {
            deleteDir()
        }
    }
}
