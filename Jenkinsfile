pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target deployment environment')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
    }

    environment {
        REGISTRY       = 'docker.io'
        IMAGE_PREFIX   = 'sjlassi/garage-microservices'
        REGISTRY_CREDS = 'docker-credentials'
        KUBECONFIG     = credentials('kubeconfig')
        SONAR_TOKEN    = credentials('sonar-token')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests -Dorg.slf4j.simpleLogger.defaultLogLevel=warn'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            }
        }

        stage('Code Quality') {
            when {
                anyOf { branch 'main'; branch 'develop' }
            }
            steps {
                sh """
                    mvn sonar:sonar \\
                        -Dsonar.projectKey=garage-microservices \\
                        -Dsonar.host.url=http://sonarqube:9000 \\
                        -Dsonar.login=\${SONAR_TOKEN}
                """
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.REGISTRY_CREDS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh "echo \$DOCKER_PASS | docker login ${env.REGISTRY} -u \$DOCKER_USER --password-stdin"
                    sh """
                        docker build -t ${env.REGISTRY}/${env.IMAGE_PREFIX}-auth:${params.IMAGE_TAG} \\
                            -f auth-service/Dockerfile auth-service/
                        docker build -t ${env.REGISTRY}/${env.IMAGE_PREFIX}-vehicle:${params.IMAGE_TAG} \\
                            -f vehicle-service/Dockerfile vehicle-service/
                        docker build -t ${env.REGISTRY}/${env.IMAGE_PREFIX}-stock:${params.IMAGE_TAG} \\
                            -f stock-service/Dockerfile stock-service/

                        docker push ${env.REGISTRY}/${env.IMAGE_PREFIX}-auth:${params.IMAGE_TAG}
                        docker push ${env.REGISTRY}/${env.IMAGE_PREFIX}-vehicle:${params.IMAGE_TAG}
                        docker push ${env.REGISTRY}/${env.IMAGE_PREFIX}-stock:${params.IMAGE_TAG}

                        docker logout ${env.REGISTRY}
                    """
                }
            }
        }

        stage('Security Scan') {
            steps {
                sh """
                    trivy image --exit-code 0 --severity HIGH,CRITICAL \\
                        ${env.REGISTRY}/${env.IMAGE_PREFIX}-auth:${params.IMAGE_TAG} || true
                    trivy image --exit-code 0 --severity HIGH,CRITICAL \\
                        ${env.REGISTRY}/${env.IMAGE_PREFIX}-vehicle:${params.IMAGE_TAG} || true
                    trivy image --exit-code 0 --severity HIGH,CRITICAL \\
                        ${env.REGISTRY}/${env.IMAGE_PREFIX}-stock:${params.IMAGE_TAG} || true
                """
            }
        }

        stage('Deploy to Dev') {
            when {
                expression { params.ENVIRONMENT == 'dev' }
            }
            steps {
                sh """
                    kubectl apply -f k8s/namespaces.yaml
                    kubectl apply -k k8s/dev/

                    kubectl set image deployment/auth-service \\
                        auth-service=${env.REGISTRY}/${env.IMAGE_PREFIX}-auth:${params.IMAGE_TAG} \\
                        -n garage-dev
                    kubectl set image deployment/vehicle-service \\
                        vehicle-service=${env.REGISTRY}/${env.IMAGE_PREFIX}-vehicle:${params.IMAGE_TAG} \\
                        -n garage-dev
                    kubectl set image deployment/stock-service \\
                        stock-service=${env.REGISTRY}/${env.IMAGE_PREFIX}-stock:${params.IMAGE_TAG} \\
                        -n garage-dev
                """
            }
        }

        stage('Approve Production Deploy') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    input message: "Deploy [${params.IMAGE_TAG}] to PRODUCTION?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy to Prod') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
            }
            steps {
                sh """
                    kubectl apply -f k8s/namespaces.yaml
                    kubectl apply -k k8s/prod/

                    kubectl set image deployment/auth-service \\
                        auth-service=${env.REGISTRY}/${env.IMAGE_PREFIX}-auth:${params.IMAGE_TAG} \\
                        -n garage-prod
                    kubectl set image deployment/vehicle-service \\
                        vehicle-service=${env.REGISTRY}/${env.IMAGE_PREFIX}-vehicle:${params.IMAGE_TAG} \\
                        -n garage-prod
                    kubectl set image deployment/stock-service \\
                        stock-service=${env.REGISTRY}/${env.IMAGE_PREFIX}-stock:${params.IMAGE_TAG} \\
                        -n garage-prod
                """
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def ns = "garage-${params.ENVIRONMENT}"
                    sh """
                        kubectl rollout status deployment/auth-service    -n ${ns} --timeout=5m
                        kubectl rollout status deployment/vehicle-service -n ${ns} --timeout=5m
                        kubectl rollout status deployment/stock-service   -n ${ns} --timeout=5m
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f || true'
        }
        success {
            echo "Pipeline succeeded — ${params.ENVIRONMENT} is live at tag ${params.IMAGE_TAG}"
        }
        failure {
            echo "Pipeline failed for ${params.ENVIRONMENT} — check the logs above"
        }
    }
}
