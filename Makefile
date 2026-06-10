.PHONY: help build test docker-build docker-push deploy clean logs

# =====================================================
# Garage Microservices - Makefile
# =====================================================

# Default target
help:
	@echo "Garage Microservices - Available Commands"
	@echo "=========================================="
	@echo ""
	@echo "Build & Development:"
	@echo "  make build              - Build all microservices with Maven"
	@echo "  make clean              - Clean build artifacts"
	@echo "  make test               - Run all unit tests"
	@echo "  make coverage           - Generate code coverage report"
	@echo "  make sonar              - Run SonarQube analysis"
	@echo ""
	@echo "Docker Operations:"
	@echo "  make docker-build       - Build Docker images"
	@echo "  make docker-push        - Push images to registry"
	@echo "  make docker-run         - Run services with docker-compose"
	@echo "  make docker-stop        - Stop docker-compose services"
	@echo "  make docker-logs        - View docker-compose logs"
	@echo ""
	@echo "DevOps Infrastructure:"
	@echo "  make devops-up          - Start DevOps stack (Jenkins, SonarQube, etc.)"
	@echo "  make devops-down        - Stop DevOps stack"
	@echo "  make devops-logs        - View DevOps logs"
	@echo ""
	@echo "Kubernetes Deployment:"
	@echo "  make k8s-deploy-dev     - Deploy to DEV environment"
	@echo "  make k8s-deploy-staging - Deploy to STAGING environment"
	@echo "  make k8s-deploy-prod    - Deploy to PRODUCTION environment"
	@echo "  make k8s-status         - Check Kubernetes status"
	@echo "  make k8s-logs           - View Kubernetes logs"
	@echo "  make k8s-port-forward   - Setup port forwarding"
	@echo ""
	@echo "Jenkins Pipeline:"
	@echo "  make jenkins-job        - Create/update Jenkins job"
	@echo "  make jenkins-build      - Trigger Jenkins build"
	@echo "  make jenkins-logs       - View Jenkins logs"
	@echo ""
	@echo "Utilities:"
	@echo "  make setup              - Initial project setup"
	@echo "  make env-check          - Check environment setup"
	@echo "  make all                - Build, test and create Docker images"
	@echo ""

# =====================================================
# Build & Development
# =====================================================

build:
	@echo "Building all microservices..."
	mvn clean package -DskipTests

clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	rm -rf target/
	find . -type d -name target -exec rm -rf {} + 2>/dev/null || true

test:
	@echo "Running unit tests..."
	mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=info

coverage:
	@echo "Generating code coverage report..."
	mvn jacoco:report

sonar:
	@echo "Running SonarQube analysis..."
	mvn sonar:sonar \
		-Dsonar.projectKey=garage-microservices \
		-Dsonar.sources=. \
		-Dsonar.host.url=http://localhost:9000 \
		-Dsonar.login=$${SONAR_TOKEN}

# =====================================================
# Docker Operations
# =====================================================

docker-build:
	@echo "Building Docker images..."
	docker-compose build

docker-push:
	@echo "Pushing Docker images to registry..."
	docker-compose push

docker-run:
	@echo "Starting services with docker-compose..."
	docker-compose up -d

docker-stop:
	@echo "Stopping docker-compose services..."
	docker-compose down

docker-logs:
	@echo "Viewing docker-compose logs..."
	docker-compose logs -f

docker-clean:
	@echo "Cleaning Docker images and containers..."
	docker system prune -a --volumes

# =====================================================
# DevOps Infrastructure
# =====================================================

devops-up:
	@echo "Starting DevOps infrastructure stack..."
	docker-compose -f devops/docker-compose-devops.yml up -d
	@echo ""
	@echo "Services available at:"
	@echo "  Jenkins:     http://localhost:8080"
	@echo "  SonarQube:   http://localhost:9000"
	@echo "  Nexus:       http://localhost:8081"
	@echo "  Portainer:   http://localhost:9001"
	@echo "  Prometheus: http://localhost:9090"
	@echo "  Grafana:     http://localhost:3000"

devops-down:
	@echo "Stopping DevOps infrastructure..."
	docker-compose -f devops/docker-compose-devops.yml down

devops-logs:
	@echo "Viewing DevOps logs..."
	docker-compose -f devops/docker-compose-devops.yml logs -f

