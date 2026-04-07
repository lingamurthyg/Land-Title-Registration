# Land Title Registry System - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Docker Deployment](#docker-deployment)
5. [AWS EKS Deployment](#aws-eks-deployment)
6. [Configuration Management](#configuration-management)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)
9. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

The Land Title Registry System is a Java 8 enterprise application built with Maven, originally designed for IBM WebSphere Application Server. This guide covers containerization and deployment to AWS EKS using OpenLiberty as the application server.

**Application Details:**
- **Technology Stack**: Java 8, Maven, EJB 3.1, Servlets, JSP
- **Packaging**: EAR (Enterprise Archive) with embedded EJB and WAR modules
- **Application Server**: OpenLiberty 23.0.0.3 (Java EE 7 compatible)
- **Base Image**: Amazon Corretto 8
- **Target Platform**: AWS EKS (Elastic Kubernetes Service)
- **Health Endpoint**: `/ltr/health`
- **Default Port**: 8080

---

## Prerequisites

### Required Software

#### For Local Development:
- **Docker Desktop** (v20.10+)
  - Windows: https://docs.docker.com/desktop/install/windows-install/
  - macOS: https://docs.docker.com/desktop/install/mac-install/
  - Linux: https://docs.docker.com/engine/install/
- **Docker Compose** (v2.0+) - Usually included with Docker Desktop
- **Maven** (v3.6+) - For local builds (optional)
- **Java 8 JDK** - For local development (optional)

#### For AWS EKS Deployment:
- **AWS CLI** (v2.0+)
  - Installation: https://aws.amazon.com/cli/
  - Configuration: `aws configure`
- **kubectl** (v1.24+)
  - Installation: https://kubernetes.io/docs/tasks/tools/
- **eksctl** (optional, for cluster creation)
  - Installation: https://eksctl.io/introduction/#installation
- **AWS Account** with appropriate IAM permissions
  - EKS cluster management
  - ECR repository access
  - IAM role creation

### AWS IAM Permissions Required

Your AWS user/role needs the following permissions:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "eks:DescribeCluster",
        "eks:ListClusters",
        "eks:UpdateClusterConfig",
        "ecr:GetAuthorizationToken",
        "ecr:CreateRepository",
        "ecr:DescribeRepositories",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:BatchCheckLayerAvailability"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## Local Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd "Full Application"
```

### 2. Prepare Database JDBC Driver

The application requires a DB2 JDBC driver. Place the driver JAR file in the `lib` directory:

```bash
mkdir -p lib
# Copy your db2jcc4.jar to the lib directory
cp /path/to/db2jcc4.jar lib/
```

### 3. Configure Environment Variables

Create a `.env` file in the project root:

```bash
# Database Configuration
DB_HOST=your-db-host
DB_PORT=50000
DB_NAME=landtitle
DB_USER=db2admin
DB_PASSWORD=your-secure-password

# Application Configuration
APP_ENV=development
LOG_LEVEL=DEBUG
```

### 4. Build the Application Locally (Optional)

```bash
# Build all modules
mvn clean package -DskipTests

# Verify EAR artifact
ls -lh ltr-ear/target/land-title-registry.ear
```

---

## Docker Deployment

### 1. Build Docker Image

```bash
# Linux/macOS
docker build -t land-title-registry:latest .

# Windows
docker build -t land-title-registry:latest .
```

### 2. Run with Docker Compose

```bash
# Start the application
docker-compose up -d

# View logs
docker-compose logs -f land-title-registry

# Stop the application
docker-compose down
```

### 3. Access the Application

- **Application URL**: http://localhost:8080/ltr
- **Health Check**: http://localhost:8080/ltr/health

### 4. Verify Health Status

```bash
curl http://localhost:8080/ltr/health
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

## AWS EKS Deployment

### Step 1: Create EKS Cluster (If Not Exists)

#### Option A: Using eksctl (Recommended)

```bash
eksctl create cluster \
  --name land-title-registry-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

#### Option B: Using AWS Console

1. Navigate to AWS EKS Console
2. Click "Create cluster"
3. Configure cluster settings:
   - Name: `land-title-registry-cluster`
   - Kubernetes version: 1.28 or later
   - VPC and subnets
   - Security groups
4. Create node group with t3.medium instances

### Step 2: Install AWS Load Balancer Controller

The AWS Load Balancer Controller is required for Ingress support.

```bash
# Create IAM policy
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.6.0/docs/install/iam_policy.json

aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

# Create IAM role and service account
eksctl create iamserviceaccount \
  --cluster=land-title-registry-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::<AWS_ACCOUNT_ID>:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# Install the controller using Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=land-title-registry-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 3: Build and Push Docker Image

#### Using the Automated Script (Recommended)

**Linux/macOS:**
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Windows:**
```cmd
scripts\build-push.bat
```

The script will prompt you for:
1. Registry type (AWS ECR or Docker Hub)
2. Registry credentials and details
3. Image tag

#### Manual Build and Push to AWS ECR

```bash
# Set variables
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
ECR_REPO=land-title-registry
IMAGE_TAG=latest

# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository \
  --repository-name $ECR_REPO \
  --region $AWS_REGION

# Build and tag image
docker build -t $ECR_REPO:$IMAGE_TAG .
docker tag $ECR_REPO:$IMAGE_TAG \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
```

### Step 4: Deploy to EKS

#### Using the Automated Script (Recommended)

**Linux/macOS:**
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

**Windows:**
```cmd
scripts\deploy-image.bat
```

The script will prompt you for:
1. AWS region and EKS cluster name
2. Docker image URI
3. Database configuration (host, port, name, user, password)

#### Manual Deployment

```bash
# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name land-title-registry-cluster

# Verify connectivity
kubectl cluster-info

# Update deployment.yaml with your image URI
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest|g' \
  kubernetes/deployment.yaml

# Update database configuration
sed -i 's|{{DB_HOST}}|your-db-host|g' kubernetes/deployment.yaml
sed -i 's|{{DB_PORT}}|50000|g' kubernetes/deployment.yaml
sed -i 's|{{DB_NAME}}|landtitle|g' kubernetes/deployment.yaml
sed -i 's|{{DB_USER}}|db2admin|g' kubernetes/deployment.yaml

# Create secret for database password
kubectl create secret generic land-title-registry-secrets \
  --from-literal=db-password='your-secure-password' \
  --namespace=land-title-registry

# Apply Kubernetes manifests
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for deployment
kubectl rollout status deployment/land-title-registry -n land-title-registry

# Verify deployment
kubectl get pods -n land-title-registry
kubectl get svc -n land-title-registry
kubectl get ingress -n land-title-registry
```

### Step 5: Access the Application

```bash
# Get the load balancer URL
INGRESS_URL=$(kubectl get ingress land-title-registry-ingress \
  -n land-title-registry \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

echo "Application URL: http://$INGRESS_URL/ltr"
echo "Health Check: http://$INGRESS_URL/ltr/health"
```

**Note**: It may take 2-3 minutes for the AWS Load Balancer to be fully provisioned.

---

## Configuration Management

### Environment Variables

The application supports the following environment variables:

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` | No |
| `TZ` | Timezone | `UTC` | No |
| `PORT` | Application port | `8080` | No |
| `APP_ENV` | Environment name | `production` | No |
| `LOG_LEVEL` | Logging level | `INFO` | No |
| `DB_HOST` | Database host | - | Yes |
| `DB_PORT` | Database port | `50000` | Yes |
| `DB_NAME` | Database name | - | Yes |
| `DB_USER` | Database user | - | Yes |
| `DB_PASSWORD` | Database password | - | Yes |

### Kubernetes ConfigMap

For advanced configuration, create a ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: land-title-registry-config
  namespace: land-title-registry
data:
  server.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <server description="Custom Configuration">
      <!-- Custom Liberty configuration -->
    </server>
```

Apply the ConfigMap:
```bash
kubectl apply -f configmap.yaml
```

### Kubernetes Secrets

Store sensitive data in Kubernetes Secrets:

```bash
# Create secret from literal values
kubectl create secret generic land-title-registry-secrets \
  --from-literal=db-password='your-password' \
  --from-literal=api-key='your-api-key' \
  --namespace=land-title-registry

# Create secret from file
kubectl create secret generic land-title-registry-secrets \
  --from-file=db-password=./secrets/db-password.txt \
  --namespace=land-title-registry
```

---

## Troubleshooting

### Common Issues

#### 1. Pod Fails to Start

**Symptoms:**
- Pod status: `CrashLoopBackOff` or `Error`

**Diagnosis:**
```bash
# Check pod status
kubectl get pods -n land-title-registry

# Describe pod for events
kubectl describe pod <pod-name> -n land-title-registry

# View pod logs
kubectl logs <pod-name> -n land-title-registry

# View previous container logs (if restarted)
kubectl logs <pod-name> -n land-title-registry --previous
```

**Common Causes:**
- Database connection failure
- Missing JDBC driver
- Insufficient memory/CPU
- Invalid configuration

**Solutions:**
- Verify database connectivity: `telnet $DB_HOST $DB_PORT`
- Check database credentials in secrets
- Increase resource limits in deployment.yaml
- Review application logs for errors

#### 2. Service Not Accessible

**Symptoms:**
- Cannot access application via ingress URL
- Connection timeout or refused

**Diagnosis:**
```bash
# Check service endpoints
kubectl get endpoints -n land-title-registry

# Check ingress status
kubectl describe ingress land-title-registry-ingress -n land-title-registry

# Check load balancer
kubectl get ingress -n land-title-registry -o wide
```

**Solutions:**
- Verify AWS Load Balancer Controller is installed
- Check security groups allow traffic on ports 80/443
- Verify target group health in AWS Console
- Check pod health: `kubectl get pods -n land-title-registry`

#### 3. Health Check Failures

**Symptoms:**
- Pods restarting frequently
- Readiness probe failures

**Diagnosis:**
```bash
# Check health endpoint directly
kubectl port-forward <pod-name> 8080:8080 -n land-title-registry
curl http://localhost:8080/ltr/health
```

**Solutions:**
- Increase `initialDelaySeconds` in probes (application startup time)
- Verify database connectivity
- Check application logs for errors
- Adjust probe timeout and failure thresholds

#### 4. Image Pull Errors

**Symptoms:**
- Pod status: `ImagePullBackOff` or `ErrImagePull`

**Diagnosis:**
```bash
kubectl describe pod <pod-name> -n land-title-registry
```

**Solutions:**
- Verify image URI is correct
- Check ECR repository permissions
- Ensure EKS nodes have ECR access (IAM role)
- Re-authenticate with ECR: `aws ecr get-login-password`

### Debugging Commands

```bash
# Get all resources in namespace
kubectl get all -n land-title-registry

# View events
kubectl get events -n land-title-registry --sort-by='.lastTimestamp'

# Execute command in pod
kubectl exec -it <pod-name> -n land-title-registry -- /bin/bash

# Port forward for local testing
kubectl port-forward <pod-name> 8080:8080 -n land-title-registry

# View resource usage
kubectl top pods -n land-title-registry
kubectl top nodes

# Check logs from all pods
kubectl logs -n land-title-registry -l app=land-title-registry --tail=100 -f
```

### EKS-Specific Troubleshooting

#### Check Node Status
```bash
kubectl get nodes
kubectl describe node <node-name>
```

#### Check IAM Roles
```bash
# Verify node IAM role has ECR permissions
aws iam get-role --role-name <node-role-name>
```

#### Check VPC and Networking
```bash
# Verify security groups
aws ec2 describe-security-groups --group-ids <sg-id>

# Check VPC configuration
aws eks describe-cluster --name land-title-registry-cluster --query 'cluster.resourcesVpcConfig'
```

---

## Security Considerations

### 1. Container Security

- **Non-root User**: Application runs as `liberty` user (UID 1001)
- **Read-only Filesystem**: Consider mounting volumes as read-only
- **Security Scanning**: Scan images for vulnerabilities
  ```bash
  docker scan land-title-registry:latest
  ```

### 2. Network Security

- **TLS/SSL**: Configure HTTPS in ingress with ACM certificates
  ```yaml
  annotations:
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:region:account:certificate/id
  ```
- **Network Policies**: Restrict pod-to-pod communication
- **Security Groups**: Limit ingress to required ports only

### 3. Secrets Management

- **Never commit secrets**: Use `.gitignore` for sensitive files
- **Use AWS Secrets Manager**: For production secrets
  ```bash
  # Install External Secrets Operator
  helm repo add external-secrets https://charts.external-secrets.io
  helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
  ```
- **Rotate credentials**: Regularly update database passwords and API keys

### 4. RBAC (Role-Based Access Control)

Create service accounts with minimal permissions:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: land-title-registry-sa
  namespace: land-title-registry
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: land-title-registry-role
  namespace: land-title-registry
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: land-title-registry-rolebinding
  namespace: land-title-registry
subjects:
- kind: ServiceAccount
  name: land-title-registry-sa
roleRef:
  kind: Role
  name: land-title-registry-role
  apiGroup: rbac.authorization.k8s.io
```

### 5. Monitoring and Auditing

- **CloudWatch Logs**: Enable container insights
  ```bash
  aws eks update-cluster-config \
    --name land-title-registry-cluster \
    --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}'
  ```
- **AWS CloudTrail**: Enable for API auditing
- **Prometheus/Grafana**: For application metrics

---

## Technology-Specific Notes

### Java 8 and OpenLiberty

#### JVM Tuning

The application uses the following JVM options:
```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

**Explanation:**
- `-Xmx512m`: Maximum heap size
- `-Xms256m`: Initial heap size
- `-XX:+UseContainerSupport`: Respect container memory limits
- `-XX:MaxRAMPercentage=75.0`: Use 75% of container memory for heap

**Tuning Recommendations:**
- For production, increase heap size based on load
- Monitor GC logs: Add `-Xlog:gc*:file=/logs/gc.log`
- Use G1GC for better performance: `-XX:+UseG1GC`

#### OpenLiberty Configuration

The application uses OpenLiberty 23.0.0.3 with Java EE 7 features:
- `javaee-7.0`: Full Java EE 7 profile
- `localConnector-1.0`: For local JMX connections

**Custom Configuration:**
Place custom `server.xml` in `config/` directory and mount as volume.

#### EJB and JNDI

The application uses:
- **EJB 3.1**: Session beans for business logic
- **JNDI**: For DataSource and EJB lookups
- **JTA**: Container-managed transactions

**JNDI Names:**
- DataSource: `jdbc/LandTitleDS`
- EJB: `ejblocal:LandTitleRegistryLocal`

### Database Connectivity

#### DB2 JDBC Driver

The application requires IBM DB2 JDBC driver (`db2jcc4.jar`).

**Installation:**
1. Download from IBM website or extract from DB2 installation
2. Place in `lib/` directory
3. Mount as volume in container: `/opt/ol/usr/servers/defaultServer/lib`

**Connection Pool Configuration:**
```xml
<dataSource id="LandTitleDS" jndiName="jdbc/LandTitleDS">
  <jdbcDriver libraryRef="db2-library"/>
  <properties.db2.jcc
    serverName="${env.DB_HOST}"
    portNumber="${env.DB_PORT}"
    databaseName="${env.DB_NAME}"
    user="${env.DB_USER}"
    password="${env.DB_PASSWORD}"/>
  <connectionManager
    maxPoolSize="50"
    minPoolSize="10"
    connectionTimeout="30s"
    maxIdleTime="1800s"/>
</dataSource>
```

### Migration from WebSphere

This application was migrated from IBM WebSphere Application Server to OpenLiberty:

**Key Changes:**
1. **Application Server**: WebSphere → OpenLiberty
2. **Base Image**: WebSphere Liberty → Amazon Corretto 8
3. **Configuration**: `server.xml` replaces WebSphere admin console
4. **Security**: JAAS/LDAP → Kubernetes RBAC + external auth
5. **Monitoring**: PMI → Prometheus/CloudWatch

**Compatibility:**
- EJB 2.x/3.x: Fully supported
- Servlets: Java EE 7 compatible
- JNDI: Standard JNDI lookups work
- JTA: Container-managed transactions supported

---

## Scaling and Performance

### Horizontal Pod Autoscaling (HPA)

Create HPA based on CPU/memory usage:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: land-title-registry-hpa
  namespace: land-title-registry
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: land-title-registry
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

Apply HPA:
```bash
kubectl apply -f hpa.yaml
kubectl get hpa -n land-title-registry
```

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment land-title-registry --replicas=5 -n land-title-registry

# Verify scaling
kubectl get pods -n land-title-registry
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/land-title-registry \
  land-title-registry=123456789012.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:v2.0.0 \
  -n land-title-registry

# Monitor rollout
kubectl rollout status deployment/land-title-registry -n land-title-registry

# Rollback if needed
kubectl rollout undo deployment/land-title-registry -n land-title-registry
```

---

## Backup and Disaster Recovery

### Database Backups

Ensure regular database backups are configured:
```bash
# Example: DB2 backup
db2 backup database landtitle to /backup
```

### Kubernetes Resource Backups

Use Velero for cluster backups:
```bash
# Install Velero
velero install --provider aws --bucket my-backup-bucket --backup-location-config region=us-east-1

# Create backup
velero backup create land-title-registry-backup --include-namespaces land-title-registry

# Restore from backup
velero restore create --from-backup land-title-registry-backup
```

---

## Monitoring and Observability

### CloudWatch Container Insights

Enable Container Insights for EKS:
```bash
aws eks update-cluster-config \
  --name land-title-registry-cluster \
  --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}'
