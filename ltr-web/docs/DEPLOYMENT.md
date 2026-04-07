# Land Title Registry Web Application - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS EKS Deployment](#aws-eks-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)
8. [Security Considerations](#security-considerations)
9. [Scaling and Performance](#scaling-and-performance)

---

## Overview

The Land Title Registry Web Application (ltr-web) is a Java EE 7 web application packaged as a WAR file and deployed on Open Liberty application server. This guide covers containerization and deployment to AWS EKS (Elastic Kubernetes Service).

**Technology Stack:**
- Java 8 (Amazon Corretto)
- Maven 3.9.4
- Open Liberty 23.0.0.3
- Java EE 7 (Servlet 3.1, EJB 3.1, JNDI, JDBC)
- Docker & Kubernetes

**Application Details:**
- **Module**: ltr-web
- **Packaging**: WAR
- **Port**: 8080 (HTTP), 8443 (HTTPS)
- **Health Endpoint**: `/health`
- **Context Root**: `/`

---

## Prerequisites

### Required Software

#### For Local Development:
- **Docker Desktop** 20.10+ ([Download](https://www.docker.com/products/docker-desktop))
- **Docker Compose** 1.29+ (included with Docker Desktop)
- **Maven** 3.6+ (for local builds)
- **Java 8 JDK** (for local development)

#### For AWS EKS Deployment:
- **AWS CLI** 2.x ([Installation Guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html))
- **kubectl** 1.24+ ([Installation Guide](https://kubernetes.io/docs/tasks/tools/))
- **eksctl** (optional, for cluster management) ([Installation Guide](https://eksctl.io/introduction/#installation))
- **AWS Account** with appropriate IAM permissions

### AWS IAM Permissions Required

Your AWS user/role needs the following permissions:
- `eks:DescribeCluster`
- `eks:ListClusters`
- `ecr:GetAuthorizationToken`
- `ecr:CreateRepository`
- `ecr:DescribeRepositories`
- `ecr:PutImage`
- `ecr:BatchCheckLayerAvailability`
- `ecr:InitiateLayerUpload`
- `ecr:UploadLayerPart`
- `ecr:CompleteLayerUpload`

### Database Requirements

The application requires an external database (DB2 or compatible):
- **Database**: DB2 or PostgreSQL
- **JDBC Driver**: Included in Liberty configuration
- **Connection Details**: Configured via environment variables

---

## Local Development with Docker Compose

### Step 1: Configure Environment Variables

Create a `.env` file in the `ltr-web` directory:

```bash
# Database Configuration
DB_HOST=your-database-host
DB_PORT=50000
DB_NAME=LTRDB
DB_USER=db2admin
DB_PASSWORD=your-secure-password

# JVM Configuration
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

### Step 2: Build and Run with Docker Compose

```bash
# Navigate to ltr-web directory
cd ltr-web

# Build and start the application
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Step 3: Access the Application

- **Application URL**: http://localhost:8080
- **Health Check**: http://localhost:8080/health
- **HTTPS**: https://localhost:8443 (self-signed certificate)

### Step 4: Verify Health Status

```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00",
  "application": "Land Title Registry",
  "version": "1.0.0",
  "checks": {
    "database": { "status": "UP" },
    "ejbContainer": { "status": "UP" }
  }
}
```

---

## Building and Pushing Docker Images

### Option 1: Using Build Script (Recommended)

#### Linux/macOS:

```bash
cd ltr-web
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows:

```cmd
cd ltr-web
scripts\build-push.bat
```

The script will prompt you for:
1. **Registry Type**: AWS ECR or Docker Hub
2. **Registry Details**: Region, account ID, repository name
3. **Image Tag**: Version tag (default: latest)

### Option 2: Manual Build and Push

#### AWS ECR:

```bash
# Set variables
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
ECR_REPO=ltr-web
IMAGE_TAG=v1.0.0

# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION || true

# Build image (from parent directory)
cd ..
docker build -f ltr-web/Dockerfile -t ltr-web:$IMAGE_TAG .

# Tag image
docker tag ltr-web:$IMAGE_TAG \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG

# Push image
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
```

#### Docker Hub:

```bash
# Set variables
DOCKER_USERNAME=your-username
IMAGE_TAG=v1.0.0

# Login to Docker Hub
docker login -u $DOCKER_USERNAME

# Build image (from parent directory)
cd ..
docker build -f ltr-web/Dockerfile -t ltr-web:$IMAGE_TAG .

# Tag image
docker tag ltr-web:$IMAGE_TAG $DOCKER_USERNAME/ltr-web:$IMAGE_TAG

# Push image
docker push $DOCKER_USERNAME/ltr-web:$IMAGE_TAG
```

---

## AWS EKS Deployment

### Prerequisites

1. **EKS Cluster**: Create an EKS cluster if you don't have one

```bash
eksctl create cluster \
  --name ltr-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

2. **AWS Load Balancer Controller**: Install for ingress support

```bash
# Add Helm repository
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install AWS Load Balancer Controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=ltr-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Deployment Steps

#### Option 1: Using Deployment Script (Recommended)

##### Linux/macOS:

```bash
cd ltr-web
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

##### Windows:

```cmd
cd ltr-web
scripts\deploy-image.bat
```

The script will prompt you for:
1. **AWS Region**: e.g., us-east-1
2. **EKS Cluster Name**: Your cluster name
3. **Docker Image URI**: Full image path with tag
4. **Database Configuration**: Host, port, name, user (optional)

#### Option 2: Manual Deployment

```bash
# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name ltr-cluster

# Verify connectivity
kubectl cluster-info

# Update deployment.yaml with your image URI
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-web:v1.0.0|g' \
  kubernetes/deployment.yaml

# Update database configuration
sed -i 's|{{DB_HOST}}|your-db-host|g' kubernetes/deployment.yaml
sed -i 's|{{DB_PORT}}|50000|g' kubernetes/deployment.yaml
sed -i 's|{{DB_NAME}}|LTRDB|g' kubernetes/deployment.yaml
sed -i 's|{{DB_USER}}|db2admin|g' kubernetes/deployment.yaml

# Apply manifests
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for rollout
kubectl rollout status deployment/ltr-web -n ltr-web

# Check status
kubectl get pods,svc,ingress -n ltr-web
```

### Verify Deployment

```bash
# Check pod status
kubectl get pods -n ltr-web

# View logs
kubectl logs -f deployment/ltr-web -n ltr-web

# Check service
kubectl get svc -n ltr-web

# Get ingress URL
kubectl get ingress ltr-web-ingress -n ltr-web -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

### Access the Application

```bash
# Get the Load Balancer URL
INGRESS_URL=$(kubectl get ingress ltr-web-ingress -n ltr-web -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Access application
curl http://$INGRESS_URL/health

# Or open in browser
echo "Application URL: http://$INGRESS_URL"
```

---

## Configuration Management

### Environment Variables

The application uses the following environment variables:

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | Database hostname | localhost | Yes |
| `DB_PORT` | Database port | 50000 | Yes |
| `DB_NAME` | Database name | LTRDB | Yes |
| `DB_USER` | Database username | db2admin | Yes |
| `DB_PASSWORD` | Database password | - | Yes |
| `JAVA_OPTS` | JVM options | -Xmx512m -Xms256m | No |
| `TZ` | Timezone | UTC | No |
| `LANG` | Locale | en_US.UTF-8 | No |

### Kubernetes Secrets

For production, use Kubernetes secrets for sensitive data:

```bash
# Create database password secret
kubectl create secret generic ltr-db-secret \
  --from-literal=password='your-secure-password' \
  -n ltr-web

# Verify secret
kubectl get secret ltr-db-secret -n ltr-web
```

The deployment.yaml is already configured to use this secret for `DB_PASSWORD`.

### ConfigMaps

For application configuration files:

```bash
# Create ConfigMap from file
kubectl create configmap ltr-web-config \
  --from-file=server.xml=config/server.xml \
  -n ltr-web

# Verify ConfigMap
kubectl get configmap ltr-web-config -n ltr-web
```

---

## Monitoring and Troubleshooting

### Health Checks

The application exposes a health endpoint at `/health`:

```bash
# Check health from within cluster
kubectl exec -it deployment/ltr-web -n ltr-web -- \
  curl http://localhost:8080/health

# Check health from outside (via ingress)
curl http://$INGRESS_URL/health
```

### Viewing Logs

```bash
# Stream logs from all pods
kubectl logs -f deployment/ltr-web -n ltr-web

# View logs from specific pod
kubectl logs ltr-web-<pod-id> -n ltr-web

# View previous container logs (if crashed)
kubectl logs ltr-web-<pod-id> -n ltr-web --previous

# Tail last 100 lines
kubectl logs --tail=100 deployment/ltr-web -n ltr-web
```

### Common Issues

#### 1. Pod Not Starting

```bash
# Check pod status
kubectl describe pod ltr-web-<pod-id> -n ltr-web

# Check events
kubectl get events -n ltr-web --sort-by='.lastTimestamp'

# Common causes:
# - Image pull errors (check ECR permissions)
# - Resource limits (check node capacity)
# - Configuration errors (check environment variables)
```

#### 2. Database Connection Issues

```bash
# Check database connectivity from pod
kubectl exec -it deployment/ltr-web -n ltr-web -- \
  curl http://localhost:8080/health

# Verify environment variables
kubectl exec -it deployment/ltr-web -n ltr-web -- env | grep DB_

# Check database secret
kubectl get secret ltr-db-secret -n ltr-web -o yaml
```

#### 3. Ingress Not Working

```bash
# Check ingress status
kubectl describe ingress ltr-web-ingress -n ltr-web

# Verify AWS Load Balancer Controller
kubectl get pods -n kube-system | grep aws-load-balancer-controller

# Check ALB in AWS Console
aws elbv2 describe-load-balancers --region us-east-1
```

#### 4. Application Errors

```bash
# Check application logs
kubectl logs -f deployment/ltr-web -n ltr-web | grep ERROR

# Exec into container
kubectl exec -it deployment/ltr-web -n ltr-web -- /bin/bash

# Check Liberty server status
kubectl exec -it deployment/ltr-web -n ltr-web -- \
  server status ltrServer
```

### Performance Monitoring

```bash
# Check resource usage
kubectl top pods -n ltr-web

# Check node resource usage
kubectl top nodes

# Describe pod for resource limits
kubectl describe pod ltr-web-<pod-id> -n ltr-web | grep -A 5 "Limits"
```

---

## Security Considerations

### 1. Image Security

- **Use Official Base Images**: Amazon Corretto 8 (explicitly specified)
- **Scan Images**: Use AWS ECR image scanning

```bash
# Enable ECR scanning
aws ecr put-image-scanning-configuration \
  --repository-name ltr-web \
  --image-scanning-configuration scanOnPush=true \
  --region us-east-1

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name ltr-web \
  --image-id imageTag=v1.0.0 \
  --region us-east-1
```

### 2. Network Security

- **Use Network Policies**: Restrict pod-to-pod communication
- **Enable TLS**: Configure HTTPS with valid certificates
- **Use Security Groups**: Control EKS node access

### 3. Secrets Management

- **Never commit secrets**: Use Kubernetes secrets or AWS Secrets Manager
- **Rotate credentials**: Regularly update database passwords
- **Use IAM roles**: For AWS service access (IRSA)

```bash
# Create IAM role for service account
eksctl create iamserviceaccount \
  --name ltr-web-sa \
  --namespace ltr-web \
  --cluster ltr-cluster \
  --attach-policy-arn arn:aws:iam::aws:policy/AmazonRDSReadOnlyAccess \
  --approve
```

### 4. Pod Security

- **Run as non-root**: Already configured in Dockerfile
- **Read-only filesystem**: Where possible
- **Drop capabilities**: Minimize container privileges

---

## Scaling and Performance

### Horizontal Pod Autoscaling (HPA)

```bash
# Create HPA based on CPU usage
kubectl autoscale deployment ltr-web \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n ltr-web

# Check HPA status
kubectl get hpa -n ltr-web

# Describe HPA
kubectl describe hpa ltr-web -n ltr-web
```

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment/ltr-web --replicas=5 -n ltr-web

# Verify scaling
kubectl get pods -n ltr-web
```

### Resource Optimization

Current resource configuration:
- **Requests**: CPU: 250m, Memory: 512Mi
- **Limits**: CPU: 500m, Memory: 1Gi

Adjust based on your workload:

```yaml
resources:
  requests:
    cpu: "500m"      # Increase for higher throughput
    memory: "1Gi"    # Increase for larger heap
  limits:
    cpu: "1000m"
    memory: "2Gi"
```

### JVM Tuning

Adjust `JAVA_OPTS` in deployment.yaml:

```yaml
env:
- name: JAVA_OPTS
  value: "-Xmx1024m -Xms512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

---

## Rolling Updates and Rollbacks

### Perform Rolling Update

```bash
# Update image
kubectl set image deployment/ltr-web \
  ltr-web=123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-web:v1.1.0 \
  -n ltr-web

# Watch rollout
kubectl rollout status deployment/ltr-web -n ltr-web

# Check rollout history
kubectl rollout history deployment/ltr-web -n ltr-web
```

### Rollback Deployment

```bash
# Rollback to previous version
kubectl rollout undo deployment/ltr-web -n ltr-web

# Rollback to specific revision
kubectl rollout undo deployment/ltr-web --to-revision=2 -n ltr-web

# Verify rollback
kubectl rollout status deployment/ltr-web -n ltr-web
```

---

## Cleanup

### Remove Application

```bash
# Delete all resources
kubectl delete -f kubernetes/ingress.yaml
kubectl delete -f kubernetes/service.yaml
kubectl delete -f kubernetes/deployment.yaml
kubectl delete -f kubernetes/namespace.yaml

# Or delete namespace (removes everything)
kubectl delete namespace ltr-web
```

### Remove Docker Images

```bash
# Local images
docker rmi ltr-web:latest

# ECR images
aws ecr batch-delete-image \
  --repository-name ltr-web \
  --image-ids imageTag=v1.0.0 \
  --region us-east-1
```

---

## Additional Resources

- [Open Liberty Documentation](https://openliberty.io/docs/)
- [AWS EKS User Guide](https://docs.aws.amazon.com/eks/latest/userguide/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)

---

## Support and Troubleshooting

For issues or questions:
1. Check application logs: `kubectl logs -f deployment/ltr-web -n ltr-web`
2. Review pod events: `kubectl describe pod <pod-name> -n ltr-web`
3. Verify configuration: `kubectl get configmap,secret -n ltr-web`
4. Check health endpoint: `curl http://<ingress-url>/health`

---

**Document Version**: 1.0.0  
**Last Updated**: 2024-01-15  
**Application**: Land Title Registry Web (ltr-web)  
**Target Platform**: AWS EKS
