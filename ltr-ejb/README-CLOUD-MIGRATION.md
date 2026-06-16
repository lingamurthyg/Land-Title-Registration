# Land Title Registry - Cloud-Native Migration

## Overview
This application has been migrated from IBM WebSphere Application Server (WAS) to a cloud-native Spring Boot architecture optimized for AWS deployment.

## Cloud Readiness Fixes Applied

### 1. Hard-coded Database Credentials (cr-java-0069) ✅
**Issue**: Database credentials were hard-coded in source code.
**Fix**: Integrated AWS Secrets Manager for secure credential management.
- Credentials retrieved dynamically from AWS Secrets Manager
- Environment variable configuration for database connection
- Automatic credential rotation support

### 2. EJB 2.x Usage (cr-java-0085) ✅
**Issue**: Application used EJB 2.x with heavy container dependencies.
**Fix**: Migrated to Spring Boot microservices architecture.
- Replaced `@Stateless` EJB with Spring `@Service`
- Replaced `@TransactionAttribute` with Spring `@Transactional`
- Replaced `@RolesAllowed` with Spring Security `@Secured`
- Removed dependency on EJB container

### 3. Clock/Time Dependencies (cr-java-0111) ✅
**Issue**: Application used `java.util.Date` and local timezone dependencies.
**Fix**: Migrated to `java.time.Instant` with UTC standardization.
- All timestamps use `java.time.Instant` (UTC)
- Eliminated timezone inconsistencies
- Compatible with distributed cloud deployments

### 4. Heavy Coupling to Stateful Middleware (cr-java-0116) ✅
**Issue**: Application depended on WebSphere-specific clustering and session management.
**Fix**: Replaced with Amazon ElastiCache (Redis) for distributed sessions.
- Removed WebSphere UOWManager dependencies
- Removed WebSphere DataSource dependencies
- Implemented Redis-based session storage
- Stateless design for horizontal scaling

### 5. WAR Packaging (cr-java-0107) ✅
**Issue**: Application packaged as WAR requiring external application server.
**Fix**: Converted to executable JAR with embedded Tomcat.
- Changed packaging from `war` to `jar`
- Embedded servlet container (Tomcat)
- Self-contained executable for containerization

## Architecture Changes

### Before (WebSphere)
```
┌─────────────────────────────────────┐
│   IBM WebSphere Application Server  │
│  ┌──────────────────────────────┐   │
│  │  EJB Container               │   │
│  │  - Stateless Session Beans   │   │
│  │  - Container-Managed Trans.  │   │
│  │  - JNDI DataSource           │   │
│  │  - WAS Security (JAAS)       │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### After (Spring Boot on AWS)
```
┌─────────────────────────────────────┐
│   Spring Boot Application (ECS/EKS) │
│  ┌──────────────────────────────┐   │
│  │  Spring Services             │   │
│  │  - @Service components       │   │
│  │  - @Transactional            │   │
│  │  - HikariCP DataSource       │   │
│  │  - Spring Security           │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
         │              │
         ▼              ▼
┌──────────────┐  ┌──────────────┐
│  AWS RDS     │  │ ElastiCache  │
│  PostgreSQL  │  │  (Redis)     │
└──────────────┘  └──────────────┘
         │
         ▼
┌──────────────────┐
│ AWS Secrets Mgr  │
└──────────────────┘
```

## AWS Services Integration

### 1. AWS RDS (PostgreSQL)
- HikariCP connection pooling optimized for RDS
- Automatic failover support
- Read replicas for scaling

### 2. AWS Secrets Manager
- Secure credential storage
- Automatic credential rotation
- No credentials in source code or environment variables

### 3. Amazon ElastiCache (Redis)
- Distributed session management
- Horizontal scaling support
- Session persistence across container restarts

### 4. AWS ECS/EKS Deployment
- Containerized deployment
- Auto-scaling based on load
- Health checks and monitoring

## Environment Variables

### Required Configuration
```bash
# Database Configuration
DB_HOST=your-rds-endpoint.region.rds.amazonaws.com
DB_PORT=5432
DB_NAME=landtitle
DB_SECRET_NAME=ltr/db/credentials

# AWS Configuration
AWS_REGION=us-east-1

# Redis Configuration (ElastiCache)
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379

# Application Configuration
SERVER_PORT=8080
```

### AWS Secrets Manager Secret Format
```json
{
  "username": "ltr_app_user",
  "password": "secure_password_from_secrets_manager"
}
```

## Building and Running

### Local Development
```bash
# Build
mvn clean package

# Run
java -jar target/ltr-ejb-1.0.0.jar

# Or with Maven
mvn spring-boot:run
```

### Docker Build
```bash
docker build -t land-title-registry:latest .
docker run -p 8080:8080 \
  -e DB_HOST=localhost \
  -e DB_SECRET_NAME=ltr/db/credentials \
  -e REDIS_HOST=localhost \
  land-title-registry:latest
```

### AWS ECS Deployment
1. Push Docker image to Amazon ECR
2. Create ECS task definition with environment variables
3. Configure service with load balancer
4. Enable auto-scaling

## Health Checks

### Endpoints
- `GET /actuator/health` - Application health status
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics

### Health Check Response
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "6.2.6"
      }
    }
  }
}
```

## Security

### Authentication
- Spring Security with role-based access control
- Basic authentication (replace with JWT/OAuth2 in production)
- Integration with AWS Cognito recommended for production

### Roles
- `ROLE_REGISTRY_OFFICER` - Basic registry operations
- `ROLE_REGISTRY_SUPERVISOR` - Approve/reject transfers
- `ROLE_REGISTRY_ADMIN` - Full administrative access

## Monitoring and Logging

### CloudWatch Integration
- Structured JSON logging for CloudWatch Logs
- Application metrics via Spring Boot Actuator
- Custom metrics for business operations

### Log Format
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.trianz.ltr.ejb.LandTitleRegistryBean",
  "message": "Title registered: LTR-2024-REG-000123"
}
```

## Migration Checklist

- [x] Replace EJB with Spring Boot services
- [x] Replace WAS DataSource with HikariCP
- [x] Replace WAS transactions with Spring @Transactional
- [x] Replace WAS security with Spring Security
- [x] Replace java.util.Date with java.time.Instant
- [x] Integrate AWS Secrets Manager
- [x] Implement Redis session management
- [x] Convert WAR to executable JAR
- [x] Add health check endpoints
- [x] Configure UTC timezone handling
- [x] Remove WebSphere dependencies

## Next Steps

1. **Database Migration**: Migrate data from existing database to AWS RDS
2. **Load Testing**: Validate performance under load
3. **Security Hardening**: Implement JWT/OAuth2 authentication
4. **CI/CD Pipeline**: Set up automated deployment pipeline
5. **Monitoring**: Configure CloudWatch dashboards and alarms
6. **Backup Strategy**: Implement RDS automated backups and snapshots

## Support

For issues or questions, contact the development team.
