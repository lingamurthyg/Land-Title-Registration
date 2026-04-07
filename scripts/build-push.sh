#!/bin/bash

# ============================================================================
# Build and Push Docker Image Script
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
echo "  Land Title Registry - Docker Build and Push Script"
echo "============================================================================"
echo ""

# Project configuration
PROJECT_NAME="land-title-registry"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo -e "${GREEN}Project:${NC} $PROJECT_NAME"
echo -e "${GREEN}Sanitized Image Name:${NC} $IMAGE_NAME"
echo ""

# Prompt for registry type
echo "Select Docker Registry:"
echo "  1. AWS ECR (Elastic Container Registry)"
echo "  2. Docker Hub"
echo ""
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" == "1" ]; then
    # AWS ECR Configuration
    echo ""
    echo -e "${YELLOW}AWS ECR Configuration${NC}"
    echo "----------------------------------------"
    
    read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
    read -p "Enter AWS Account ID: " AWS_ACCOUNT_ID
    read -p "Enter ECR Repository Name [$IMAGE_NAME]: " ECR_REPO
    ECR_REPO=${ECR_REPO:-$IMAGE_NAME}
    
    # Sanitize ECR repository name
    ECR_REPO=$(echo "$ECR_REPO" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9/_-' '-' | sed 's/^-*//;s/-*$//')
    
    REGISTRY_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    
    echo ""
    echo -e "${GREEN}Registry URL:${NC} $REGISTRY_URL"
    echo -e "${GREEN}Repository:${NC} $ECR_REPO"
    
    # Prompt for image tag
    read -p "Enter image tag [latest]: " IMAGE_TAG
    IMAGE_TAG=${IMAGE_TAG:-latest}
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
    [ -z "$IMAGE_TAG" ] && IMAGE_TAG="latest"
    
    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO:$IMAGE_TAG"
    
    echo ""
    echo -e "${GREEN}Full Image Name:${NC} $FULL_IMAGE_NAME"
    echo ""
    
    # Authenticate with AWS ECR
    echo -e "${YELLOW}Authenticating with AWS ECR...${NC}"
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}ERROR: ECR authentication failed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ ECR authentication successful${NC}"
    
    # Check if ECR repository exists, create if not
    echo ""
    echo -e "${YELLOW}Checking ECR repository...${NC}"
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || {
        echo -e "${YELLOW}Repository does not exist. Creating...${NC}"
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"
        echo -e "${GREEN}✓ ECR repository created${NC}"
    }
    
elif [ "$REGISTRY_CHOICE" == "2" ]; then
    # Docker Hub Configuration
    echo ""
    echo -e "${YELLOW}Docker Hub Configuration${NC}"
    echo "----------------------------------------"
    
    read -p "Enter Docker Hub Username: " DOCKER_USERNAME
    read -sp "Enter Docker Hub Password/Token: " DOCKER_PASSWORD
    echo ""
    read -p "Enter Docker Hub Repository [$IMAGE_NAME]: " DOCKER_REPO
    DOCKER_REPO=${DOCKER_REPO:-$IMAGE_NAME}
    
    # Sanitize repository name
    DOCKER_REPO=$(echo "$DOCKER_REPO" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9_-' '-' | sed 's/^-*//;s/-*$//')
    
    # Prompt for image tag
    read -p "Enter image tag [latest]: " IMAGE_TAG
    IMAGE_TAG=${IMAGE_TAG:-latest}
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
    [ -z "$IMAGE_TAG" ] && IMAGE_TAG="latest"
    
    FULL_IMAGE_NAME="$DOCKER_USERNAME/$DOCKER_REPO:$IMAGE_TAG"
    
    echo ""
    echo -e "${GREEN}Full Image Name:${NC} $FULL_IMAGE_NAME"
    echo ""
    
    # Authenticate with Docker Hub
    echo -e "${YELLOW}Authenticating with Docker Hub...${NC}"
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}ERROR: Docker Hub authentication failed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker Hub authentication successful${NC}"
    
else
    echo -e "${RED}ERROR: Invalid choice. Please select 1 or 2.${NC}"
    exit 1
fi

# Build Docker image
echo ""
echo "============================================================================"
echo -e "${YELLOW}Building Docker Image...${NC}"
echo "============================================================================"
echo ""

docker build -t "$FULL_IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Docker build failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Docker build successful${NC}"

# Push Docker image
echo ""
echo "============================================================================"
echo -e "${YELLOW}Pushing Docker Image...${NC}"
echo "============================================================================"
echo ""

docker push "$FULL_IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Docker push failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Docker push successful${NC}"

# Summary
echo ""
echo "============================================================================"
echo -e "${GREEN}Build and Push Complete!${NC}"
echo "============================================================================"
echo ""
echo -e "${GREEN}Image:${NC} $FULL_IMAGE_NAME"
echo ""
echo "Next steps:"
echo "  1. Update kubernetes/deployment.yaml with the image URI"
echo "  2. Run deploy-image.sh to deploy to AWS EKS"
echo ""
