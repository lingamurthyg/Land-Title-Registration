# Land Title Registry - Cloud-Native Migration

## Overview
This application has been migrated from IBM WebSphere Application Server (EJB 2.x) to a cloud-native Spring Boot microservice architecture, optimized for deployment on AWS.

## Cloud Readiness Fixes Applied

### 1. Hard-coded Database Credentials (CRITICAL)
**Issue**: Database credentials were hard-coded in source code  
**Fix**: Integrated AWS Secrets Manager for secure credential management
- Credentials retrieved dynamically at runtime
- Supports automatic rotation without redeployment
- No credentials stored in source code or configuration files

### 2. EJB 2.x Usage (HIGH)
**Issue**: Heavy dependency on EJB 2.x container  
**Fix**: Migrated to Spring Boot microservices
- Replaced `@Stateless` EJB with `@Service` Spring components
- Replaced `@Local`/`@Remote` interfaces with standard Java interfaces
- Replaced EJB CMT with Spring `@Transactional`
- Replaced `@RolesAllowed` with Spring Security `@PreAuthorize`
- Added REST API layer to replace RMI-IIOP

### 3. Clock/Time Dependencies (HIGH)
**Issue**: Usage of `java.util.Date` and local timezone dependencies  
**Fix**: Migrated to `java.time` API with UTC standardization
- Replaced `new Date()` with `Date.from(Instant.now())`
- Replaced `new Timestamp(System.currentTimeMillis())` with `Timestamp.from(Instant.now())`
- All timestamps stored and processed in UTC
- Eliminated timezone inconsistencies in distributed deployments

### 4. Heavy Coupling to Stateful Middleware (HIGH)
**Issue**: WebSphere-specific APIs and JNDI dependencies  
**Fix**: Replaced with cloud-native alternatives
- Removed `com.ibm.websphere.uow.UOWSynchronizationRegistry`
- Removed `com.ibm.wsspi.uow.UOWManager`
- Removed `com.ibm.websphere.rsadapter.WSDataSource`
- Replaced JNDI lookups with Spring dependency injection
- Integrated Amazon ElastiCache (Redis) for session management

### 5. WAR Packaging (LOW)
**Issue**: WAR packaging requires external application server  
**Fix**: Converted to executable JAR with embedded Tomcat
- Changed packaging from `ejb` to `jar`
- Added Spring Boot Maven plugin
- Self-contained executable for containerization

## AWS Integration

### AWS Services Used
1. **AWS Secrets Manager**: Database credential management
2. **Amazon RDS/Aurora**: PostgreSQL database
3. **Amazon ElastiCache (Redis)**: Session state management
4. **AWS Application Load Balancer**: Traffic distribution
5. **Amazon ECS/EKS/Fargate**: Container orchestration

### Environment Variables
```bash
# AWS Configuration
AWS_REGION=us-east-1
DB_SECRET_NAME=ltr/database/credentials

# Redis Configuration (ElastiCache)
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Application Configuration
PORT=8080
ADMIN_USERNAME=admin
ADMIN_PASSWORD=changeme
```

### AWS Secrets Manager Secret Format
```json
{
  "username": "ltr_user",
  "password": "secure_password",
  "host": "your-rds-endpoint.rds.amazonaws.com",
  "port": "5432",
  "dbname": "landtitleregistry"
}
```

## Architecture Changes

### Before (WebSphere EJB)
```
┌─────────────────────────────────────┐
│   IBM WebSphere Application Server  │
│  ┌──────────────────────────────┐   │
│  │  EJB 2.x Container           │   │
│  │  - Stateless Session Beans   │   │
│  │  - Container-Managed Trans.  │   │
│  │  - JNDI DataSource Lookup    │   │
│  │  - RMI-IIOP Remote Access    │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### After (Spring Boot on AWS)
```
┌─────────────────────────────────────┐
│   Spring Boot Microservice          │
│  ┌──────────────────────────────┐   │
│  │  Spring Framework            │   │
│  │  - @Service Components       │   │
│  │  - @Transactional            │   │
│  │  - HikariCP Connection Pool  │   │
│  │  - REST APIs (HTTP/JSON)     │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │  AWS Integration             │   │
│  │  - Secrets Manager           │   │
│  │  - RDS/Aurora                │   │
│  │  - ElastiCache (Redis)       │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

