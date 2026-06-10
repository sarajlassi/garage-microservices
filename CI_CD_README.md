# Garage Microservices - Complete CI/CD Infrastructure

## 📋 Project Overview

Complete microservices architecture for garage management with integrated CI/CD pipeline using Jenkins, Docker, and Kubernetes.

### Services
1. **Auth Service** - User authentication and JWT token management
2. **Vehicle Service** - Vehicle and service records management
3. **Stock Service** - Product inventory and supplier order management

### Architecture
- **Framework**: Spring Boot 3.2
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Message Queue**: Apache Kafka 7.5
- **Orchestration**: Kubernetes
- **CI/CD**: Jenkins + GitHub Actions
- **Monitoring**: Prometheus + Grafana

---

## 🚀 Quick Start

### Prerequisites
```bash
# Required Tools
- JDK 17+
- Maven 3.9+
- Docker & Docker Compose
- kubectl (for Kubernetes)
- Make
```

### 1. Initial Setup
```bash
# Copy environment configuration
cp .env.example .env

# Edit .env with your values
nano .env

# Run setup
make setup
```

### 2. Build and Test
```bash
# Build all services
make build

# Run unit tests
make test

# Generate code coverage
make coverage
```

### 3. Local Development (Docker)
```bash
# Start all services
make docker-run

# View logs
make docker-logs

# Stop services
make docker-stop
```

### 4. Deploy to Kubernetes
```bash
# Deploy to DEV
make k8s-deploy-dev

# Deploy to STAGING
make k8s-deploy-staging

# Deploy to PRODUCTION
make k8s-deploy-prod
```

---

## 📁 Project Structure

```
garage-microservices/
├── auth-service/              # Authentication & Authorization
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── vehicle-service/           # Vehicle Management
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── stock-service/             # Stock Management
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── k8s/                        # Kubernetes Manifests
│   ├── dev/                    # Development environment
│   ├── staging/                # Staging environment
│   └── prod/                   # Production environment
├── devops/                     # DevOps Infrastructure
│   ├── docker-compose-devops.yml
│   ├── jenkins-casc.yaml
│   └── prometheus.yml
├── scripts/                    # Deployment scripts
│   ├── deploy-k8s.sh
│   └── setup-jenkins.sh
├── .github/workflows/          # GitHub Actions
│   └── ci-cd.yml
├── Jenkinsfile                 # Jenkins Pipeline
├── docker-compose.yml          # Local development
├── Makefile                    # Development commands
└── API_CURL_GUIDE.md          # API Documentation
```

---

## 🔄 CI/CD Pipeline

### Jenkins Pipeline

```
Checkout
    ↓
Build (Maven)
    ↓
Unit Tests
    ↓
Code Quality (SonarQube)
    ↓
Build Docker Images
    ↓
Push to Registry
    ↓
Security Scan (Trivy)
    ↓
Deploy (Dev/Staging/Prod)
    ↓
Health Check
```

### GitHub Actions Pipeline

Automated CI/CD with GitHub Actions triggered on:
- Push to branches (develop → Dev, main → Staging)
- Tags (v*. → Production)
- Pull requests (Quality checks only)

---

## 📚 Available Commands

### Build & Test
```bash
make build              # Build all services
make test               # Run unit tests
make coverage           # Generate coverage report
make sonar              # Run SonarQube analysis
make clean              # Clean artifacts
```

### Docker
```bash
make docker-build       # Build Docker images
make docker-push        # Push to registry
make docker-run         # Start with docker-compose
make docker-stop        # Stop services
make docker-logs        # View logs
```

### Kubernetes
```bash
make k8s-deploy-dev     # Deploy to DEV
make k8s-deploy-staging # Deploy to STAGING
make k8s-deploy-prod    # Deploy to PRODUCTION
make k8s-status         # Check status
make k8s-port-forward   # Setup port forwarding
```

### DevOps
```bash
make devops-up          # Start Jenkins, SonarQube, Nexus
make devops-down        # Stop infrastructure
make devops-logs        # View logs
```

### Utilities
```bash
make setup              # Initial setup
make env-check          # Check environment
make all                # Build → Test → Docker Build
```

---

## 🌐 Services Access

After running `make devops-up`:

| Service | URL | Default Credentials |
|---------|-----|-------------------|
| **Jenkins** | http://localhost:8080 | admin/admin |
| **SonarQube** | http://localhost:9000 | admin/admin |
| **Nexus** | http://localhost:8081 | admin/admin123 |
| **Portainer** | http://localhost:9001 | admin/12345678 |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **Kafka UI** | http://localhost:8090 | - |
| **Registry** | http://localhost:5000 | - |
| **Registry UI** | http://localhost:5001 | - |

---

## 🔌 API Endpoints

### Authentication Service (Port 8081)
```bash
POST   /api/auth/register       # Register new user
POST   /api/auth/login          # User login
POST   /api/auth/refresh        # Refresh token
POST   /api/auth/validate       # Validate token
GET    /api/auth/users          # Get all users (Admin)
```

