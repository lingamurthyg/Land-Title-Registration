#!/bin/bash
# ============================================================================
# Deploy to AWS EKS - Land Title Registry EAR Application
# Platform: Linux/macOS
# Target: AWS EKS (Elastic Kubernetes Service)
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
echo "AWS Region: $AWS_REGION"
echo "EKS Cluster: $CLUSTER_NAME"
echo "Image URI: $IMAGE_URI"
echo ""

# Prompt for environment-specific configuration
echo "--- Application Configuration ---"
echo "Enter values for environment variables (or press Enter to use defaults):"
echo ""

read -p "Database Host (default: database.example.com): " DB_HOST
DB_HOST=${DB_HOST:-database.example.com}

read -p "Database Port (default: 5432): " DB_PORT
DB_PORT=${DB_PORT:-5432}

read -p "Database Name (default: landtitledb): " DB_NAME
DB_NAME=${DB_NAME:-landtitledb}

read -p "Database User (default: ltruser): " DB_USER
DB_USER=${DB_USER:-ltruser}

read -sp "Database Password: " DB_PASSWORD
echo ""

read -p "LDAP Host (default: ldap.example.com): " LDAP_HOST
LDAP_HOST=${LDAP_HOST:-ldap.example.com}

read -p "LDAP Port (default: 389): " LDAP_PORT
LDAP_PORT=${LDAP_PORT:-389}

read -p "LDAP Base DN (default: dc=example,dc=com): " LDAP_BASE_DN
LDAP_BASE_DN=${LDAP_BASE_DN:-dc=example,dc=com}

echo ""
echo "Configuration captured. Proceeding with deployment..."
echo ""

# Configure kubectl for EKS
echo "============================================================================"
echo "Configuring kubectl for EKS cluster..."
echo "============================================================================"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to configure kubectl for EKS cluster"
    exit 1
fi

echo "kubectl configured successfully"
echo ""

# Verify cluster connectivity
echo "Verifying cluster connectivity..."
kubectl cluster-info || {
    echo "ERROR: Cannot connect to EKS cluster"
    exit 1
}
echo ""

# Update Kubernetes manifests with configuration
echo "============================================================================"
echo "Updating Kubernetes manifests with configuration..."
echo "============================================================================"

# Create temporary directory for processed manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Replace placeholders in deployment.yaml
sed -i.bak "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_HOST}}|$DB_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_PORT}}|$DB_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_NAME}}|$DB_NAME|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{DB_USER}}|$DB_USER|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{LDAP_HOST}}|$LDAP_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{LDAP_PORT}}|$LDAP_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i.bak "s|{{LDAP_BASE_DN}}|$LDAP_BASE_DN|g" "$TEMP_DIR/deployment.yaml"

# Create database password secret if provided
if [ -n "$DB_PASSWORD" ]; then
    echo "Creating database password secret..."
    kubectl create secret generic ltr-db-secret \
        --from-literal=password="$DB_PASSWORD" \
        --namespace=ltr-ear \
        --dry-run=client -o yaml | kubectl apply -f -
fi

echo "Manifests updated successfully"
echo ""

# Apply Kubernetes manifests
echo "============================================================================"
echo "Deploying to AWS EKS..."
echo "============================================================================"

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

# Wait for deployment rollout
echo "============================================================================"
echo "Waiting for deployment to complete..."
echo "============================================================================"
kubectl rollout status deployment/ltr-ear -n ltr-ear --timeout=5m

if [ $? -ne 0 ]; then
    echo "WARNING: Deployment rollout did not complete within timeout"
    echo "Check deployment status with: kubectl get pods -n ltr-ear"
fi

echo ""

# Verify deployment
echo "============================================================================"
echo "Verifying deployment..."
echo "============================================================================"
kubectl get pods,svc,ingress -n ltr-ear

echo ""
echo "============================================================================"
echo "  Deployment Completed!"
echo "============================================================================"
echo ""
echo "Application Details:"
echo "  Namespace: ltr-ear"
echo "  Deployment: ltr-ear"
echo "  Service: ltr-ear-service"
echo "  Ingress: ltr-ear-ingress"
echo ""
echo "To get the application URL:"
echo "  kubectl get ingress ltr-ear-ingress -n ltr-ear"
echo ""
echo "To view logs:"
echo "  kubectl logs -f deployment/ltr-ear -n ltr-ear"
echo ""
echo "To scale the deployment:"
echo "  kubectl scale deployment/ltr-ear --replicas=3 -n ltr-ear"
echo ""
echo "To rollback if needed:"
echo "  kubectl rollout undo deployment/ltr-ear -n ltr-ear"
echo ""
echo "============================================================================"

# Cleanup temporary directory
rm -rf "$TEMP_DIR"
