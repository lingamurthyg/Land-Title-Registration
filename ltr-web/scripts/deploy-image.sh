#!/bin/bash
# ============================================================================
# Deploy Land Title Registry Web Application to AWS EKS
# ============================================================================
# This script deploys the containerized application to AWS EKS cluster
# Prerequisites: AWS CLI, kubectl, valid AWS credentials
# ============================================================================

set -e
set -o pipefail

echo "============================================================================"
echo "  Land Title Registry - AWS EKS Deployment Script"
echo "============================================================================"
echo ""

# Prompt for AWS EKS configuration
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter EKS Cluster Name: " CLUSTER_NAME
read -p "Enter Docker Image URI (full path with tag): " IMAGE_URI

echo ""
echo "--- Optional: Database Configuration ---"
echo "Press Enter to skip if using Kubernetes secrets"
read -p "Enter Database Host (or press Enter to skip): " DB_HOST
read -p "Enter Database Port (or press Enter to skip): " DB_PORT
read -p "Enter Database Name (or press Enter to skip): " DB_NAME
read -p "Enter Database User (or press Enter to skip): " DB_USER

# Set defaults if not provided
DB_HOST=${DB_HOST:-"localhost"}
DB_PORT=${DB_PORT:-"50000"}
DB_NAME=${DB_NAME:-"LTRDB"}
DB_USER=${DB_USER:-"db2admin"}

echo ""
echo "============================================================================"
echo "Configuring kubectl for EKS"
echo "============================================================================"
echo "Region: $AWS_REGION"
echo "Cluster: $CLUSTER_NAME"
echo ""

aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to configure kubectl for EKS cluster"
  exit 1
fi

echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info || {
  echo "ERROR: Cannot connect to Kubernetes cluster"
  exit 1
}

echo ""
echo "============================================================================"
echo "Updating Kubernetes Manifests"
echo "============================================================================"
echo ""

# Update deployment.yaml with image URI and environment variables
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" kubernetes/deployment.yaml
sed -i "s|{{DB_HOST}}|$DB_HOST|g" kubernetes/deployment.yaml
sed -i "s|{{DB_PORT}}|$DB_PORT|g" kubernetes/deployment.yaml
sed -i "s|{{DB_NAME}}|$DB_NAME|g" kubernetes/deployment.yaml
sed -i "s|{{DB_USER}}|$DB_USER|g" kubernetes/deployment.yaml

echo "Manifests updated successfully"

echo ""
echo "============================================================================"
echo "Deploying to AWS EKS"
echo "============================================================================"
echo ""

# Apply Kubernetes manifests in order
echo "Creating namespace..."
kubectl apply -f kubernetes/namespace.yaml

echo ""
echo "Deploying application..."
kubectl apply -f kubernetes/deployment.yaml

echo ""
echo "Creating service..."
kubectl apply -f kubernetes/service.yaml

echo ""
echo "Creating ingress..."
kubectl apply -f kubernetes/ingress.yaml

echo ""
echo "============================================================================"
echo "Waiting for Deployment Rollout"
echo "============================================================================"
echo ""

kubectl rollout status deployment/ltr-web -n ltr-web --timeout=5m

if [ $? -ne 0 ]; then
  echo "WARNING: Deployment rollout did not complete within timeout"
  echo "Check pod status with: kubectl get pods -n ltr-web"
fi

echo ""
echo "============================================================================"
echo "Deployment Status"
echo "============================================================================"
echo ""

kubectl get pods,svc,ingress -n ltr-web

echo ""
echo "============================================================================"
echo "Deployment Completed"
echo "============================================================================"
echo ""
echo "Application deployed to namespace: ltr-web"
echo "Image: $IMAGE_URI"
echo ""
echo "To check application logs:"
echo "  kubectl logs -f deployment/ltr-web -n ltr-web"
echo ""
echo "To get ingress URL:"
echo "  kubectl get ingress ltr-web-ingress -n ltr-web -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'"
echo ""
echo "To scale deployment:"
echo "  kubectl scale deployment/ltr-web -n ltr-web --replicas=3"
echo ""
echo "To rollback deployment:"
echo "  kubectl rollout undo deployment/ltr-web -n ltr-web"
echo ""
echo "============================================================================"