### Vehicle Service (Port 8082)
```bash
POST   /api/vehicles            # Create vehicle
GET    /api/vehicles/:id        # Get vehicle
PUT    /api/vehicles/:id        # Update vehicle
DELETE /api/vehicles/:id        # Delete vehicle
GET    /api/vehicles/owner/:id  # Get user's vehicles
POST   /api/vehicles/:id/services# Schedule service
```

### Stock Service (Port 8083)
```bash
POST   /api/stock/products      # Create product
GET    /api/stock/products/:id  # Get product
PUT    /api/stock/products/:id  # Update product
POST   /api/stock/:id/add       # Add to stock
POST   /api/stock/:id/remove    # Remove from stock
POST   /api/stock/orders        # Place supplier order
```

See `API_CURL_GUIDE.md` for complete API documentation and examples.

---

## 📊 Monitoring & Observability

### Metrics
- **Prometheus**: Time-series metrics collection
- **Grafana**: Visualization and dashboards
- **Spring Boot Actuator**: Application metrics

### Logs
- **Container logs**: `docker logs <container>`
- **Kubernetes logs**: `kubectl logs <pod>`
- **Centralized logging**: (Optional - add ELK/EFK stack)

### Alerts
- Create alerts in Grafana based on metrics
- Configure email notifications
- Slack/Teams integration available

---

## 🔐 Security

### Authentication
- JWT tokens with configurable expiration
- Refresh token rotation
- Role-based access control (RBAC)

### Network
- Network policies in Kubernetes
- Service-to-service communication via service DNS
- TLS/SSL encryption (configurable)

### Data Security
- Password hashing (bcrypt)
- Encrypted database connections
- Secrets management via Kubernetes Secrets

### Scanning
- Trivy for container image vulnerabilities
- SonarQube for code quality
- Dependency checking

---

## 🐛 Troubleshooting

### Common Issues

**1. Services not starting**
```bash
# Check logs
docker logs <service-name>
kubectl logs <pod-name> -n garage-dev

# Verify network
docker network ls
kubectl get svc -n garage-dev
```

**2. Database connection errors**
```bash
# Test connection
psql -h localhost -U postgres -d authDB
docker exec postgres_container psql -U postgres -d authDB -c "SELECT 1"
```

**3. Jenkins pipeline failing**
```bash
# Access Jenkins logs
docker logs jenkins
# Check pipeline logs in Jenkins UI
# Verify credentials are set correctly
```

**4. Kubernetes pods not ready**
```bash
# Check pod status
kubectl describe pod <pod-name> -n garage-dev

# Check events
kubectl get events -n garage-dev

# Check resource limits
kubectl top pods -n garage-dev
```

---

## 📖 Documentation

- `API_CURL_GUIDE.md` - Complete API documentation with curl examples
- `CI_CD_INFRASTRUCTURE_GUIDE.md` - Detailed infrastructure setup guide
- `CONFIGURATION_GUIDE.md` - Configuration and customization
- `README_UPDATED.md` - Project updates and changes

---

## 🔄 Development Workflow

### Feature Development
```bash
# 1. Create feature branch
git checkout -b feature/my-feature

# 2. Make changes and test locally
make test

# 3. Commit and push
git push origin feature/my-feature

# 4. Create Pull Request
# GitHub Actions runs quality checks

# 5. Merge to develop
# Automatic deployment to DEV environment
```

### Release Process
```bash
# 1. Merge to main
git checkout main
git merge develop

# 2. Create release tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# 3. Automatic deployment to staging and production
# Triggered by GitHub Actions on tag push
```

---

## 📝 Environment Variables

See `.env.example` for all available configuration options:

```bash
# Docker Registry
DOCKER_REGISTRY=docker.io
DOCKER_USERNAME=your_username
DOCKER_PASSWORD=your_password

# Database
DB_USER=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=<your-secret-key>
JWT_EXPIRATION=86400000

# Jenkins
JENKINS_URL=http://localhost:8080
JENKINS_TOKEN=<token>

# SonarQube
SONAR_TOKEN=<token>

# Kubernetes
KUBE_CONFIG_PATH=~/.kube/config
```

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Make changes and test: `make test`
4. Commit: `git commit -am 'Add amazing feature'`
5. Push: `git push origin feature/amazing-feature`
6. Create Pull Request

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.

---

## 📞 Support

For issues or questions:
1. Check documentation in `CI_CD_INFRASTRUCTURE_GUIDE.md`
2. Review error logs: `docker logs` or `kubectl logs`
3. Check GitHub Issues
4. Contact the development team

---

## 🎯 Next Steps

- [ ] Customize microservices for your needs
- [ ] Configure production database
- [ ] Set up SSL/TLS certificates
- [ ] Configure monitoring and alerts
- [ ] Implement disaster recovery
- [ ] Performance testing and optimization
- [ ] Security audit
- [ ] Documentation review

---

**Happy Coding! 🚀**