## Deployment

### Build
```bash
mvn clean package
```

### Run Locally
```bash
export AWS_REGION=us-east-1
export DB_SECRET_NAME=ltr/database/credentials
export REDIS_HOST=localhost
export REDIS_PORT=6379

java -jar target/ltr-ejb-1.0.0.jar
```

### Docker Build
```bash
docker build -t land-title-registry:latest .
```

### Deploy to AWS ECS/EKS
```bash
# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag land-title-registry:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest

# Deploy to ECS/EKS using your deployment pipeline
```

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Title Management
```
POST   /api/v1/titles                    - Register new title
GET    /api/v1/titles/{titleNumber}      - Get title by number
GET    /api/v1/titles/parcel/{parcelId}  - Get title by parcel ID
GET    /api/v1/titles/owner/{nationalId} - Get titles by owner
GET    /api/v1/titles/status/{status}    - Get titles by status
GET    /api/v1/titles/search?keyword=    - Search titles
PUT    /api/v1/titles                     - Update title
PUT    /api/v1/titles/{titleNumber}/status - Update title status
```

### Transfer Management
```
POST   /api/v1/titles/transfers                      - Initiate transfer
PUT    /api/v1/titles/transfers/{id}/approve         - Approve transfer
PUT    /api/v1/titles/transfers/{id}/reject          - Reject transfer
GET    /api/v1/titles/{titleNumber}/transfers        - Get transfer history
GET    /api/v1/titles/transfers/pending              - Get pending transfers
```

## Security

### Authentication
- Spring Security with role-based access control
- Roles: `REGISTRY_OFFICER`, `REGISTRY_SUPERVISOR`, `REGISTRY_ADMIN`
- JWT or OAuth2 integration recommended for production

### Authorization
- Method-level security with `@PreAuthorize`
- Sensitive operations restricted to supervisor/admin roles

## Monitoring

### Health Checks
- Spring Actuator endpoints for AWS load balancer health checks
- Database connectivity check
- Redis connectivity check

### Metrics
- JVM metrics
- Database connection pool metrics
- HTTP request metrics
- Custom business metrics

### Logging
- Structured logging for CloudWatch
- UTC timestamps for all log entries
- Request/response logging for audit trail

## Migration Summary

| Component | Before | After |
|-----------|--------|-------|
| Framework | EJB 2.x | Spring Boot 2.7 |
| Transaction Management | CMT | Spring @Transactional |
| Security | JAAS/WAS Roles | Spring Security |
| DataSource | WAS JNDI | HikariCP |
| Credentials | Hard-coded | AWS Secrets Manager |
| Session Management | WAS Clustering | Redis (ElastiCache) |
| Communication | RMI-IIOP | REST/HTTP |
| Packaging | WAR | Executable JAR |
| Date/Time | java.util.Date | java.time.Instant (UTC) |
| Deployment | WebSphere | AWS ECS/EKS/Fargate |

## Benefits

1. **Cloud-Native**: Fully compatible with AWS managed services
2. **Scalable**: Stateless horizontal scaling with Redis sessions
3. **Secure**: Credentials managed by AWS Secrets Manager
4. **Portable**: Standard Spring Boot, deployable anywhere
5. **Observable**: Health checks, metrics, structured logging
6. **Maintainable**: Modern framework, active community support
7. **Cost-Effective**: No expensive application server licenses

## Next Steps

1. Set up AWS infrastructure (RDS, ElastiCache, ECS/EKS)
2. Configure AWS Secrets Manager with database credentials
3. Set up CI/CD pipeline for automated deployments
4. Configure CloudWatch for monitoring and alerting
5. Implement API Gateway for external access
6. Add comprehensive integration tests
7. Performance testing and optimization