# =====================================================
# Kubernetes Deployment
# =====================================================

k8s-deploy-dev:
	@echo "Deploying to DEV environment..."
	bash scripts/deploy-k8s.sh dev latest

k8s-deploy-staging:
	@echo "Deploying to STAGING environment..."
	bash scripts/deploy-k8s.sh staging latest

k8s-deploy-prod:
	@echo "Deploying to PRODUCTION environment..."
	bash scripts/deploy-k8s.sh prod 1.0.0

k8s-status:
	@echo "Checking Kubernetes status..."
	@echo ""
	@echo "Namespaces:"
	kubectl get namespaces
	@echo ""
	@echo "Pods (DEV):"
	kubectl get pods -n garage-dev
	@echo ""
	@echo "Services (DEV):"
	kubectl get svc -n garage-dev

k8s-logs:
	@echo "Viewing Kubernetes logs..."
	@read -p "Enter namespace (default: garage-dev): " NS; \
	NS=$${NS:-garage-dev}; \
	kubectl logs -f deployment/auth-service -n $$NS

k8s-port-forward:
	@echo "Setting up port forwarding..."
	@echo "Auth Service: 8081:8081"
	@echo "Vehicle Service: 8082:8082"
	@echo "Stock Service: 8083:8083"
	@echo "Kafka UI: 8080:8080"
	kubectl port-forward -n garage-dev svc/auth-service 8081:8081 &
	kubectl port-forward -n garage-dev svc/vehicle-service 8082:8082 &
	kubectl port-forward -n garage-dev svc/stock-service 8083:8083 &
	kubectl port-forward -n garage-dev svc/kafka-ui 8080:8080 &

# =====================================================
# Jenkins Operations
# =====================================================

jenkins-job:
	@echo "Creating Jenkins job..."
	# Implement Jenkins job creation

jenkins-build:
	@echo "Triggering Jenkins build..."
	java -jar jenkins-cli.jar -s http://localhost:8080 \
		build garage-microservices-pipeline

jenkins-logs:
	@echo "Viewing Jenkins logs..."
	docker logs -f jenkins

# =====================================================
# Utilities
# =====================================================

setup: env-check
	@echo "Setting up project..."
	@echo "Creating .env from .env.example..."
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "✓ .env file created. Please update with your values."; \
	fi
	@echo ""
	@echo "Installing Maven dependencies..."
	mvn dependency:resolve -q
	@echo "✓ Dependencies installed"
	@echo ""
	@echo "Setup completed!"

env-check:
	@echo "Checking environment setup..."
	@echo ""
	@echo "Checking required tools:"
	@command -v java > /dev/null && echo "✓ Java" || echo "✗ Java not found"
	@command -v mvn > /dev/null && echo "✓ Maven" || echo "✗ Maven not found"
	@command -v docker > /dev/null && echo "✓ Docker" || echo "✗ Docker not found"
	@command -v docker-compose > /dev/null && echo "✓ Docker Compose" || echo "✗ Docker Compose not found"
	@command -v kubectl > /dev/null && echo "✓ kubectl" || echo "✗ kubectl not found"
	@echo ""
	@echo "Checking Java version:"
	@java -version 2>&1 | grep "17"
	@echo ""
	@echo "Checking Docker:"
	@docker version --format "Docker: {{.Server.Version}}"
	@echo ""
	@echo "Checking Kubernetes:"
	@kubectl version --short --client

all: clean test docker-build
	@echo "✓ Build pipeline completed successfully!"

# =====================================================
# Development Commands
# =====================================================

run-dev:
	@echo "Starting development environment..."
	mvn spring-boot:run

run-auth:
	@echo "Starting Auth Service..."
	mvn -pl auth-service spring-boot:run

run-vehicle:
	@echo "Starting Vehicle Service..."
	mvn -pl vehicle-service spring-boot:run

run-stock:
	@echo "Starting Stock Service..."
	mvn -pl stock-service spring-boot:run

# =====================================================
# API Testing
# =====================================================

test-api:
	@echo "Running API tests..."
	bash test-api.sh all

# =====================================================
# Documentation
# =====================================================

docs:
	@echo "Building documentation..."
	# Add documentation build commands

# =====================================================
# Cleanup
# =====================================================

cleanup: clean docker-clean
	@echo "Cleanup completed!"

