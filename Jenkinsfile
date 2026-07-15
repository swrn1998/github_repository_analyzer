pipeline {
    agent any

    environment {
        JAVA_HOME        = '/usr/lib/jvm/java-17-openjdk'
        GITHUB_API_TOKEN = credentials('github-api-token')   // Jenkins Credential ID
        DOCKER_REGISTRY  = 'your-registry.com'
        IMAGE_TAG        = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'echo "Building commit: $(git log --oneline -1)"'
            }
        }

        stage('Backend: Build') {
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests -B -q'
                }
            }
        }

        stage('Backend: Test') {
            steps {
                dir('backend') {
                    sh 'mvn test -B'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                    jacoco execPattern: 'backend/target/jacoco.exec',
                           classPattern: 'backend/target/classes',
                           sourcePattern: 'backend/src/main/java'
                }
            }
        }

        stage('Frontend: Build & Test') {
            parallel {
                stage('Install & Build') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'npm run build'
                        }
                    }
                }
                stage('Lint') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'npm run lint'
                        }
                    }
                }
            }
        }

        stage('Frontend: Test') {
            steps {
                dir('frontend') {
                    sh 'npm test -- --watchAll=false --ci'
                }
            }
        }

        stage('Docker: Build') {
            steps {
                sh """
                    docker build -t ${DOCKER_REGISTRY}/github-analyzer-backend:${IMAGE_TAG} ./backend
                    docker build -t ${DOCKER_REGISTRY}/github-analyzer-frontend:${IMAGE_TAG} ./frontend
                """
            }
        }

        stage('Docker: Push') {
            when { branch 'main' }
            steps {
                withDockerRegistry([credentialsId: 'docker-creds', url: "https://${DOCKER_REGISTRY}"]) {
                    sh """
                        docker push ${DOCKER_REGISTRY}/github-analyzer-backend:${IMAGE_TAG}
                        docker push ${DOCKER_REGISTRY}/github-analyzer-frontend:${IMAGE_TAG}
                        docker tag ${DOCKER_REGISTRY}/github-analyzer-backend:${IMAGE_TAG} ${DOCKER_REGISTRY}/github-analyzer-backend:latest
                        docker push ${DOCKER_REGISTRY}/github-analyzer-backend:latest
                    """
                }
            }
        }

        stage('Deploy: Staging') {
            when { branch 'main' }
            steps {
                sh """
                    docker-compose -f docker-compose.yml pull
                    docker-compose -f docker-compose.yml up -d --remove-orphans
                """
            }
            post {
                success {
                    echo "Deployed successfully: http://localhost:3000"
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        failure {
            echo "Build FAILED: ${env.BUILD_URL}"
        }
    }
}
