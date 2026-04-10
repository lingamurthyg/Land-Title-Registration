# AWS Deployment Guide - Land Title Registry

## Overview

This guide provides instructions for deploying the Land Title Registry application to AWS cloud environment. The application has been migrated from IBM WebSphere EJB 2.x to Spring Boot for cloud-native deployment.

## Cloud-Ready Features

### Architecture Changes
- **Packaging**: WAR → Executable JAR with embedded Tomcat
- **EJB**: EJB 2.x → Spring Boot Services
- **DataSource**: WAS JNDI → HikariCP Connection Pool
- **Security**: WAS JAAS → Spring Security
- **Configuration**: Hard-coded → Environment Variables
- **Credentials**: Hard-coded → AWS Secrets Manager
- **Logging**: java.util.logging → SLF4J/Logback JSON
- **Time**: java.util.Date → java.time.Instant (UTC)
- **Health Checks**: Custom Servlet → Spring Boot Actuator

### AWS Services Integration
- **AWS RDS**: PostgreSQL database with HikariCP pooling
- **AWS Secrets Manager**: Secure credential management
- **AWS CloudWatch**: Structured JSON logging and metrics
- **AWS ECS/EKS**: Container orchestration
- **AWS ALB**: Load balancing with health checks
- **AWS IAM**: Role-based access control

## Prerequisites

1. AWS Account with appropriate permissions
2. AWS CLI configured
3. Docker installed (for containerization)
4. Maven 3.6+ and Java 11+

## Deployment Options

### Option 1: Amazon ECS (Elastic Container Service)

#### Step 1: Create RDS Database

```bash
# Create RDS PostgreSQL instance
aws rds create-db-instance \
  --db-instance-identifier landtitle-db \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --engine-version 14.7 \
  --master-username dbadmin \
  --master-user-password <secure-password> \
  --allocated-storage 20 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name my-db-subnet-group \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --multi-az \
  --storage-encrypted \
  --tags Key=Application,Value=LandTitleRegistry
```

#### Step 2: Store Credentials in Secrets Manager

```bash
# Create secret for database credentials
aws secretsmanager create-secret \
  --name landtitle/db/credentials \
  --description "Database credentials for Land Title Registry" \
  --secret-string '{"username":"dbadmin","password":"<secure-password>"}' \
  --region us-east-1
```

#### Step 3: Build and Push Docker Image

```bash
# Build Docker image
docker build -t land-title-registry:latest .

# Tag for ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

docker tag land-title-registry:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest
```

#### Step 4: Create ECS Task Definition

```json
{
  "family": "land-title-registry",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::<account-id>:role/landTitleTaskRole",
  "containerDefinitions": [
    {
      "name": "land-title-registry",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "DB_HOST",
          "value": "landtitle-db.xxxxx.us-east-1.rds.amazonaws.com"
        },
        {
          "name": "DB_PORT",
          "value": "5432"
        },
        {
          "name": "DB_NAME",
          "value": "landtitle"
        },
        {
          "name": "AWS_REGION",
          "value": "us-east-1"
        },
        {
          "name": "AWS_SECRETS_ENABLED",
          "value": "true"
        },
        {
          "name": "DB_SECRET_ARN",
          "value": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:landtitle/db/credentials"
        },
        {
          "name": "CLOUDWATCH_METRICS_ENABLED",
          "value": "true"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/land-title-registry",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### Step 5: Create ECS Service

```bash
# Create ECS service with ALB
aws ecs create-service \
  --cluster land-title-cluster \
  --service-name land-title-service \
  --task-definition land-title-registry:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx,subnet-yyyyy],securityGroups=[sg-xxxxx],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:us-east-1:<account-id>:targetgroup/land-title-tg/xxxxx,containerName=land-title-registry,containerPort=8080" \
  --health-check-grace-period-seconds 60
