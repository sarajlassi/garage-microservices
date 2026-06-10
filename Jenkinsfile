pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Environment to deploy to')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
    }

    environment {
        REGISTRY = 'docker.io'
        REGISTRY_CREDENTIALS = 'docker-credentials'
        PROJECT_NAME = 'garage-microservices'
        KUBECONFIG = '/var/jenkins_home/.kube/config'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "🔄 Checking out source code..."
                    checkout scm
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    echo "🔨 Building microservices with Maven..."
                    sh '''
                        mvn clean package -DskipTests \
                            -Dorg.slf4j.simpleLogger.defaultLogLevel=info
                    '''
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    echo "🧪 Running unit tests..."
                    sh '''
                        mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=info
                    '''
                }
            }
        }

        stage('Code Quality Analysis') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "📊 Running SonarQube analysis..."
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=garage-microservices \
                            -Dsonar.sources=. \
                            -Dsonar.host.url=http://sonarqube:9000 \
                            -Dsonar.login=${SONAR_TOKEN} || true
                    '''
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    echo "🐳 Building Docker images..."
                    sh '''
                        # Build auth-service
                        docker build -t ${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG} \
                            -f auth-service/Dockerfile auth-service/

                        # Build vehicle-service
                        docker build -t ${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG} \
                            -f vehicle-service/Dockerfile vehicle-service/

                        # Build stock-service
                        docker build -t ${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG} \
                            -f stock-service/Dockerfile stock-service/
                    '''
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    echo "📤 Pushing Docker images to registry..."
                    withCredentials([usernamePassword(credentialsId: "${REGISTRY_CREDENTIALS}",
                                    usernameVariable: 'DOCKER_USER',
                                    passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin

                            docker push ${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG}
                            docker push ${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG}
                            docker push ${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG}

                            # Tag as latest if not already
                            if [ "${IMAGE_TAG}" != "latest" ]; then
                                docker tag ${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG} \
                                          ${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:latest
                                docker tag ${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG} \
                                          ${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:latest
                                docker tag ${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG} \
                                          ${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:latest

                                docker push ${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:latest
                                docker push ${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:latest
                                docker push ${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:latest
                            fi

                            docker logout
                        '''
                    }
                }
            }
        }

        stage('Security Scan') {
            steps {
                script {
                    echo "🔒 Running security scan..."
                    sh '''
                        # Scan images for vulnerabilities using trivy
                        trivy image --severity HIGH,CRITICAL \
                            ${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG} || true
                        trivy image --severity HIGH,CRITICAL \
                            ${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG} || true
                        trivy image --severity HIGH,CRITICAL \
                            ${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG} || true
                    '''
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                expression { params.ENVIRONMENT == 'dev' }
            }
            steps {
                script {
                    echo "🚀 Deploying to DEV environment..."
                    sh '''
                        kubectl set image deployment/auth-service \
                            auth-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG} \
                            -n garage-dev || \
                        kubectl apply -f k8s/dev/ -n garage-dev

                        kubectl set image deployment/vehicle-service \
                            vehicle-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG} \
                            -n garage-dev || true

                        kubectl set image deployment/stock-service \
                            stock-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG} \
                            -n garage-dev || true
                    '''
                }
            }
        }

        stage('Deploy to Staging') {
            when {
                expression { params.ENVIRONMENT == 'staging' }
                branch 'main'
            }
            steps {
                script {
                    echo "🚀 Deploying to STAGING environment..."
                    sh '''
                        kubectl apply -f k8s/staging/ -n garage-staging
                        kubectl set image deployment/auth-service \
                            auth-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG} \
                            -n garage-staging || true
                        kubectl set image deployment/vehicle-service \
                            vehicle-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG} \
                            -n garage-staging || true
                        kubectl set image deployment/stock-service \
                            stock-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG} \
                            -n garage-staging || true
                    '''
                }
            }
        }

        stage('Deploy to Production') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
                tag pattern: "v\\d+.\\d+.\\d+", comparator: "REGEXP"
            }
            input {
                message "Deploy to production?"
                ok "Deploy"
            }
            steps {
                script {
                    echo "🚀 Deploying to PRODUCTION environment..."
                    sh '''
                        kubectl apply -f k8s/prod/ -n garage-prod
                        kubectl set image deployment/auth-service \
                            auth-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-auth:${IMAGE_TAG} \
                            -n garage-prod || true
                        kubectl set image deployment/vehicle-service \
                            vehicle-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-vehicle:${IMAGE_TAG} \
                            -n garage-prod || true
                        kubectl set image deployment/stock-service \
                            stock-service=${REGISTRY}/sjlassi/${PROJECT_NAME}-stock:${IMAGE_TAG} \
                            -n garage-prod || true
                    '''
                }
            }
        }

        stage('Health Check') {
            when {
                expression { params.ENVIRONMENT in ['dev', 'staging', 'prod'] }
            }
            steps {
                script {
                    def namespace = "garage-${params.ENVIRONMENT}"
                    echo "✅ Checking health of deployments in ${namespace}..."
                    sh '''
                        kubectl rollout status deployment/auth-service -n ${namespace} --timeout=5m
                        kubectl rollout status deployment/vehicle-service -n ${namespace} --timeout=5m
                        kubectl rollout status deployment/stock-service -n ${namespace} --timeout=5m
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 Cleaning up..."
                sh '''
                    docker system prune -f || true
                '''
            }
        }
        success {
            echo "✅ Pipeline completed successfully!"
        }
        failure {
            echo "❌ Pipeline failed!"
        }
    }
}

