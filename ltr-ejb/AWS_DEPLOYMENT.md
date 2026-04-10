# Land Title Registry - AWS Cloud Deployment Guide

## Cloud-Native Transformation Summary

This application has been transformed from IBM WebSphere EJB 2.x to a cloud-native Spring Boot microservice optimized for AWS deployment.

### Key Changes

#### 1. **Removed WebSphere Dependencies**
- ❌ Removed: `com.ibm.websphere.appserver:was_public`
- ❌ Removed: `com.ibm.websphere:uow` (UOW Manager)
- ❌ Removed: EJB 2.x container dependencies
- ✅ Added: Spring Boot 2.7.x
- ✅ Added: HikariCP connection pooling
- ✅ Added: AWS SDK v2 (Secrets Manager, CloudWatch)

#### 2. **Database Connection Management**
- ❌ Removed: WAS JNDI DataSource lookup
- ❌ Removed: Direct JDBC connections
- ✅ Added: HikariCP connection pool (20 max, 5 min idle)
- ✅ Added: AWS Secrets Manager integration for credentials
- ✅ Added: Connection timeout and retry logic

#### 3. **Transaction Management**
- ❌ Removed: EJB Container-Managed Transactions (CMT)
- ❌ Removed: WebSphere UOWManager
- ✅ Added: Spring `@Transactional` annotations
- ✅ Added: Standard JTA transaction management

#### 4. **Security & Authentication**
- ❌ Removed: WebSphere JAAS authentication
- ❌ Removed: `SessionContext.getCallerPrincipal()`
- ❌ Removed: `@RolesAllowed` (EJB)
- ✅ Added: Spring Security
- ✅ Added: `SecurityContextHolder` for user context
- ✅ Added: `@PreAuthorize` for method-level security
- ✅ Ready for: AWS Cognito, IAM, OAuth2 integration

#### 5. **Time & Date Handling**
- ❌ Removed: `java.util.Date` (timezone issues)
- ❌ Removed: `SimpleDateFormat` (not thread-safe)
- ✅ Added: `java.time.Instant` (UTC timestamps)
- ✅ Added: `DateTimeFormatter` (thread-safe)
- ✅ All timestamps stored in UTC for cloud consistency

#### 6. **Logging**
- ❌ Removed: `java.util.logging.Logger`
- ✅ Added: SLF4J with Logback
- ✅ Added: Structured JSON logging for CloudWatch
- ✅ Added: Correlation IDs for distributed tracing

#### 7. **Packaging & Deployment**
- ❌ Removed: WAR packaging (requires app server)
- ❌ Removed: EJB JAR packaging
- ✅ Added: Executable JAR with embedded Tomcat
- ✅ Added: Docker containerization
- ✅ Added: Multi-stage Dockerfile for optimization

#### 8. **API Layer**
- ❌ Removed: EJB Remote interface (RMI-IIOP)
- ❌ Removed: EJB Local interface
- ✅ Added: REST API with Spring MVC
- ✅ Added: JSON request/response
- ✅ Added: HTTP status codes and error handling

## AWS Deployment Architecture

### Recommended AWS Services

1. **Compute**: AWS ECS Fargate or EKS
2. **Database**: Amazon RDS for PostgreSQL (Multi-AZ)
3. **Secrets**: AWS Secrets Manager
4. **Load Balancer**: Application Load Balancer (ALB)
5. **Monitoring**: CloudWatch Logs + Metrics
6. **Container Registry**: Amazon ECR

### Environment Variables

Configure these environment variables in ECS Task Definition or Kubernetes ConfigMap:

```bash
# Database Configuration
DB_HOST=your-rds-endpoint.region.rds.amazonaws.com
DB_PORT=5432
DB_NAME=landtitle
DB_SECRET_NAME=ltr/db/credentials
AWS_REGION=us-east-1

# Connection Pool
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT_MS=30000

# Security
SECURITY_USER=admin
SECURITY_PASSWORD=<from-secrets-manager>

# Monitoring
CLOUDWATCH_METRICS_ENABLED=true
LOG_FORMAT=CONSOLE_JSON
ENVIRONMENT=production
```

### AWS Secrets Manager Secret Format

Create a secret named `ltr/db/credentials` with this JSON structure:

```json
{
  "username": "ltr_app_user",
  "password": "your-secure-password"
}
```

## Deployment Steps

### 1. Build Docker Image

```bash
cd ltr-ejb
docker build -t land-title-registry:latest .
```

