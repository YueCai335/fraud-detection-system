// Same three-stage pipeline as .github/workflows/ci.yml, written for Jenkins.
// Runs on the local Jenkins from ci/jenkins/ (see docs/ci-jenkins.md).
pipeline {
    agent none   // no default machine: every stage below says where it runs

    options {
        timestamps()
    }

    stages {

        stage('model-service (pytest)') {
            // Jenkins starts this container, mounts the checked-out repo into it,
            // runs the steps inside, then throws the container away.
            agent {
                docker { image 'python:3.10-slim' }
            }
            steps {
                dir('model-service') {
                    sh '''
                        python -m venv .venv
                        .venv/bin/pip install -q -r requirements-dev.txt
                        .venv/bin/pytest -q
                    '''
                }
            }
        }

        stage('fraud-service (JUnit)') {
            agent {
                docker { image 'maven:3.9-eclipse-temurin-17' }
            }
            steps {
                dir('fraud-service') {
                    sh './mvnw -B --no-transfer-progress verify'
                }
            }
            post {
                always {
                    junit 'fraud-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('docker compose smoke test') {
            // Not a throwaway container this time: this stage runs on the Jenkins
            // controller itself, because it needs the docker CLI + compose plugin
            // (baked into ci/jenkins/Dockerfile) talking to the host's Docker daemon.
            agent any
            environment {
                // Own project name, so the CI stack never collides with the
                // `docker compose up` stack a developer has running on the same machine.
                COMPOSE_PROJECT_NAME = 'fraud-ci'
                // Every `docker compose` below reads both files (compose honours COMPOSE_FILE),
                // so the override cannot be forgotten on one of them.
                COMPOSE_FILE = 'docker-compose.yml:ci/compose.ci.yml'
                // Inside this container localhost:8080 is Jenkins' own UI, not the app.
                // The stack's published ports are on the Docker Desktop host; this alias reaches it.
                CI_HOST = 'host.docker.internal'
                // ci/compose.ci.yml publishes the app here, clear of a developer's own stack on 8080.
                CI_PORT = '18080'
            }
            steps {
                sh 'docker compose up --build -d --wait --wait-timeout 300'
                // The login the smoke test uses lives in the Jenkins credential store,
                // not in this file. Single-quoted sh: the shell expands $SMOKE_USER /
                // $SMOKE_PASS, Groovy never sees the values, and Jenkins masks them in the log.
                withCredentials([usernamePassword(credentialsId: 'smoke-login',
                                                  usernameVariable: 'SMOKE_USER',
                                                  passwordVariable: 'SMOKE_PASS')]) {
                    sh 'scripts/smoke.sh http://$CI_HOST:$CI_PORT "$SMOKE_USER" "$SMOKE_PASS"'
                }
            }
            post {
                failure { sh 'docker compose logs' }
                always  { sh 'docker compose down -v' }
            }
        }

    }
}
