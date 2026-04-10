# Land Title Registry - Cloud Native Deployment Guide

## Overview
This application has been migrated from IBM WebSphere to a cloud-native architecture for AWS deployment.

## Cloud Readiness Fixes Applied

### 1. Hard-coded Database Credentials (cr-java-0069) ✅
**Fixed:**
- Replaced WAS-specific DataSource with HikariCP connection pooling
- Database credentials now loaded from environment variables
- Compatible with AWS Secrets Manager
- Configuration: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

### 2. EJB 2.x Usage (cr-java-0085) ✅
**Fixed:**
- Migrated from EJB 2.x to Spring Boot services
- Replaced stateless session beans with Spring @Service components
- Removed EJB-specific annotations and interfaces
- Standard JTA @Transactional for transaction management

### 3. Clock/Time Dependencies (cr-java-0111) ✅
**Fixed:**
- Replaced `java.util.Date` with `java.time.Instant`
- All timestamps use UTC for consistency across regions
- Timezone-safe date handling for distributed systems
- ISO-8601 formatted timestamps for logging

### 4. Heavy Coupling to Stateful Middleware (cr-java-0116) ✅
**Fixed:**
- Removed WebSphere-specific dependencies (UOWManager, WSDataSource)
- Replaced with standard Java APIs and Spring Boot
- Stateless service design for horizontal scaling
- No vendor lock-in

### 5. Synchronous Blocking Operations (cr-java-0099) ✅
**Fixed:**
- Added async operations with CompletableFuture
- Thread pool for concurrent database operations
- Non-blocking I/O patterns for high throughput
- Configurable thread pool size via environment variables

### 6. WAR Packaging (cr-java-0107) ✅
**Fixed:**
- Converted from WAR to executable JAR
- Embedded Tomcat server (Spring Boot)
- Docker container support
- Compatible with AWS ECS, EKS, Elastic Beanstalk

## Architecture Changes

### Before (WebSphere)
```
EAR Package
├── ltr-ejb.jar (EJB 2.x Session Beans)
├── ltr-web.war (Servlets)
└── lib/ (WAS-specific libraries)
```

### After (Cloud Native)
```
Executable JAR
├── Spring Boot Application
├── REST Controllers
├── Service Layer (@Transactional)
├── DAO Layer (HikariCP)
└── Embedded Tomcat
```

## Environment Variables

### Required
- `DB_URL` - Database JDBC URL (e.g., `jdbc:postgresql://rds-endpoint:5432/ltrdb`)
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

### Optional
- `SERVER_PORT` - HTTP port (default: 8080)
- `ENVIRONMENT` - Environment name (development/staging/production)
- `AWS_REGION` - AWS region (default: us-east-1)
- `DB_POOL_SIZE` - HikariCP pool size (default: 10)
- `LOG_LEVEL` - Logging level (default: INFO)

## AWS Deployment Options

### 1. AWS ECS/Fargate
```bash
# Build Docker image
docker build -t ltr-cloud-native:latest .

# Tag for ECR
docker tag ltr-cloud-native:latest <account-id>.dkr.ecr.<region>.amazonaws.com/ltr:latest

# Push to ECR
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/ltr:latest

# Deploy to ECS (use task definition with environment variables)
```

### 2. AWS EKS (Kubernetes)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ltr-deployment
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: ltr
        image: <account-id>.dkr.ecr.<region>.amazonaws.com/ltr:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: ltr-secrets
              key: db-url
```

### 3. AWS Elastic Beanstalk
```bash
# Package as JAR
mvn clean package

# Deploy to Elastic Beanstalk
eb init -p "Corretto 11" ltr-app
eb create ltr-env
eb deploy
```

## Database Setup

### AWS RDS PostgreSQL
```sql
CREATE DATABASE ltrdb;
CREATE USER ltr_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE ltrdb TO ltr_user;

-- Run schema creation scripts
\i schema.sql
```

## Health Checks

### Endpoint
- URL: `http://localhost:8080/ltr/health`
- Format: JSON
- Status Codes: 200 (UP), 503 (DOWN)

### AWS ELB/ALB Configuration
- Health Check Path: `/ltr/health`
- Health Check Interval: 30 seconds
- Healthy Threshold: 2
- Unhealthy Threshold: 3
- Timeout: 5 seconds

## Monitoring

### CloudWatch Logs
- Log Group: `/aws/ecs/ltr` or `/aws/eks/ltr`
- Log Stream: Container instance ID
- Format: Structured JSON logs

### CloudWatch Metrics
- Enabled via Spring Boot Actuator
- Metrics: JVM, HTTP requests, database connections
- Namespace: `LandTitleRegistry`

## Security

### AWS Secrets Manager Integration
```java
// Database credentials from Secrets Manager
String secretName = "ltr/database";
// Automatically retrieved and injected
```

### IAM Roles
- ECS Task Role: Access to RDS, Secrets Manager, S3
- Required Policies:
  - `AmazonRDSFullAccess` (or custom policy)
  - `SecretsManagerReadWrite`
  - `AmazonS3ReadOnlyAccess`

## Performance Tuning

### JVM Options
```bash
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### HikariCP Settings
- Maximum Pool Size: 10 (adjust based on RDS instance)
- Connection Timeout: 30 seconds
- Idle Timeout: 10 minutes
- Max Lifetime: 30 minutes

## Troubleshooting

### Connection Issues
```bash
# Check database connectivity
curl http://localhost:8080/ltr/health

# View logs
docker logs <container-id>
```

### Performance Issues
```bash
# Check thread pool metrics
curl http://localhost:8080/ltr/actuator/metrics/hikaricp.connections.active
```

## Migration Checklist

- [x] Remove WebSphere dependencies
- [x] Replace EJB with Spring services
- [x] Convert WAR to executable JAR
- [x] Implement HikariCP connection pooling
- [x] Replace java.util.Date with java.time.Instant
- [x] Add async operations
- [x] Configure environment variables
- [x] Create Dockerfile
- [x] Add health check endpoint
- [x] Configure CloudWatch logging
- [x] Document deployment procedures

## Support

For issues or questions, contact the development team.