```

### Option 2: Amazon EKS (Elastic Kubernetes Service)

#### Step 1: Create Kubernetes Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: land-title-registry
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: land-title-registry
  template:
    metadata:
      labels:
        app: land-title-registry
    spec:
      containers:
      - name: land-title-registry
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_HOST
          value: "landtitle-db.xxxxx.us-east-1.rds.amazonaws.com"
        - name: DB_PORT
          value: "5432"
        - name: DB_NAME
          value: "landtitle"
        - name: AWS_REGION
          value: "us-east-1"
        - name: AWS_SECRETS_ENABLED
          value: "true"
        - name: DB_SECRET_ARN
          value: "arn:aws:secretsmanager:us-east-1:<account-id>:secret:landtitle/db/credentials"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: land-title-registry-service
spec:
  type: LoadBalancer
  selector:
    app: land-title-registry
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

#### Step 2: Deploy to EKS

```bash
# Apply deployment
kubectl apply -f deployment.yaml

# Check deployment status
kubectl get deployments
kubectl get pods
kubectl get services
```

### Option 3: AWS Elastic Beanstalk

```bash
# Create application
aws elasticbeanstalk create-application \
  --application-name land-title-registry \
  --description "Land Title Registry Application"

# Create environment
aws elasticbeanstalk create-environment \
  --application-name land-title-registry \
  --environment-name land-title-prod \
  --solution-stack-name "64bit Amazon Linux 2 v3.4.0 running Corretto 11" \
  --option-settings file://eb-options.json
```

## IAM Permissions

### Task Role (for application)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:landtitle/db/credentials*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:us-east-1:<account-id>:log-group:/ecs/land-title-registry:*"
    }
  ]
}
```

## Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| DB_HOST | RDS endpoint | Yes | - |
| DB_PORT | Database port | No | 5432 |
| DB_NAME | Database name | Yes | - |
| DB_USERNAME | Database username | Yes* | - |
| DB_PASSWORD | Database password | Yes* | - |
| AWS_REGION | AWS region | Yes | us-east-1 |
| AWS_SECRETS_ENABLED | Enable Secrets Manager | No | false |
| DB_SECRET_ARN | Secrets Manager ARN | Yes** | - |
| CLOUDWATCH_METRICS_ENABLED | Enable CloudWatch metrics | No | true |
| LOG_LEVEL | Logging level | No | INFO |

\* Required if AWS_SECRETS_ENABLED=false  
\** Required if AWS_SECRETS_ENABLED=true

## Monitoring and Observability

### CloudWatch Logs

Logs are automatically sent to CloudWatch Logs in JSON format:

```bash
# View logs
aws logs tail /ecs/land-title-registry --follow
```

### CloudWatch Metrics

Custom metrics are published to CloudWatch:

```bash
# View metrics
aws cloudwatch get-metric-statistics \
  --namespace LandTitleRegistry \
  --metric-name http.server.requests \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Overall**: `/actuator/health`

## Security Best Practices

1. **Use AWS Secrets Manager** for all credentials
2. **Enable encryption** for RDS and EBS volumes
3. **Use IAM roles** instead of access keys
4. **Enable VPC** for network isolation
5. **Use security groups** to restrict access
6. **Enable CloudTrail** for audit logging
7. **Use HTTPS/TLS** for all communications
8. **Implement WAF** for API protection

## Troubleshooting

### Application won't start

```bash
# Check ECS task logs
aws logs tail /ecs/land-title-registry --follow

# Check task status
aws ecs describe-tasks --cluster land-title-cluster --tasks <task-id>
```

### Database connection issues

```bash
# Test RDS connectivity
aws rds describe-db-instances --db-instance-identifier landtitle-db

# Check security groups
aws ec2 describe-security-groups --group-ids sg-xxxxx
```

### Secrets Manager issues

```bash
# Verify secret exists
aws secretsmanager describe-secret --secret-id landtitle/db/credentials

# Test secret retrieval
aws secretsmanager get-secret-value --secret-id landtitle/db/credentials
```

## Cost Optimization

1. **Use Fargate Spot** for non-production environments
2. **Enable auto-scaling** based on CPU/memory
3. **Use RDS Reserved Instances** for production
4. **Implement CloudWatch Logs retention** policies
5. **Use S3 lifecycle policies** for backups

## Support

For issues or questions, contact the Cloud Migration Team.
