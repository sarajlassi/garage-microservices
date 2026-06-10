# CI/CD Infrastructure Guide - Garage Microservices

## Overview

Ce guide couvre la mise en place complète d'une infrastructure CI/CD pour les microservices Garage avec Jenkins, Docker et Kubernetes.

### Architecture

```
┌─────────────────────────────────────────┐
│       GitHub/GitLab Repository          │
└────────────────┬────────────────────────┘
                 │
                 ▼
        ┌───────────────────┐
        │     Jenkins       │
        │   (CI/CD Server)  │
        └────────┬──────────┘
                 │
        ┌────────┴────────────────────────┐
        ▼                                  ▼
   ┌─────────┐                      ┌──────────┐
   │ Build   │                      │  Tests   │
   │ & Test  │                      │  & QA    │
   └────┬────┘                      └────┬─────┘
        │                                │
        └───────────────┬────────────────┘
                        ▼
            ┌──────────────────────┐
            │  SonarQube Analysis  │
            │  Security Scanning   │
            └──────────┬───────────┘
                        ▼
            ┌──────────────────────┐
            │ Build Docker Images  │
            │ Push to Registry     │
            └──────────┬───────────┘
                        ▼
        ┌───────────────┴──────────────┐
        ▼                              ▼
   ┌─────────┐                   ┌──────────┐
   │   Dev   │                   │ Staging  │
   │Kubernetes                   │Kubernetes│
   │Cluster  │                   │Cluster   │
   └─────────┘                   └──────────┘
                                      │
                                      ▼
                                 ┌──────────┐
                                 │   Prod   │
                                 │Kubernetes│
                                 │ Cluster  │
                                 └──────────┘
```

---

## Prerequisites

- Docker Desktop ou Docker Engine + kubectl
- Kubernetes cluster (local ou cloud)
- Git
- 8GB RAM minimum
- 50GB disk space

---

## 1. Setup Development Infrastructure

### 1.1 Start DevOps Stack (Jenkins, SonarQube, Nexus, etc.)

```bash
cd devops
docker-compose -f docker-compose-devops.yml up -d
```

### 1.2 Access Services

| Service | URL | Default Credentials |
|---------|-----|-------------------|
| Jenkins | http://localhost:8080 | admin/admin |
| SonarQube | http://localhost:9000 | admin/admin |
| Nexus | http://localhost:8081 | admin/admin123 |
| GitLab | http://localhost:8000 | root/5iveL!fe |
| Portainer | http://localhost:9001 | admin/12345678 |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Registry | http://localhost:5000 | - |
| Registry UI | http://localhost:5001 | - |

### 1.3 Setup Jenkins

```bash
# Configure Jenkins with CasC
docker exec jenkins bash /scripts/setup-jenkins.sh

# Or manually configure:
# 1. Access http://localhost:8080
# 2. Complete Initial Setup Wizard
# 3. Install suggested plugins
# 4. Create credentials:
#    - Docker Registry credentials
#    - GitHub/GitLab credentials
#    - Kubernetes credentials
#    - SonarQube token
```

---

## 2. Setup Kubernetes Cluster

### 2.1 Create Namespaces

```bash
# Create all namespaces
kubectl apply -f k8s/namespaces.yaml

# Verify
kubectl get namespaces
```

### 2.2 Deploy to Dev Environment

```bash
# Using script
bash scripts/deploy-k8s.sh dev latest

# Or manually with kubectl
kubectl apply -f k8s/dev/ -n garage-dev

# Or with Kustomize
kustomize build k8s/dev | kubectl apply -f -
```

### 2.3 Deploy to Staging

```bash
bash scripts/deploy-k8s.sh staging latest
```

### 2.4 Deploy to Production

```bash
bash scripts/deploy-k8s.sh prod 1.0.0
```

### 2.5 Verify Deployments

```bash
# Check all pods
kubectl get pods -n garage-dev

# Check services
kubectl get svc -n garage-dev

# Check logs
kubectl logs deployment/auth-service -n garage-dev

# Port forward for testing
kubectl port-forward svc/auth-service 8081:8081 -n garage-dev &
kubectl port-forward svc/vehicle-service 8082:8082 -n garage-dev &
kubectl port-forward svc/stock-service 8083:8083 -n garage-dev &
```

---

## 3. Configure CI/CD Pipeline

### 3.1 Add Repository to Jenkins

1. Access Jenkins at http://localhost:8080
2. New Item → Pipeline
3. Name: `garage-microservices-pipeline`
4. Pipeline → Definition: `Pipeline script from SCM`
5. SCM: Git
6. Repository URL: `https://github.com/yourusername/garage-microservices.git`
7. Branch: `*/main`
8. Script Path: `Jenkinsfile`
9. Save

### 3.2 Configure Jenkins Credentials

```bash
# Create credentials in Jenkins UI

# Docker Registry
Type: Username with password
Username: your_docker_username
Password: your_docker_password
ID: docker-credentials

# GitHub
Type: Username with password
Username: your_github_username
Password: your_github_token
ID: github-credentials

# SonarQube
Type: Secret text
Secret: your_sonarqube_token
ID: sonar-token

# Kubernetes
Type: Kubernetes configuration
Kubeconfig: content of ~/.kube/config
ID: kubernetes-config
```

### 3.3 Update Environment Variables

