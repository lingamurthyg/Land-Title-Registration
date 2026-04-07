# Deployment Guide - ltr-ejb Module

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS EKS Deployment](#aws-eks-deployment)
6. [Configuration Management](#configuration-management)
7. [Troubleshooting](#troubleshooting)
8. [Scaling and Management](#scaling-and-management)
9. [Security Considerations](#security-considerations)
10. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

This guide provides comprehensive instructions for containerizing and deploying the **ltr-ejb** module (Land Title Registry EJB Module) to AWS Elastic Kubernetes Service (EKS).

### Application Details
- **Project Name**: ltr-ejb
- **Technology Stack**: Java 8, Maven, Java EE (EJB 3.1)
- **Build Tool**: Maven 3.8+
- **Base Image**: Amazon Corretto 8 (amazoncorretto:8)
- **Target Platform**: AWS EKS (Kubernetes)
- **Application Type**: Enterprise Java Bean (EJB) Module
- **Port**: 8080 (default)

### Architecture
This is a multi-module Maven project with the following structure:
- **Parent**: land-title-registry (pom.xml)
- **Modules**: ltr-ejb, ltr-web, ltr-ear

The ltr-ejb module contains business logic and data access components packaged as an EJB JAR.

---

## Prerequisites

### Required Software

#### For Local Development
- **Docker Desktop**: 20.10+ ([Download](https://www.docker.com/products/docker-desktop))
- **Docker Compose**: 2.0+ (included with Docker Desktop)
- **Java Development Kit**: JDK 8 or higher
- **Maven**: 3.8+ (for local builds)
- **Git**: For version control

#### For AWS EKS Deployment
- **AWS CLI**: 2.x ([Installation Guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html))
- **kubectl**: 1.24+ ([Installation Guide](https://kubernetes.io/docs/tasks/tools/))
- **eksctl**: (Optional) For EKS cluster management ([Installation Guide](https://eksctl.io/introduction/#installation))

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user/role with the following permissions:
  - ECR: Full access (for pushing Docker images)
  - EKS: Full access (for cluster management)
  - EC2: Read access (for EKS node groups)
  - CloudFormation: Read access (for EKS stack management)

### AWS EKS Cluster Setup
Before deploying, ensure you have an EKS cluster running:

```bash
# Create EKS cluster using eksctl (if not already created)
eksctl create cluster \
  --name ltr-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed

# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name ltr-cluster

# Verify cluster connectivity
kubectl cluster-info
kubectl get nodes
```

### Install AWS Load Balancer Controller (for Ingress)
```bash
# Add IAM OIDC provider
eksctl utils associate-iam-oidc-provider \
  --region us-east-1 \
  --cluster ltr-cluster \
  --approve

# Create IAM policy for ALB controller
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.4.7/docs/install/iam_policy.json

aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

# Create service account
eksctl create iamserviceaccount \
  --cluster=ltr-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::<AWS_ACCOUNT_ID>:policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts \
  --approve

# Install ALB controller using Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=ltr-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd "Full Application/ltr-ejb"
```

### 2. Build the Application Locally (Optional)
```bash
# Navigate to parent directory
cd ..

# Build all modules
mvn clean package -DskipTests

# Verify build artifacts
ls -l ltr-ejb/target/*.jar
```

### 3. Run with Docker Compose
```bash
# Navigate to ltr-ejb directory
cd ltr-ejb

# Start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### 4. Access the Application
- **Application**: http://localhost:8080

---

## Building and Pushing Docker Images

### Option 1: Using build-push.sh (Linux/macOS)

```bash
cd ltr-ejb/scripts
chmod +x build-push.sh
./build-push.sh
```

**Interactive Prompts:**
1. Enter image tag (default: latest)
2. Select registry type:
   - **1**: AWS ECR
   - **2**: Docker Hub
3. Provide registry-specific details

**AWS ECR Example:**
```
Enter image tag: v1.0.0
Select Docker Registry: 1
Enter AWS Region: us-east-1
Enter AWS Account ID: 123456789012
Enter ECR Repository Name: ltr-ejb
```

**Docker Hub Example:**
```
Enter image tag: v1.0.0
Select Docker Registry: 2
Enter Docker Hub username: myusername
Enter Docker Hub password: ********
Enter repository name: ltr-ejb
```

### Option 2: Using build-push.bat (Windows)

```cmd
cd ltr-ejb\scripts
build-push.bat
```

Follow the same interactive prompts as the Linux/macOS version.

### Manual Docker Build (Advanced)

```bash
# Navigate to parent directory (build context)
cd "Full Application"

# Build image
docker build -f ltr-ejb/Dockerfile -t ltr-ejb:latest .

# Tag for ECR
docker tag ltr-ejb:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-ejb:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-ejb:latest
```

---

## AWS EKS Deployment

### Step 1: Prepare Configuration

Before deploying, gather the following information:
- AWS Region (e.g., us-east-1)
- EKS Cluster Name
- Docker Image URI (from build-push step)
- Database connection details (if applicable)

### Step 2: Deploy Using deploy-image.sh (Linux/macOS)

```bash
cd ltr-ejb/scripts
chmod +x deploy-image.sh
./deploy-image.sh
```

**Interactive Prompts:**
```
Enter AWS Region: us-east-1
Enter EKS Cluster Name: ltr-cluster
Docker Image URI: 123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-ejb:v1.0.0
Database Host: my-rds-instance.us-east-1.rds.amazonaws.com
Database Port: 5432
Database Name: ltr_db
Database User: ltr_user
JNDI DataSource: jdbc/LandTitleDB
```

### Step 3: Deploy Using deploy-image.bat (Windows)

```cmd
cd ltr-ejb\scripts
deploy-image.bat
```

Follow the same interactive prompts as the Linux/macOS version.

### Step 4: Verify Deployment

```bash
# Check namespace
kubectl get namespace ltr-ejb

# Check pods
kubectl get pods -n ltr-ejb

# Check services
kubectl get svc -n ltr-ejb

# Check ingress
kubectl get ingress -n ltr-ejb

# View pod logs
kubectl logs -n ltr-ejb -l app=ltr-ejb

# Describe pod for detailed information
kubectl describe pod <pod-name> -n ltr-ejb
```

### Step 5: Access the Application

```bash
# Get ingress URL
kubectl get ingress ltr-ejb-ingress -n ltr-ejb

# Access application
curl http://<ingress-url>
```

---

## Configuration Management

### Environment Variables

The application supports the following environment variables:

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` | No |
| `APP_ENV` | Application environment | `production` | No |
| `TZ` | Timezone | `UTC` | No |
| `DB_HOST` | Database host | `database-host` | Yes |
| `DB_PORT` | Database port | `5432` | Yes |
| `DB_NAME` | Database name | `ltr_db` | Yes |
| `DB_USER` | Database user | `ltr_user` | Yes |
| `DB_PASSWORD` | Database password | - | Yes |
| `JNDI_DATASOURCE` | JNDI DataSource name | `jdbc/LandTitleDB` | Yes |
| `LOG_LEVEL` | Logging level | `INFO` | No |

### Kubernetes Secrets

For sensitive data like database passwords, use Kubernetes secrets:

```bash
# Create secret for database password
kubectl create secret generic ltr-ejb-secrets \
  --from-literal=db-password='your-secure-password' \
  -n ltr-ejb

# Verify secret
kubectl get secret ltr-ejb-secrets -n ltr-ejb
```

### ConfigMaps

For non-sensitive configuration:

```bash
# Create ConfigMap
kubectl create configmap ltr-ejb-config \
  --from-literal=log.level=INFO \
  --from-literal=app.env=production \
  -n ltr-ejb

# Verify ConfigMap
kubectl get configmap ltr-ejb-config -n ltr-ejb
```

### Updating Configuration

```bash
# Edit deployment to update environment variables
kubectl edit deployment ltr-ejb -n ltr-ejb

# Or apply updated deployment.yaml
kubectl apply -f kubernetes/deployment.yaml
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Pod Not Starting

**Symptoms:**
- Pod status: `CrashLoopBackOff`, `Error`, or `ImagePullBackOff`

**Diagnosis:**
```bash
# Check pod status
kubectl get pods -n ltr-ejb

# Describe pod for events
kubectl describe pod <pod-name> -n ltr-ejb

# View logs
kubectl logs <pod-name> -n ltr-ejb
```

**Solutions:**
- **ImagePullBackOff**: Verify image URI and ECR permissions
- **CrashLoopBackOff**: Check application logs for startup errors
- **Insufficient resources**: Increase resource limits in deployment.yaml

#### 2. Service Not Accessible

**Symptoms:**
- Cannot access application via service or ingress

**Diagnosis:**
```bash
# Check service endpoints
kubectl get endpoints ltr-ejb-service -n ltr-ejb

# Check ingress status
kubectl describe ingress ltr-ejb-ingress -n ltr-ejb

# Test service internally
kubectl run -it --rm debug --image=busybox --restart=Never -n ltr-ejb -- wget -O- http://ltr-ejb-service
```

**Solutions:**
- Verify service selector matches pod labels
- Check ingress annotations for ALB configuration
- Ensure security groups allow traffic

#### 3. Database Connection Issues

**Symptoms:**
- Application logs show database connection errors

**Diagnosis:**
```bash
# Check environment variables
kubectl exec -it <pod-name> -n ltr-ejb -- env | grep DB_

# Test database connectivity
kubectl exec -it <pod-name> -n ltr-ejb -- nc -zv $DB_HOST $DB_PORT
```

**Solutions:**
- Verify database credentials in secrets
- Check database security groups allow EKS node traffic
- Verify JNDI DataSource configuration

#### 4. High Memory Usage

**Symptoms:**
- Pods being OOMKilled (Out of Memory)

**Diagnosis:**
```bash
# Check resource usage
kubectl top pods -n ltr-ejb

# View pod events
kubectl describe pod <pod-name> -n ltr-ejb
```

**Solutions:**
- Increase memory limits in deployment.yaml
- Adjust JVM heap size in JAVA_OPTS
- Optimize application code

#### 5. Ingress Not Creating Load Balancer

**Symptoms:**
- Ingress has no external address

**Diagnosis:**
```bash
# Check ingress status
kubectl describe ingress ltr-ejb-ingress -n ltr-ejb

# Check ALB controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
```

**Solutions:**
- Verify AWS Load Balancer Controller is installed
- Check IAM permissions for ALB controller
- Verify ingress annotations

### Viewing Logs

```bash
# View logs for all pods
kubectl logs -n ltr-ejb -l app=ltr-ejb

# Follow logs in real-time
kubectl logs -n ltr-ejb -l app=ltr-ejb -f

# View logs for specific pod
kubectl logs <pod-name> -n ltr-ejb

# View previous container logs (if pod restarted)
kubectl logs <pod-name> -n ltr-ejb --previous
```

### Debugging Pods

```bash
# Execute shell in running pod
kubectl exec -it <pod-name> -n ltr-ejb -- /bin/sh

# Run debug container in same namespace
kubectl run -it --rm debug --image=busybox --restart=Never -n ltr-ejb -- sh

# Port forward to local machine
kubectl port-forward <pod-name> -n ltr-ejb 8080:8080
```

---

## Scaling and Management

### Horizontal Pod Autoscaling (HPA)

```bash
# Create HPA based on CPU usage
kubectl autoscale deployment ltr-ejb \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n ltr-ejb

# View HPA status
kubectl get hpa -n ltr-ejb

# Describe HPA
kubectl describe hpa ltr-ejb -n ltr-ejb
```

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment ltr-ejb --replicas=5 -n ltr-ejb

# Verify scaling
kubectl get pods -n ltr-ejb
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/ltr-ejb \
  ltr-ejb=123456789012.dkr.ecr.us-east-1.amazonaws.com/ltr-ejb:v2.0.0 \
  -n ltr-ejb

# Monitor rollout
kubectl rollout status deployment/ltr-ejb -n ltr-ejb

# View rollout history
kubectl rollout history deployment/ltr-ejb -n ltr-ejb
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/ltr-ejb -n ltr-ejb

# Rollback to specific revision
kubectl rollout undo deployment/ltr-ejb --to-revision=2 -n ltr-ejb
```

### Resource Management

```bash
# View resource usage
kubectl top pods -n ltr-ejb
kubectl top nodes

# Update resource limits
kubectl edit deployment ltr-ejb -n ltr-ejb
```

---

## Security Considerations

### 1. Container Security

- **Non-root user**: Application runs as non-root user `appuser`
- **Read-only filesystem**: Consider adding `readOnlyRootFilesystem: true`
- **Security context**: Implement pod security policies

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  capabilities:
    drop:
      - ALL
```

### 2. Network Security

- **Network Policies**: Restrict pod-to-pod communication
- **Security Groups**: Configure EKS node security groups
- **TLS/SSL**: Enable HTTPS for ingress

```bash
# Create network policy
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ltr-ejb-netpol
  namespace: ltr-ejb
spec:
  podSelector:
    matchLabels:
      app: ltr-ejb
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector: {}
EOF
```

### 3. Secrets Management

- Use Kubernetes secrets for sensitive data
- Consider AWS Secrets Manager integration
- Rotate credentials regularly

```bash
# Create secret from AWS Secrets Manager
kubectl create secret generic ltr-ejb-secrets \
  --from-literal=db-password=$(aws secretsmanager get-secret-value \
    --secret-id ltr-db-password \
    --query SecretString \
    --output text) \
  -n ltr-ejb
```

### 4. Image Security

- Scan images for vulnerabilities
- Use minimal base images
- Keep base images updated

```bash
# Scan image with AWS ECR
aws ecr start-image-scan \
  --repository-name ltr-ejb \
  --image-id imageTag=latest \
  --region us-east-1

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name ltr-ejb \
  --image-id imageTag=latest \
  --region us-east-1
```

### 5. RBAC (Role-Based Access Control)

```bash
# Create service account
kubectl create serviceaccount ltr-ejb-sa -n ltr-ejb

# Create role
kubectl create role ltr-ejb-role \
  --verb=get,list,watch \
  --resource=pods,services \
  -n ltr-ejb

# Create role binding
kubectl create rolebinding ltr-ejb-rolebinding \
  --role=ltr-ejb-role \
  --serviceaccount=ltr-ejb:ltr-ejb-sa \
  -n ltr-ejb
```

---

## Technology-Specific Notes

### Java EE / EJB Considerations

#### 1. EJB Container Requirements
- This module is packaged as an EJB JAR, typically deployed within an EAR
- For standalone deployment, ensure proper EJB container runtime
- Consider migrating to Spring Boot for cloud-native deployment

#### 2. JNDI Configuration
- JNDI lookups require proper configuration in containerized environment
- Use environment variables for JNDI resource names
- Consider externalizing JNDI configuration

#### 3. Transaction Management
- EJB container-managed transactions require proper setup
- Verify transaction manager configuration
- Monitor transaction timeouts and rollbacks

#### 4. Session Beans
- Stateless session beans scale horizontally
- Stateful session beans require session replication
- Consider using external session store (Redis, Hazelcast)

### Java 8 Specific

#### 1. JVM Tuning
```bash
# Recommended JVM options for containers
JAVA_OPTS="-Xmx512m -Xms256m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/./urandom"
```

#### 2. Garbage Collection
- Use G1GC for better pause times
- Monitor GC logs for tuning
- Adjust heap size based on workload

#### 3. Memory Management
- Set appropriate heap size for container limits
- Use `-XX:+UseContainerSupport` for container awareness
- Monitor native memory usage

### Maven Multi-Module Build

#### 1. Build Context
- Dockerfile uses parent directory as build context
- All modules are built together
- Dependency resolution happens at parent level

#### 2. Dependency Management
- Parent POM manages dependency versions
- Child modules inherit dependencies
- Use `mvn dependency:tree` to analyze dependencies

#### 3. Build Optimization
- Layer caching for dependencies
- Copy POM files first for better caching
- Skip tests in Docker build

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

### Docker Documentation
- [Docker Documentation](https://docs.docker.com/)
- [Dockerfile Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

### Java EE Resources
- [Java EE 7 Tutorial](https://docs.oracle.com/javaee/7/tutorial/)
- [EJB 3.1 Specification](https://jcp.org/en/jsr/detail?id=318)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## Support and Maintenance

### Monitoring

```bash
# View cluster metrics
kubectl top nodes
kubectl top pods -n ltr-ejb

# View events
kubectl get events -n ltr-ejb --sort-by='.lastTimestamp'
```

### Backup and Recovery

```bash
# Backup namespace resources
kubectl get all -n ltr-ejb -o yaml > ltr-ejb-backup.yaml

# Restore from backup
kubectl apply -f ltr-ejb-backup.yaml
```

### Cleanup

```bash
# Delete deployment
kubectl delete namespace ltr-ejb

# Delete ECR repository
aws ecr delete-repository \
  --repository-name ltr-ejb \
  --region us-east-1 \
  --force
```

---

## Conclusion

This deployment guide provides comprehensive instructions for containerizing and deploying the ltr-ejb module to AWS EKS. Follow the steps carefully and refer to the troubleshooting section for common issues.

For additional support, consult the AWS and Kubernetes documentation or contact your DevOps team.

**Happy Deploying! 🚀**
