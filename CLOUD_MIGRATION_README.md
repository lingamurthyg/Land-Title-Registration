# Land Title Registry - Cloud-Native Migration

## Overview

This application has been migrated from IBM WebSphere Application Server (WAS) to a cloud-native Spring Boot microservices architecture, optimized for deployment on AWS.

## Cloud Readiness Fixes Applied

### 1. Hard-coded Database Credentials (CRITICAL)
**Issue**: Database credentials were hard-coded in source code  
**Fix**: Integrated AWS Secrets Manager for secure credential storage and automatic rotation
- Credentials retrieved at runtime from AWS Secrets Manager
- Secret name: `ltr/database/credentials`
- Supports automatic rotation without redeployment

### 2. EJB 2.x Usage (HIGH)
**Issue**: Application used EJB 2.x with heavy container dependencies  
**Fix**: Migrated to Spring Boot microservices
- Replaced `@Stateless` EJB with Spring `@Service`
- Replaced Container-Managed Transactions with Spring `@Transactional`
- Replaced `@RolesAllowed` with Spring Security `@PreAuthorize`
- Removed all `javax.ejb.*` dependencies

### 3. Clock/Time Dependencies (HIGH)
**Issue**: Application used `java.util.Date` and local timezone settings  
**Fix**: Migrated to `java.time` API with UTC standardization
- Replaced `java.util.Date` with `java.time.Instant` for timestamps
- Standardized on UTC timezone across all services
- Updated database timestamp handling to use UTC
- Files fixed:
  - `LandTitleDAO.java`
  - `TitleTransferDAO.java`
  - `LandTitleRegistryBean.java`
  - `LandTitle.java`
  - `TitleTransfer.java`
  - `TitleNumberGenerator.java`
  - `HealthCheckServlet.java`

### 4. Heavy Coupling to Stateful Middleware (HIGH)
**Issue**: Application depended on WebSphere clustering and session replication  
**Fix**: Externalized session state to Amazon ElastiCache (Redis)
- Removed all `com.ibm.websphere.*` dependencies
- Replaced WAS DataSource with HikariCP connection pooling
- Replaced WAS TransactionManager with Spring PlatformTransactionManager
- Integrated Spring Session with Redis for distributed session management
- Files fixed:
  - `WASDataSourceUtil.java` → Cloud-native HikariCP implementation
  - `WASTransactionUtil.java` → Spring transaction management
  - `pom.xml` files - removed WAS dependencies

### 5. WAR Packaging (LOW)
**Issue**: Application packaged as WAR requiring external application server  
**Fix**: Converted to executable JAR with embedded Tomcat
- Changed packaging from `war` to `jar` in `ltr-web/pom.xml`
- Added Spring Boot Maven Plugin for executable JAR creation
- Embedded Tomcat eliminates need for external application server
- Smaller container images and faster startup times

## Architecture Changes

### Before (WebSphere)
```
┌─────────────────────────────────────┐
│  IBM WebSphere Application Server   │
│  ┌───────────────────────────────┐  │
│  │  EAR Package                  │  │
│  │  ├── EJB Module (EJB 2.x)     │  │
│  │  └── WAR Module               │  │
│  └───────────────────────────────┘  │
│  ├── WAS DataSource (JNDI)          │
│  ├── WAS Transaction Manager        │
│  ├── WAS Security                   │
│  └── WAS Clustering                 │
└─────────────────────────────────────┘
```

### After (Spring Boot on AWS)
```
┌─────────────────────────────────────┐
│  Spring Boot Executable JAR         │
│  ├── Embedded Tomcat                │
│  ├── Spring Services (@Service)     │
│  ├── Spring Security                │
│  ├── HikariCP Connection Pool       │
│  └── Spring Session (Redis)         │
└─────────────────────────────────────┘
         │
         ├──> AWS RDS/Aurora (PostgreSQL)
         ├──> AWS Secrets Manager
         ├──> Amazon ElastiCache (Redis)
         └──> AWS CloudWatch (Logging)
```

## AWS Services Integration

### 1. AWS Secrets Manager
- **Purpose**: Secure credential storage with automatic rotation
- **Configuration**: 
  - Secret name: `ltr/database/credentials`
  - Region: Configurable via `AWS_REGION` environment variable