Create `.env` file in root directory:

```bash
DOCKER_REGISTRY=docker.io
DOCKER_USERNAME=your_username
DOCKER_PASSWORD=your_password
SONAR_TOKEN=your_sonarqube_token
KUBE_USER=your_kube_user
KUBE_PASS=your_kube_password
SMTP_USER=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

---

## 4. Build and Push Docker Images

### 4.1 Build Locally

```bash
# Build all services
docker-compose build

# Build specific service
docker build -t garage-microservices-auth:latest ./auth-service
```

### 4.2 Push to Registry

```bash
# Login to Docker
docker login docker.io

# Tag images
docker tag garage-microservices-auth:latest sjlassi/garage-microservices-auth:latest

# Push
docker push sjlassi/garage-microservices-auth:latest
docker push sjlassi/garage-microservices-vehicle:latest
docker push sjlassi/garage-microservices-stock:latest
```

---

## 5. Run Jenkins Pipeline

### 5.1 Trigger Pipeline

```bash
# Via Jenkins UI
# Or via CLI
java -jar jenkins-cli.jar -s http://localhost:8080 \
    build garage-microservices-pipeline \
    -p ENVIRONMENT=dev \
    -p IMAGE_TAG=latest
```

### 5.2 Monitor Pipeline

- Access Jenkins at http://localhost:8080
- Click on job to see console output
- Check Blue Ocean for better visualization

---

## 6. Monitoring and Observability

### 6.1 Prometheus

```bash
# Access: http://localhost:9090
# Query example:
# - http_requests_total
# - jvm_memory_used_bytes
# - process_cpu_usage
```

### 6.2 Grafana

```bash
# Access: http://localhost:3000
# Add Prometheus as data source
# Import dashboards:
# - Search for "Spring Boot" (id: 4701)
# - Search for "Docker" (id: 8588)
# - Search for "Kubernetes" (id: 6417)
```

### 6.3 View Kubernetes Metrics

```bash
# Install metrics-server (if needed)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# View metrics
kubectl top nodes
kubectl top pods -n garage-dev
kubectl top pods -n garage-dev --containers
```

---

## 7. Troubleshooting

### 7.1 Common Issues

#### Jenkins not starting
```bash
docker logs jenkins
docker exec jenkins cat /var/jenkins_home/init.groovy.d/basic-security.groovy
```

#### Kubernetes pods not deploying
```bash
# Check events
kubectl describe pod <pod-name> -n garage-dev

# Check logs
kubectl logs <pod-name> -n garage-dev

# Check resources
kubectl get events -n garage-dev --sort-by='.lastTimestamp'
```

#### Docker images not pulling
```bash
# Create image pull secret
kubectl create secret docker-registry regcred \
  --docker-server=docker.io \
  --docker-username=<username> \
  --docker-password=<password> \
  --docker-email=<email> \
  -n garage-dev

# Update deployment to use secret
# Add imagePullSecrets:
#   - name: regcred
```

#### Database connection issues
```bash
# Check database pod
kubectl logs deployment/auth-postgres -n garage-dev

# Test connection
kubectl run -it --rm debug --image=postgres:15 --restart=Never \
  -- psql -h auth-postgres -U postgres -d authDB
```

---

## 8. Production Deployment Checklist

- [ ] All services passing unit tests
- [ ] Code coverage > 80%
- [ ] Security scanning passed (Trivy, SonarQube)
- [ ] Load testing completed
- [ ] Database backups configured
- [ ] SSL/TLS certificates installed
- [ ] Monitoring and alerting configured
- [ ] Disaster recovery plan in place
- [ ] Documentation updated
- [ ] Team trained on new infrastructure

---

## 9. Useful Commands

### Jenkins

```bash
# View logs
docker logs -f jenkins

# Backup Jenkins
docker exec jenkins sh -c 'tar -czf /tmp/jenkins-backup.tar.gz -C /var/jenkins_home .'
docker cp jenkins:/tmp/jenkins-backup.tar.gz ./jenkins-backup.tar.gz

# Restore Jenkins
docker cp ./jenkins-backup.tar.gz jenkins:/tmp/
docker exec jenkins sh -c 'cd /var/jenkins_home && tar -xzf /tmp/jenkins-backup.tar.gz'
```

### Kubernetes

```bash
# Get all resources
kubectl get all -n garage-dev

# Delete environment
kubectl delete namespace garage-dev

# Scale deployment
kubectl scale deployment auth-service --replicas=3 -n garage-dev

# Execute command in pod
kubectl exec -it deployment/auth-service -n garage-dev -- /bin/sh

# Port forward
kubectl port-forward svc/auth-service 8081:8081 -n garage-dev &

# Get resource usage
kubectl describe node
kubectl top node
```

### Docker

```bash
# Clean up
docker system prune -a --volumes

# View image layers
docker history <image>

# Build with BuildKit
DOCKER_BUILDKIT=1 docker build -t image:tag .
```

---

## 10. Next Steps

1. Integrate with your Git repository
2. Set up web hooks for automatic builds
3. Configure email/Slack notifications
4. Implement security policies
5. Set up backup and disaster recovery
6. Configure auto-scaling policies
7. Implement blue-green deployments
8. Set up cost monitoring

---

## Support

For issues or questions, refer to:
- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [SonarQube Documentation](https://docs.sonarqube.org/)