```

### Application Logs

View application logs:
```bash
# Real-time logs
kubectl logs -f -n land-title-registry -l app=land-title-registry

# Logs from specific pod
kubectl logs <pod-name> -n land-title-registry

# Export logs to file
kubectl logs <pod-name> -n land-title-registry > app.log
```

### Metrics

Monitor application metrics:
```bash
# Pod resource usage
kubectl top pods -n land-title-registry

# Node resource usage
kubectl top nodes
```

---

## Support and Maintenance

### Useful Commands Reference

```bash
# Deployment Management
kubectl get deployments -n land-title-registry
kubectl describe deployment land-title-registry -n land-title-registry
kubectl edit deployment land-title-registry -n land-title-registry

# Pod Management
kubectl get pods -n land-title-registry -o wide
kubectl delete pod <pod-name> -n land-title-registry
kubectl exec -it <pod-name> -n land-title-registry -- /bin/bash

# Service Management
kubectl get svc -n land-title-registry
kubectl describe svc land-title-registry-service -n land-title-registry

# Ingress Management
kubectl get ingress -n land-title-registry
kubectl describe ingress land-title-registry-ingress -n land-title-registry

# ConfigMap and Secrets
kubectl get configmaps -n land-title-registry
kubectl get secrets -n land-title-registry
kubectl describe secret land-title-registry-secrets -n land-title-registry