### 2. Push to Amazon ECR

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Tag image
docker tag land-title-registry:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest

# Push image
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest
```

### 3. Create RDS Database

```bash
aws rds create-db-instance \
  --db-instance-identifier ltr-postgres \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --engine-version 14.7 \
  --master-username postgres \
  --master-user-password <secure-password> \
  --allocated-storage 100 \
  --storage-type gp3 \
  --multi-az \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name ltr-db-subnet-group
```

### 4. Store Database Credentials in Secrets Manager

```bash
aws secretsmanager create-secret \
  --name ltr/db/credentials \
  --secret-string '{"username":"ltr_app_user","password":"your-secure-password"}' \
  --region us-east-1
```

### 5. Deploy to ECS Fargate

Create an ECS Task Definition with:
- Container image from ECR
- Environment variables configured
- IAM role with permissions for Secrets Manager and CloudWatch
- Health check: `/health/ready`
- Port mapping: 8080

### 6. Configure Application Load Balancer

- Target group health check: `/health/ready`
- Health check interval: 30 seconds
- Healthy threshold: 2
- Unhealthy threshold: 3

## Monitoring & Observability

### CloudWatch Logs
- Log group: `/aws/ecs/land-title-registry`
- JSON structured logs with correlation IDs
- Automatic log retention (30 days recommended)

### CloudWatch Metrics
- Custom namespace: `LandTitleRegistry`
- Metrics: Request count, latency, error rate
- Database connection pool metrics

### Health Checks
- Liveness: `GET /health`
- Readiness: `GET /health/ready`
- Detailed: `GET /health/detailed`

## Security Considerations

1. **Network Security**
   - Deploy in private subnets
   - Use security groups to restrict access
   - Enable VPC Flow Logs

2. **Secrets Management**
   - Never hardcode credentials
   - Use AWS Secrets Manager for all sensitive data
   - Enable automatic secret rotation

3. **IAM Permissions**
   - Use least-privilege IAM roles
   - Grant only necessary permissions:
     - `secretsmanager:GetSecretValue`
     - `rds:DescribeDBInstances`
     - `cloudwatch:PutMetricData`
     - `logs:CreateLogStream`, `logs:PutLogEvents`

4. **Authentication**
   - Integrate with AWS Cognito for user authentication
   - Use JWT tokens for stateless authentication
   - Implement API Gateway for additional security layer

## Performance Optimization

1. **Connection Pooling**
   - HikariCP configured with optimal settings
   - Max pool size: 20 connections
   - Connection timeout: 30 seconds

2. **Database Optimization**
   - Use RDS read replicas for read-heavy workloads
   - Enable query caching
   - Use prepared statements (already implemented)

3. **Caching**
   - Consider Amazon ElastiCache (Redis) for session state
   - Cache frequently accessed data

4. **Auto Scaling**
   - Configure ECS Service Auto Scaling based on CPU/memory
   - Use target tracking scaling policies

## Cost Optimization

1. **Right-sizing**
   - Start with Fargate 0.5 vCPU, 1 GB memory
   - Monitor and adjust based on actual usage

2. **Database**
   - Use RDS Reserved Instances for production
   - Enable automated backups with appropriate retention

3. **Monitoring**
   - Use CloudWatch Logs Insights for log analysis
   - Set up billing alerts

## Troubleshooting

### Common Issues

1. **Database Connection Failures**
   - Check security group rules
   - Verify Secrets Manager permissions
   - Check RDS endpoint and port

2. **Health Check Failures**
   - Verify database connectivity
   - Check connection pool exhaustion
   - Review CloudWatch logs

3. **High Latency**
   - Check database query performance
   - Review connection pool settings
   - Monitor CloudWatch metrics

## Migration Checklist

- [x] Remove WebSphere dependencies
- [x] Replace EJB with Spring Boot
- [x] Implement HikariCP connection pooling
- [x] Integrate AWS Secrets Manager
- [x] Replace java.util.Date with java.time.Instant
- [x] Implement structured JSON logging
- [x] Create REST API endpoints
- [x] Add health check endpoints
- [x] Create Dockerfile
- [x] Configure Spring Security
- [x] Add CloudWatch metrics integration
- [ ] Set up AWS infrastructure (RDS, ECS, ALB)
- [ ] Configure CI/CD pipeline
- [ ] Perform load testing
- [ ] Set up monitoring and alerts

## Support

For issues or questions, contact the cloud migration team.