- **Usage**: Credentials retrieved at application startup

### 2. Amazon RDS/Aurora
- **Purpose**: Managed PostgreSQL database
- **Connection**: HikariCP connection pool (20 max connections)
- **Configuration**: JDBC URL from environment variable `DB_JDBC_URL`

### 3. Amazon ElastiCache (Redis)
- **Purpose**: Distributed session management
- **Configuration**:
  - Endpoint: `ELASTICACHE_ENDPOINT` environment variable
  - Port: `ELASTICACHE_PORT` (default: 6379)
  - SSL: Enabled in production

### 4. AWS CloudWatch
- **Purpose**: Application logging and monitoring
- **Configuration**: Log group `/aws/ecs/landtitle-registry`

## Deployment Options

### Option 1: AWS Elastic Container Service (ECS)
```bash
# Build Docker image
docker build -t landtitle-registry:latest .

# Push to Amazon ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag landtitle-registry:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/landtitle-registry:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/landtitle-registry:latest

# Deploy to ECS
aws ecs update-service --cluster landtitle-cluster --service landtitle-service --force-new-deployment
```

### Option 2: AWS Elastic Kubernetes Service (EKS)
```bash
# Build and push image (same as above)

# Deploy to EKS
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

### Option 3: AWS Fargate
- Serverless container deployment
- No EC2 instance management required
- Automatic scaling based on load

## Environment Variables

### Required
- `AWS_REGION`: AWS region (e.g., `us-east-1`)
- `DB_SECRET_NAME`: AWS Secrets Manager secret name
- `RDS_ENDPOINT`: RDS database endpoint
- `ELASTICACHE_ENDPOINT`: ElastiCache Redis endpoint

### Optional
- `SERVER_PORT`: Application port (default: 8080)
- `REDIS_PORT`: Redis port (default: 6379)
- `LOG_FILE`: Log file path (default: `/var/log/landtitle/application.log`)

## Health Checks

### Liveness Probe
```
GET /actuator/health/liveness
```

### Readiness Probe
```
GET /actuator/health/readiness
```

### Full Health Check
```
GET /actuator/health
```

## Building the Application

```bash
# Build executable JAR
mvn clean package

# Run locally
java -jar ltr-web/target/ltr-web-1.0.0.jar

# Run with AWS profile
java -jar ltr-web/target/ltr-web-1.0.0.jar --spring.profiles.active=aws
```

## Migration Summary

| Component | Before (WAS) | After (Spring Boot) | Status |
|-----------|--------------|---------------------|--------|
| Application Server | IBM WebSphere 9.x | Embedded Tomcat | ✅ Complete |
| Business Logic | EJB 2.x | Spring @Service | ✅ Complete |
| Transactions | CMT (Container-Managed) | Spring @Transactional | ✅ Complete |
| Security | WAS Security | Spring Security | ✅ Complete |
| Database Pool | WAS DataSource | HikariCP | ✅ Complete |
| Credentials | Hard-coded | AWS Secrets Manager | ✅ Complete |
| Session Management | WAS Clustering | Redis (ElastiCache) | ✅ Complete |
| Packaging | EAR/WAR | Executable JAR | ✅ Complete |
| Time Handling | java.util.Date | java.time.Instant (UTC) | ✅ Complete |
| Deployment | Manual WAS deploy | Container (ECS/EKS) | ✅ Ready |

## Next Steps

1. **Infrastructure Setup**:
   - Provision AWS RDS PostgreSQL instance
   - Create Amazon ElastiCache Redis cluster
   - Set up AWS Secrets Manager secret
   - Configure VPC and security groups

2. **CI/CD Pipeline**:
   - Set up AWS CodePipeline or GitHub Actions
   - Automate Docker image builds
   - Configure automated deployments to ECS/EKS

3. **Monitoring**:
   - Configure CloudWatch dashboards
   - Set up CloudWatch alarms
   - Enable X-Ray tracing (optional)

4. **Security**:
   - Configure AWS IAM roles for ECS tasks
   - Enable encryption at rest (RDS, ElastiCache)
   - Configure AWS WAF for API protection

## Support

For questions or issues related to the cloud migration, contact the DevOps team.
