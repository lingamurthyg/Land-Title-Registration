# Land Title Registry - AWS EKS Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Architecture](#project-architecture)
4. [Local Development Setup](#local-development-setup)
5. [Building Docker Images](#building-docker-images)
6. [AWS EKS Prerequisites](#aws-eks-prerequisites)
7. [EKS Cluster Setup](#eks-cluster-setup)
8. [Deploying to AWS EKS](#deploying-to-aws-eks)
9. [Configuration Management](#configuration-management)
10. [Monitoring and Logging](#monitoring-and-logging)
11. [Scaling and Management](#scaling-and-management)
12. [Troubleshooting](#troubleshooting)
13. [Security Considerations](#security-considerations)
14. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

The Land Title Registry is a Java EE enterprise application built with:
- **Java Version**: Java 8 (1.8)
- **Build Tool**: Maven 3.x
- **Application Server**: IBM WebSphere Application Server 9.x (migrating to Open Liberty)
- **Packaging**: EAR (Enterprise Archive)
- **Architecture**: Multi-module Maven project (EJB + WAR + EAR)
- **Target Platform**: AWS EKS (Elastic Kubernetes Service)

This guide provides comprehensive instructions for containerizing and deploying the application to AWS EKS.

---

## Prerequisites

### Required Software
- **Docker**: Version 20.10 or higher
- **AWS CLI**: Version 2.x
- **kubectl**: Version 1.24 or higher
- **eksctl**: Version 0.140 or higher (optional, for cluster creation)
- **Maven**: Version 3.6 or higher (for local builds)
- **Java JDK**: Version 8 (for local development)

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with EKS, ECR, EC2, and VPC permissions
- AWS credentials configured (`aws configure`)

### Verify Installations
```bash
# Check Docker
docker --version

# Check AWS CLI
aws --version

# Check kubectl
kubectl version --client

# Check eksctl (optional)
eksctl version

# Check Maven
mvn --version

# Check Java
java -version
```

---

## Project Architecture

### Multi-Module Structure
```
land-title-registry/
├── ltr-ejb/          # EJB module (business logic)
├── ltr-web/          # WAR module (web layer)
└── ltr-ear/          # EAR module (packaging)
    ├── Dockerfile
    ├── docker-compose.yml
    ├── .dockerignore
    ├── kubernetes/
    │   ├── namespace.yaml
    │   ├── deployment.yaml
    │   ├── service.yaml
    │   └── ingress.yaml
    ├── scripts/
    │   ├── build-push.sh
    │   ├── build-push.bat
    │   ├── deploy-image.sh
    │   └── deploy-image.bat
    └── docs/
        └── DEPLOYMENT.md
```

### Application Components
- **EJB Layer**: Session beans, entity beans, business logic
- **Web Layer**: Servlets, JSPs, REST endpoints
- **Context Root**: `/ltr`
- **Health Endpoint**: `/health`
- **Default Port**: 8080

### External Dependencies
- **Database**: PostgreSQL (external service)
- **LDAP**: Authentication and authorization (external service)
- **JNDI DataSource**: `jdbc/LandTitleDS`

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd land-title-registry
```

### 2. Build the Project
```bash
# Build all modules
mvn clean package

# Verify EAR artifact
ls -lh ltr-ear/target/land-title-registry.ear
```

### 3. Run with Docker Compose
```bash
cd ltr-ear

# Start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### 4. Access the Application
- **Application URL**: http://localhost:8080/ltr
- **Health Check**: http://localhost:8080/health

---

## Building Docker Images

### Using Build Script (Recommended)

#### Linux/macOS
```bash
cd ltr-ear
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows
```cmd
cd ltr-ear
scripts\build-push.bat
```

### Script Features
- Interactive registry selection (AWS ECR or Docker Hub)
- Automatic image name sanitization
- Tag validation and defaults
- ECR repository auto-creation
- Authentication handling
- Build and push in one step

### Manual Docker Build
```bash
# From project root
docker build -f ltr-ear/Dockerfile -t land-title-registry:latest .

# Tag for registry
docker tag land-title-registry:latest <registry>/<repo>:latest

# Push to registry
docker push <registry>/<repo>:latest
```

### Build Context Notes
- **Build context**: Project root (not ltr-ear directory)
- **Dockerfile location**: `ltr-ear/Dockerfile`
- **Multi-stage build**: Builder stage + Runtime stage
- **Base image**: Amazon Corretto 8 (explicitly configured)

---

## AWS EKS Prerequisites

### 1. AWS CLI Configuration
```bash
# Configure AWS credentials
aws configure

# Verify configuration
aws sts get-caller-identity
```

### 2. Create ECR Repository
```bash
# Set variables
AWS_REGION=us-east-1
ECR_REPO=land-title-registry

# Create repository
aws ecr create-repository \
    --repository-name $ECR_REPO \
    --region $AWS_REGION

# Get repository URI
aws ecr describe-repositories \
    --repository-names $ECR_REPO \
    --region $AWS_REGION \
    --query 'repositories[0].repositoryUri' \
    --output text
```

### 3. Authenticate Docker with ECR
```bash
aws ecr get-login-password --region $AWS_REGION | \
    docker login --username AWS --password-stdin \
    <account-id>.dkr.ecr.$AWS_REGION.amazonaws.com
```

---

## EKS Cluster Setup

### Option 1: Using eksctl (Recommended)
```bash
# Create EKS cluster
eksctl create cluster \
    --name ltr-cluster \
    --region us-east-1 \
    --nodegroup-name ltr-nodes \
    --node-type t3.medium \
    --nodes 2 \
    --nodes-min 2 \
    --nodes-max 4 \
    --managed

# Verify cluster
kubectl get nodes
```

### Option 2: Using AWS Console
1. Navigate to EKS in AWS Console
2. Click "Create cluster"
3. Configure cluster settings:
   - Cluster name: `ltr-cluster`
   - Kubernetes version: 1.27 or higher
   - VPC and subnets
   - Security groups
4. Create node group:
   - Instance type: t3.medium
   - Desired capacity: 2
   - Min: 2, Max: 4

### Configure kubectl
```bash
# Update kubeconfig
aws eks update-kubeconfig \
    --region us-east-1 \
    --name ltr-cluster

# Verify connectivity
kubectl cluster-info
kubectl get nodes
```

### Install AWS Load Balancer Controller
```bash
# Create IAM policy
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.5.4/docs/install/iam_policy.json

aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

# Install controller using Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
    -n kube-system \
    --set clusterName=ltr-cluster \
    --set serviceAccount.create=true \
    --set serviceAccount.name=aws-load-balancer-controller
```

---

## Deploying to AWS EKS

### Using Deployment Script (Recommended)

#### Linux/macOS
```bash
cd ltr-ear
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows
```cmd
cd ltr-ear
scripts\deploy-image.bat
```

### Script Prompts
The script will prompt for:
1. **AWS Region**: e.g., `us-east-1`
2. **EKS Cluster Name**: e.g., `ltr-cluster`
3. **Docker Image URI**: Full path with tag
4. **Database Configuration**:
   - Host, Port, Name, User, Password
5. **LDAP Configuration**:
   - Host, Port, Base DN

### Manual Deployment

#### 1. Update Manifests
```bash
# Replace placeholders in deployment.yaml
IMAGE_URI="<account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest"

sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" kubernetes/deployment.yaml
sed -i "s|{{DB_HOST}}|database.example.com|g" kubernetes/deployment.yaml
sed -i "s|{{DB_PORT}}|5432|g" kubernetes/deployment.yaml
# ... (replace other placeholders)
```

#### 2. Create Secrets
```bash
# Database password
kubectl create secret generic ltr-db-secret \
    --from-literal=password='your-db-password' \
    --namespace=ltr-ear
```

#### 3. Apply Manifests
```bash
# Create namespace
kubectl apply -f kubernetes/namespace.yaml

# Deploy application
kubectl apply -f kubernetes/deployment.yaml

# Create service
kubectl apply -f kubernetes/service.yaml

# Create ingress
kubectl apply -f kubernetes/ingress.yaml
```

#### 4. Wait for Rollout
```bash
kubectl rollout status deployment/ltr-ear -n ltr-ear
```

#### 5. Verify Deployment
```bash
kubectl get pods,svc,ingress -n ltr-ear
```

---

## Configuration Management

### Environment Variables
The application uses the following environment variables:

#### Java Runtime
- `JAVA_OPTS`: JVM options (default: `-Xmx512m -Xms256m`)
- `TZ`: Timezone (default: `UTC`)
- `PORT`: Application port (default: `8080`)

#### Database
- `DB_HOST`: Database hostname
- `DB_PORT`: Database port (default: `5432`)
- `DB_NAME`: Database name
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password (from secret)
- `JDBC_URL`: Full JDBC connection string

#### LDAP/Security
- `LDAP_HOST`: LDAP server hostname
- `LDAP_PORT`: LDAP port (default: `389`)
- `LDAP_BASE_DN`: LDAP base DN

#### Application
- `APP_CONTEXT_ROOT`: Application context root (default: `/ltr`)
- `SESSION_TIMEOUT`: Session timeout in minutes (default: `30`)
- `LOG_LEVEL`: Logging level (default: `INFO`)

### Kubernetes Secrets
```bash
# Create database secret
kubectl create secret generic ltr-db-secret \
    --from-literal=password='your-password' \
    --namespace=ltr-ear

# Create LDAP secret (if needed)
kubectl create secret generic ltr-ldap-secret \
    --from-literal=bind-password='ldap-password' \
    --namespace=ltr-ear

# View secrets
kubectl get secrets -n ltr-ear
```

### ConfigMaps
```bash
# Create ConfigMap for application properties
kubectl create configmap ltr-config \
    --from-file=application.properties \
    --namespace=ltr-ear

# View ConfigMaps
kubectl get configmaps -n ltr-ear
```

---

## Monitoring and Logging

### View Logs
```bash
# View all pod logs
kubectl logs -f deployment/ltr-ear -n ltr-ear

# View specific pod logs
kubectl logs -f <pod-name> -n ltr-ear

# View previous container logs
kubectl logs --previous <pod-name> -n ltr-ear

# Tail last 100 lines
kubectl logs --tail=100 deployment/ltr-ear -n ltr-ear
```

### Health Checks
```bash
# Check pod health
kubectl get pods -n ltr-ear

# Describe pod for events
kubectl describe pod <pod-name> -n ltr-ear

# Test health endpoint
kubectl port-forward deployment/ltr-ear 8080:8080 -n ltr-ear
curl http://localhost:8080/health
```

### Metrics
```bash
# View resource usage
kubectl top pods -n ltr-ear
kubectl top nodes

# View deployment status
kubectl get deployment ltr-ear -n ltr-ear -o wide
```

### AWS CloudWatch Integration
```bash
# Install CloudWatch Container Insights
curl https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/quickstart/cwagent-fluentd-quickstart.yaml | sed "s/{{cluster_name}}/ltr-cluster/;s/{{region_name}}/us-east-1/" | kubectl apply -f -
```

---

## Scaling and Management

### Manual Scaling
```bash
# Scale deployment
kubectl scale deployment/ltr-ear --replicas=3 -n ltr-ear

# Verify scaling
kubectl get pods -n ltr-ear
```

### Horizontal Pod Autoscaler (HPA)
```bash
# Create HPA
kubectl autoscale deployment ltr-ear \
    --cpu-percent=70 \
    --min=2 \
    --max=10 \
    -n ltr-ear

# View HPA status
kubectl get hpa -n ltr-ear
```

### Rolling Updates
```bash
# Update image
kubectl set image deployment/ltr-ear \
    ltr-ear=<new-image-uri> \
    -n ltr-ear

# Monitor rollout
kubectl rollout status deployment/ltr-ear -n ltr-ear

# View rollout history
kubectl rollout history deployment/ltr-ear -n ltr-ear
```

### Rollback
```bash
# Rollback to previous version
kubectl rollout undo deployment/ltr-ear -n ltr-ear

# Rollback to specific revision
kubectl rollout undo deployment/ltr-ear --to-revision=2 -n ltr-ear
```

### Resource Management
```bash
# Update resource limits
kubectl set resources deployment/ltr-ear \
    --limits=cpu=1000m,memory=2Gi \
    --requests=cpu=500m,memory=1Gi \
    -n ltr-ear
```

---

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting
```bash
# Check pod status
kubectl get pods -n ltr-ear

# Describe pod for events
kubectl describe pod <pod-name> -n ltr-ear

# Check logs
kubectl logs <pod-name> -n ltr-ear

# Common causes:
# - Image pull errors (check ECR permissions)
# - Resource constraints (check node capacity)
# - Configuration errors (check environment variables)
```

#### 2. Image Pull Errors
```bash
# Verify ECR authentication
aws ecr get-login-password --region us-east-1 | \
    docker login --username AWS --password-stdin \
    <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Check image exists
aws ecr describe-images \
    --repository-name land-title-registry \
    --region us-east-1

# Verify image URI in deployment
kubectl get deployment ltr-ear -n ltr-ear -o yaml | grep image:
```

#### 3. Service Not Accessible
```bash
# Check service
kubectl get svc -n ltr-ear

# Check endpoints
kubectl get endpoints -n ltr-ear

# Check ingress
kubectl get ingress -n ltr-ear

# Describe ingress for events
kubectl describe ingress ltr-ear-ingress -n ltr-ear

# Check AWS Load Balancer Controller logs
kubectl logs -n kube-system deployment/aws-load-balancer-controller
```

#### 4. Database Connection Issues
```bash
# Check database secret
kubectl get secret ltr-db-secret -n ltr-ear -o yaml

# Test database connectivity from pod
kubectl exec -it <pod-name> -n ltr-ear -- /bin/bash
# Inside pod:
# telnet $DB_HOST $DB_PORT
```

#### 5. Health Check Failures
```bash
# Check liveness probe
kubectl describe pod <pod-name> -n ltr-ear | grep -A 10 Liveness

# Check readiness probe
kubectl describe pod <pod-name> -n ltr-ear | grep -A 10 Readiness

# Test health endpoint manually
kubectl port-forward <pod-name> 8080:8080 -n ltr-ear
curl http://localhost:8080/health
```

### Debug Commands
```bash
# Get all resources in namespace
kubectl get all -n ltr-ear

# Describe deployment
kubectl describe deployment ltr-ear -n ltr-ear

# Get events
kubectl get events -n ltr-ear --sort-by='.lastTimestamp'

# Execute shell in pod
kubectl exec -it <pod-name> -n ltr-ear -- /bin/bash

# Port forward for local testing
kubectl port-forward deployment/ltr-ear 8080:8080 -n ltr-ear
```

---

## Security Considerations

### 1. Image Security
- Use official base images (Amazon Corretto 8)
- Scan images for vulnerabilities
- Keep base images updated
- Use specific image tags (not `latest` in production)

### 2. Secrets Management
- Never commit secrets to version control
- Use Kubernetes Secrets for sensitive data
- Consider AWS Secrets Manager integration
- Rotate secrets regularly

### 3. Network Security
- Use Network Policies to restrict pod communication
- Enable TLS/SSL for ingress
- Use private subnets for worker nodes
- Restrict security group rules

### 4. RBAC (Role-Based Access Control)
```bash
# Create service account
kubectl create serviceaccount ltr-app -n ltr-ear

# Create role
kubectl create role ltr-role \
    --verb=get,list,watch \
    --resource=pods,services \
    -n ltr-ear

# Create role binding
kubectl create rolebinding ltr-binding \
    --role=ltr-role \
    --serviceaccount=ltr-ear:ltr-app \
    -n ltr-ear
```

### 5. Pod Security
- Run containers as non-root user (UID 1001)
- Use read-only root filesystem where possible
- Drop unnecessary capabilities
- Set resource limits

---

## Technology-Specific Notes

### Java 8 Considerations
- **JVM Options**: Configured for container environments
  - `-XX:+UseContainerSupport`: Respect container memory limits
  - `-XX:MaxRAMPercentage=75.0`: Use 75% of container memory
  - `-Xmx512m -Xms256m`: Explicit heap sizing
- **Garbage Collection**: Default G1GC suitable for most workloads
- **Timezone**: Set to UTC for consistency

### Maven Multi-Module Build
- **Build Order**: Parent → EJB → WAR → EAR
- **Dependency Caching**: POMs copied first for layer caching
- **Build Command**: `mvn clean package -DskipTests`
- **Artifact Location**: `ltr-ear/target/land-title-registry.ear`

### IBM WebSphere Migration
- **Current**: IBM WebSphere Application Server 9.x
- **Target**: Open Liberty or WildFly
- **Considerations**:
  - JNDI lookups need adaptation
  - Security realm configuration
  - DataSource configuration
  - EJB deployment descriptors

### EAR Deployment
- **Application Server Required**: EAR files need a Java EE server
- **Dockerfile Note**: Current Dockerfile builds the EAR but doesn't include a server
- **Next Steps**: Add Open Liberty or WildFly to runtime stage
- **Example**: See Open Liberty Docker images for reference

### Health Endpoint
- **Path**: `/health`
- **Port**: 8080
- **Response**: JSON with status, timestamp, and checks
- **Checks**: Database connectivity, EJB container status

---

## Additional Resources

### AWS Documentation
- [Amazon EKS User Guide](https://docs.aws.amazon.com/eks/latest/userguide/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [Amazon ECR User Guide](https://docs.aws.amazon.com/ecr/latest/userguide/)

### Kubernetes Documentation
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)

### Java EE Resources
- [Open Liberty Documentation](https://openliberty.io/docs/)
- [WildFly Documentation](https://docs.wildfly.org/)
- [Java EE 7 Tutorial](https://javaee.github.io/tutorial/)

---

## Support and Maintenance

### Getting Help
- Check application logs: `kubectl logs -f deployment/ltr-ear -n ltr-ear`
- Review Kubernetes events: `kubectl get events -n ltr-ear`
- Consult AWS Support for EKS issues
- Review application documentation

### Maintenance Tasks
- **Regular Updates**: Keep base images and dependencies updated
- **Security Patches**: Apply security updates promptly
- **Backup**: Implement database backup strategy
- **Monitoring**: Set up CloudWatch alarms for critical metrics
- **Cost Optimization**: Review resource usage and right-size instances

---

## Conclusion

This deployment guide provides comprehensive instructions for containerizing and deploying the Land Title Registry application to AWS EKS. Follow the steps carefully, and refer to the troubleshooting section for common issues.

For production deployments, ensure all security considerations are addressed, monitoring is in place, and backup strategies are implemented.

**Happy Deploying! 🚀**
