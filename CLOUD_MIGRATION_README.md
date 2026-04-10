# Land Title Registry - Cloud-Native Migration

## Overview

This application has been successfully migrated from IBM WebSphere Application Server (WAS) to a cloud-native architecture compatible with AWS, Azure, and GCP.

## Cloud Readiness Fixes Applied

### 1. **Hard-coded Database Credentials (CRITICAL)** ✅
- **Issue**: Database credentials embedded in source code
- **Fix**: Migrated to AWS Secrets Manager for secure credential storage
- **Implementation**: `CloudDataSourceUtil.java` retrieves credentials from AWS Secrets Manager
- **Environment Variables**: 
  - `DB_SECRET_NAME`: AWS Secrets Manager secret name
  - `DB_HOST`, `DB_PORT`, `DB_NAME`: Database connection parameters

### 2. **EJB 2.x Usage (HIGH)** ✅
- **Issue**: Heavy dependency on EJB 2.x container
- **Fix**: Replaced with Spring Boot microservices architecture
- **Changes**:
  - `@Stateless` → `@Service`
  - `@TransactionAttribute` → `@Transactional`
  - `@EJB` → `@Autowired`
  - Removed `@Local` and `@Remote` annotations

### 3. **Clock/Time Dependencies (HIGH)** ✅
- **Issue**: Using `java.util.Date` with timezone inconsistencies
- **Fix**: Migrated to `java.time.Instant` for UTC timestamps
- **Files Updated**:
  - `LandTitle.java` - Uses `Instant` for all timestamps
  - `TitleTransfer.java` - Uses `Instant` for all timestamps
  - `TitleNumberGenerator.java` - Uses `Instant` with UTC zone
  - All DAO classes - Convert between `Instant` and `Timestamp`

### 4. **Heavy Coupling to Stateful Middleware (HIGH)** ✅
- **Issue**: Tight integration with WebSphere clustering and state replication
- **Fix**: Replaced with stateless Spring services
- **Changes**:
  - Removed `WASDataSourceUtil` dependencies
  - Removed `WASTransactionUtil` dependencies
  - Created `CloudDataSourceUtil` with HikariCP
  - Created `CloudTransactionUtil` with Spring transactions
  - Deprecated old WAS utilities for backward compatibility

### 5. **Synchronous Blocking Operations (MEDIUM)** ✅
- **Issue**: Synchronous JDBC operations reducing throughput
- **Fix**: Implemented HikariCP connection pooling for better resource utilization
- **Configuration**: 
  - Max pool size: 20 connections (configurable via `DB_POOL_MAX_SIZE`)
  - Min idle: 5 connections (configurable via `DB_POOL_MIN_IDLE`)
  - Connection timeout: 30 seconds
  - Leak detection enabled

### 6. **WAR Packaging (MEDIUM)** ✅
- **Issue**: WAR packaging requires external application server
- **Fix**: Converted to executable JAR with embedded Tomcat
- **Build**: `mvn clean package` produces `ltr-web.jar`
- **Run**: `java -jar ltr-web.jar`

## Architecture Changes

### Before (WebSphere)
```
┌─────────────────────────────────────┐
│   IBM WebSphere Application Server  │
│  ┌──────────────┐  ┌──────────────┐ │
│  │  EJB 2.x     │  │  Servlets    │ │
│  │  Container   │  │  (WAR)       │ │
│  └──────────────┘  └──────────────┘ │
│  ┌──────────────────────────────────┤
│  │  WAS DataSource (JNDI)           │
│  │  WAS Transaction Manager         │
│  │  WAS Security (JAAS)             │
│  └──────────────────────────────────┘
└─────────────────────────────────────┘
```

### After (Cloud-Native)
```
┌─────────────────────────────────────┐
│   Spring Boot (Embedded Tomcat)     │
│  ┌──────────────┐  ┌──────────────┐ │
│  │  @Service    │  │  @Controller │ │
│  │  Components  │  │  REST APIs   │ │
│  └──────────────┘  └──────────────┘ │
│  ┌──────────────────────────────────┤
│  │  HikariCP Connection Pool        │
│  │  Spring @Transactional           │
│  │  Spring Security                 │
│  │  AWS Secrets Manager             │
│  └──────────────────────────────────┘
└─────────────────────────────────────┘
```

## Environment Variables

### Required for AWS Deployment

```bash
# Database Configuration
DB_HOST=your-rds-endpoint.amazonaws.com
DB_PORT=5432
DB_NAME=landtitle
DB_SECRET_NAME=ltr/db/credentials

# AWS Configuration
AWS_REGION=us-east-1

# Connection Pool Configuration (Optional)
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT_MS=30000

# Application Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=aws
ENVIRONMENT=production
```

### AWS Secrets Manager Secret Format

```json
{
  "username": "ltr_app_user",
  "password": "your-secure-password"
}
```

## Deployment Options

### 1. AWS ECS (Fargate)
```bash
# Build Docker image
docker build -t ltr-web:latest .

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag ltr-web:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/ltr-web:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ltr-web:latest

# Deploy to ECS
aws ecs update-service --cluster ltr-cluster --service ltr-web --force-new-deployment
```

### 2. AWS EKS (Kubernetes)
```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

### 3. AWS Elastic Beanstalk
```bash
# Deploy JAR file
eb init -p "Corretto 11" ltr-web
eb create ltr-web-env
eb deploy
```

## Health Checks

### Liveness Probe
```
GET /health
```

### Readiness Probe
```
GET /actuator/health/readiness
```

### Metrics
```
GET /actuator/metrics
GET /actuator/prometheus
```

## Monitoring

### CloudWatch Logs
- Application logs are sent to CloudWatch Logs in JSON format
- Log group: `/aws/ecs/ltr-web` or `/aws/elasticbeanstalk/ltr-web`

### CloudWatch Metrics
- Custom metrics exported via Micrometer
- Namespace: `LandTitleRegistry`
- Metrics: Connection pool stats, transaction counts, API latency

### X-Ray Tracing
- Distributed tracing enabled for AWS X-Ray
- Trace API calls, database queries, and external service calls

## Security

### AWS IAM Roles
Required IAM permissions:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:ltr/db/credentials-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:DescribeDBClusters"
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
      "Resource": "arn:aws:logs:*:*:log-group:/aws/ecs/ltr-web:*"
    }
  ]
}
```

## Performance Optimizations

1. **Connection Pooling**: HikariCP with optimized settings
2. **Prepared Statement Caching**: Enabled for PostgreSQL
3. **Batch Operations**: Hibernate batch size = 20
4. **Async Logging**: Non-blocking log appenders
5. **Compression**: Gzip compression for HTTP responses

## Migration Checklist

- [x] Replace EJB with Spring services
- [x] Replace WAS DataSource with HikariCP
- [x] Replace WAS transactions with Spring @Transactional
- [x] Migrate to java.time API (UTC timestamps)
- [x] Implement AWS Secrets Manager integration
- [x] Convert WAR to executable JAR
- [x] Add structured JSON logging
- [x] Add health check endpoints
- [x] Add CloudWatch metrics integration
- [x] Remove WebSphere dependencies
- [x] Update Maven build configuration
- [x] Create cloud deployment profiles

## Testing

### Local Development
```bash
# Start PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_DB=landtitle -e POSTGRES_PASSWORD=postgres postgres:14

# Run application
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Test health endpoint
curl http://localhost:8080/health
```

### Integration Tests
```bash
mvn verify -Pintegration-tests
```

## Support

For issues or questions, contact the cloud migration team.

## License

Copyright © 2024 Trianz. All rights reserved.
