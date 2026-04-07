#!/bin/bash
set -e
set -o pipefail

# ============================================================================
# AWS EKS Deployment Script for ltr-ejb
# Deploys containerized application to Amazon Elastic Kubernetes Service
# ============================================================================

echo "=========================================="
echo "AWS EKS Deployment Script"
echo "Project: ltr-ejb"
echo "=========================================="
echo ""

# Prompt for AWS configuration
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
    echo "ERROR: AWS Region is required"
    exit 1
fi

read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo "ERROR: EKS Cluster Name is required"
    exit 1
fi

# Prompt for Docker image URI
echo ""
echo "Enter the full Docker image URI (including registry, repository, and tag)"
echo "Example: 123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-ejb:latest"
read -p "Docker Image URI: " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo "ERROR: Docker Image URI is required"
    exit 1
fi

# Prompt for application-specific environment variables
echo ""
echo "=== Application Configuration ==="
echo "Enter values for environment variables (press Enter to use defaults)"
echo ""

read -p "Database Host (default: database-host): " DB_HOST
DB_HOST=${DB_HOST:-database-host}

read -p "Database Port (default: 5432): " DB_PORT
DB_PORT=${DB_PORT:-5432}

read -p "Database Name (default: ltr_db): " DB_NAME
DB_NAME=${DB_NAME:-ltr_db}

read -p "Database User (default: ltr_user): " DB_USER
DB_USER=${DB_USER:-ltr_user}

read -p "JNDI DataSource (default: jdbc/LandTitleDB): " JNDI_DATASOURCE
JNDI_DATASOURCE=${JNDI_DATASOURCE:-jdbc/LandTitleDB}

echo ""
echo "=========================================="
echo "Configuring kubectl for EKS"
echo "=========================================="
echo "Region: $AWS_REGION"
echo "Cluster: $CLUSTER_NAME"
echo ""

# Configure kubectl to use EKS cluster
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to configure kubectl for EKS cluster"
    exit 1
fi

# Verify cluster connectivity
echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info || {
    echo "ERROR: Cannot connect to Kubernetes cluster"
    exit 1
}

echo ""
echo "=========================================="
echo "Updating Kubernetes Manifests"
echo "=========================================="
echo ""

# Create temporary directory for processed manifests
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Copy manifests to temp directory
cp -r kubernetes/* "$TEMP_DIR/"

# Replace placeholders in deployment.yaml
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_HOST}}|$DB_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PORT}}|$DB_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_NAME}}|$DB_NAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_USER}}|$DB_USER|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{JNDI_DATASOURCE}}|$JNDI_DATASOURCE|g" "$TEMP_DIR/deployment.yaml"

echo "Manifests updated successfully"
echo ""

echo "=========================================="
echo "Deploying to AWS EKS"
echo "=========================================="
echo ""

# Apply Kubernetes manifests in order
echo "Creating namespace..."
kubectl apply -f "$TEMP_DIR/namespace.yaml"

echo ""
echo "Deploying application..."
kubectl apply -f "$TEMP_DIR/deployment.yaml"

echo ""
echo "Creating service..."
kubectl apply -f "$TEMP_DIR/service.yaml"

echo ""
echo "Creating ingress..."
kubectl apply -f "$TEMP_DIR/ingress.yaml"

echo ""
echo "=========================================="
echo "Waiting for Deployment Rollout"
echo "=========================================="
echo ""

# Wait for deployment to complete
kubectl rollout status deployment/ltr-ejb -n ltr-ejb --timeout=5m

if [ $? -ne 0 ]; then
    echo "ERROR: Deployment rollout failed or timed out"
    echo ""
    echo "Checking pod status..."
    kubectl get pods -n ltr-ejb
    echo ""
    echo "Checking pod logs..."
    kubectl logs -n ltr-ejb -l app=ltr-ejb --tail=50
    exit 1
fi

echo ""
echo "=========================================="
echo "Deployment Verification"
echo "=========================================="
echo ""

# Display deployed resources
echo "Pods:"
kubectl get pods -n ltr-ejb -o wide

echo ""
echo "Services:"
kubectl get svc -n ltr-ejb

echo ""
echo "Ingress:"
kubectl get ingress -n ltr-ejb

echo ""
echo "=========================================="
echo "Deployment Completed Successfully!"
echo "=========================================="
echo ""

# Get ingress URL
INGRESS_URL=$(kubectl get ingress ltr-ejb-ingress -n ltr-ejb -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")

if [ "$INGRESS_URL" != "pending" ] && [ -n "$INGRESS_URL" ]; then
    echo "Application URL: http://$INGRESS_URL"
else
    echo "Ingress URL is being provisioned. Check status with:"
    echo "  kubectl get ingress ltr-ejb-ingress -n ltr-ejb"
fi

echo ""
echo "Useful commands:"
echo "  View pods:        kubectl get pods -n ltr-ejb"
echo "  View logs:        kubectl logs -n ltr-ejb -l app=ltr-ejb"
echo "  Describe pod:     kubectl describe pod <pod-name> -n ltr-ejb"
echo "  Scale deployment: kubectl scale deployment ltr-ejb -n ltr-ejb --replicas=3"
echo "  Delete deployment: kubectl delete namespace ltr-ejb"
echo ""