# Cleanup
kubectl delete namespace land-title-registry
```

### Maintenance Windows

For planned maintenance:
1. Scale down replicas: `kubectl scale deployment land-title-registry --replicas=0 -n land-title-registry`
2. Perform maintenance
3. Scale up: `kubectl scale deployment land-title-registry --replicas=2 -n land-title-registry`

---

## Appendix

### A. Complete Deployment Checklist

- [ ] Prerequisites installed (Docker, AWS CLI, kubectl)
- [ ] AWS credentials configured
- [ ] EKS cluster created and accessible
- [ ] AWS Load Balancer Controller installed
- [ ] Database accessible from EKS VPC
- [ ] DB2 JDBC driver available
- [ ] Docker image built and pushed to ECR
- [ ] Kubernetes manifests updated with image URI and config
- [ ] Secrets created for sensitive data
- [ ] Deployment applied and pods running
- [ ] Service and Ingress created
- [ ] Application accessible via load balancer URL
- [ ] Health check endpoint responding
- [ ] Monitoring and logging configured

### B. Resource Requirements

**Minimum:**
- CPU: 250m (0.25 cores)
- Memory: 512Mi

**Recommended:**
- CPU: 500m (0.5 cores)
- Memory: 1Gi

**Production:**
- CPU: 1000m (1 core)
- Memory: 2Gi
- Replicas: 3+

### C. Cost Optimization

- Use Spot Instances for non-production environments
- Enable cluster autoscaler for dynamic scaling
- Use Fargate for serverless pod execution
- Implement pod disruption budgets for cost-effective scaling

---

## Conclusion

This deployment guide provides comprehensive instructions for containerizing and deploying the Land Title Registry System to AWS EKS. For additional support or questions, please contact the development team.

**Version**: 1.0.0  
**Last Updated**: 2024-01-15  
**Maintained By**: Land Title Registry Team
