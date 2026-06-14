#!/bin/bash

# =====================================================
# Kubernetes Deployment Script
# =====================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}
KUBECONFIG=${KUBECONFIG:-~/.kube/config}

SERVICES=(auth-service vehicle-service stock-service invoice-service)
IMAGE_PREFIX="docker.io/sjlassi/garage-microservices"

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Validation
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    echo "Usage: $0 [dev|staging|prod] [image-tag]"
    exit 1
fi

NAMESPACE="garage-${ENVIRONMENT}"

print_header "Deploying to $ENVIRONMENT environment (tag: $IMAGE_TAG)"

# Check kubectl
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed"
    exit 1
fi

print_success "kubectl found"

# Create namespace if it doesn't exist
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    print_warning "Namespace $NAMESPACE does not exist, creating..."
    kubectl apply -f k8s/namespaces.yaml
    print_success "Namespaces created"
fi

# Create secrets
print_header "Creating secrets"
kubectl create secret generic db-credentials \
    --from-literal=username=postgres \
    --from-literal=password=postgres \
    -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic jwt-secret \
    --from-literal=secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 \
    -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

print_success "Secrets created/updated"

# Apply Kubernetes manifests using kustomize
print_header "Applying Kubernetes manifests (kustomize)"
kubectl apply -k "k8s/$ENVIRONMENT/"
print_success "Manifests applied"

# Update image tags to the correct version
print_header "Updating image tags to $IMAGE_TAG"
for svc in "${SERVICES[@]}"; do
    kubectl set image deployment/"$svc" \
        "$svc"="${IMAGE_PREFIX}-${svc}:${IMAGE_TAG}" \
        -n "$NAMESPACE" || print_warning "Could not update image for $svc"
done
print_success "Image tags updated"

# Wait for deployments
print_header "Waiting for deployments to be ready"

app_deployments=("auth-service" "vehicle-service" "stock-service" "invoice-service")
infra_deployments=("auth-postgres" "vehicle-postgres" "stock-postgres" "invoice-postgres" "kafka" "zookeeper")

for deployment in "${infra_deployments[@]}"; do
    echo "Waiting for $deployment..."
    kubectl rollout status deployment/"$deployment" -n "$NAMESPACE" --timeout=5m \
        || print_warning "Failed to wait for $deployment"
done

for deployment in "${app_deployments[@]}"; do
    echo "Waiting for $deployment..."
    kubectl rollout status deployment/"$deployment" -n "$NAMESPACE" --timeout=5m \
        || print_warning "Failed to wait for $deployment"
done

print_success "All deployments ready"

# Get service IPs
print_header "Service Information"
kubectl get services -n "$NAMESPACE" -o wide

# Get pods
print_header "Pod Status"
kubectl get pods -n "$NAMESPACE" -o wide

# Port forwarding info
print_header "Port Forwarding (for local testing)"
echo "kubectl port-forward -n $NAMESPACE svc/auth-service 8081:8081 &"
echo "kubectl port-forward -n $NAMESPACE svc/vehicle-service 8082:8082 &"
echo "kubectl port-forward -n $NAMESPACE svc/stock-service 8083:8083 &"
echo "kubectl port-forward -n $NAMESPACE svc/invoice-service 8084:8084 &"
echo "kubectl port-forward -n $NAMESPACE svc/kafka-ui 8080:8080 &"

print_success "Deployment completed for $ENVIRONMENT environment!"
