#!/bin/bash

# ============================================================================
# Deploy to AWS EKS Script
# Land Title Registry System
# Platform: Linux/macOS
# ============================================================================

set -e
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "============================================================================"
echo "  Land Title Registry - AWS EKS Deployment Script"
echo "============================================================================"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v aws &> /dev/null; then
    echo -e "${RED}ERROR: AWS CLI is not installed${NC}"
    echo "Please install AWS CLI: https://aws.amazon.com/cli/"
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}ERROR: kubectl is not installed${NC}"
    echo "Please install kubectl: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites check passed${NC}"
echo ""

# Prompt for AWS configuration
echo -e "${YELLOW}AWS EKS Configuration${NC}"
echo "----------------------------------------"
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter EKS Cluster Name: " CLUSTER_NAME

echo ""
echo -e "${GREEN}AWS Region:${NC} $AWS_REGION"
echo -e "${GREEN}EKS Cluster:${NC} $CLUSTER_NAME"
echo ""

# Configure kubectl
echo -e "${YELLOW}Configuring kubectl for EKS cluster...${NC}"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Failed to configure kubectl${NC}"
    exit 1
fi

echo -e "${GREEN}✓ kubectl configured successfully${NC}"
echo ""

# Verify cluster connectivity
echo -e "${YELLOW}Verifying cluster connectivity...${NC}"
kubectl cluster-info

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Cannot connect to cluster${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Cluster connectivity verified${NC}"
echo ""

# Prompt for Docker image URI
echo -e "${YELLOW}Docker Image Configuration${NC}"
echo "----------------------------------------"
read -p "Enter Docker Image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest): " IMAGE_URI

echo ""
echo -e "${GREEN}Image URI:${NC} $IMAGE_URI"
echo ""

# Prompt for database configuration
echo -e "${YELLOW}Database Configuration${NC}"
echo "----------------------------------------"
read -p "Enter Database Host: " DB_HOST
read -p "Enter Database Port [50000]: " DB_PORT
DB_PORT=${DB_PORT:-50000}
read -p "Enter Database Name [landtitle]: " DB_NAME
DB_NAME=${DB_NAME:-landtitle}
read -p "Enter Database User: " DB_USER
read -sp "Enter Database Password: " DB_PASSWORD
echo ""

echo ""
echo -e "${GREEN}Database Host:${NC} $DB_HOST"
echo -e "${GREEN}Database Port:${NC} $DB_PORT"
echo -e "${GREEN}Database Name:${NC} $DB_NAME"
echo -e "${GREEN}Database User:${NC} $DB_USER"
echo ""

# Update Kubernetes manifests with actual values
echo -e "${YELLOW}Updating Kubernetes manifests...${NC}"

# Create temporary directory for processed manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Replace placeholders in deployment.yaml
sed -i.bak "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_HOST}}|$DB_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_PORT}}|$DB_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_NAME}}|$DB_NAME|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_USER}}|$DB_USER|g" "$TEMP_DIR/deployment.yaml"

echo -e "${GREEN}✓ Manifests updated${NC}"
echo ""

# Create Kubernetes secret for database password
echo -e "${YELLOW}Creating Kubernetes secret for database password...${NC}"
kubectl create secret generic land-title-registry-secrets \
    --from-literal=db-password="$DB_PASSWORD" \
    --namespace=land-title-registry \
    --dry-run=client -o yaml | kubectl apply -f -

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}Warning: Failed to create secret (may already exist)${NC}"
fi

echo -e "${GREEN}✓ Secret created/updated${NC}"
echo ""

# Deploy to Kubernetes
echo "============================================================================"
echo -e "${YELLOW}Deploying to AWS EKS...${NC}"
echo "============================================================================"
echo ""

# Apply namespace
echo -e "${YELLOW}Creating namespace...${NC}"
kubectl apply -f "$TEMP_DIR/namespace.yaml"
echo -e "${GREEN}✓ Namespace created${NC}"
echo ""

# Apply deployment
echo -e "${YELLOW}Deploying application...${NC}"
kubectl apply -f "$TEMP_DIR/deployment.yaml"
echo -e "${GREEN}✓ Deployment created${NC}"
echo ""

# Apply service
echo -e "${YELLOW}Creating service...${NC}"
kubectl apply -f "$TEMP_DIR/service.yaml"
echo -e "${GREEN}✓ Service created${NC}"
echo ""

# Apply ingress
echo -e "${YELLOW}Creating ingress...${NC}"
kubectl apply -f "$TEMP_DIR/ingress.yaml"
echo -e "${GREEN}✓ Ingress created${NC}"
echo ""

# Wait for deployment rollout
echo -e "${YELLOW}Waiting for deployment to complete...${NC}"
kubectl rollout status deployment/land-title-registry -n land-title-registry --timeout=5m

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Deployment rollout failed${NC}"
    echo ""
    echo "Checking pod status:"
    kubectl get pods -n land-title-registry
    echo ""
    echo "Checking pod logs:"
    kubectl logs -n land-title-registry -l app=land-title-registry --tail=50
    exit 1
fi

echo -e "${GREEN}✓ Deployment completed successfully${NC}"
echo ""

# Verify deployment
echo "============================================================================"
echo -e "${YELLOW}Verifying Deployment${NC}"
echo "============================================================================"
echo ""

echo "Pods:"
kubectl get pods -n land-title-registry
echo ""

echo "Services:"
kubectl get svc -n land-title-registry
echo ""

echo "Ingress:"
kubectl get ingress -n land-title-registry
echo ""

# Get ingress URL
INGRESS_URL=$(kubectl get ingress land-title-registry-ingress -n land-title-registry -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

if [ -n "$INGRESS_URL" ]; then
    echo -e "${GREEN}Application URL:${NC} http://$INGRESS_URL/ltr"
    echo -e "${GREEN}Health Check:${NC} http://$INGRESS_URL/ltr/health"
else
    echo -e "${YELLOW}Ingress URL not yet available. It may take a few minutes for the load balancer to be provisioned.${NC}"
    echo "Run the following command to check the ingress status:"
    echo "  kubectl get ingress land-title-registry-ingress -n land-title-registry"
fi

echo ""
echo "============================================================================"
echo -e "${GREEN}Deployment Complete!${NC}"
echo "============================================================================"
echo ""

# Cleanup
rm -rf "$TEMP_DIR"

echo "Useful commands:"
echo "  View pods:        kubectl get pods -n land-title-registry"
echo "  View logs:        kubectl logs -n land-title-registry -l app=land-title-registry -f"
echo "  Describe pod:     kubectl describe pod <pod-name> -n land-title-registry"
echo "  Scale deployment: kubectl scale deployment land-title-registry --replicas=3 -n land-title-registry"
echo "  Delete deployment: kubectl delete namespace land-title-registry"
echo ""
