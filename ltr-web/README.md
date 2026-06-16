# Land Title Registry - Web Module (ltr-web)

## Cloud-Native Migration Summary

This module has been migrated from IBM WebSphere Application Server to a cloud-native Spring Boot application ready for AWS deployment.

### Key Changes

#### 1. **Packaging Migration** (Blockers #27, #28)
- **Before**: WAR packaging requiring external application server (WebSphere)
- **After**: Executable JAR with embedded Tomcat
- **Benefit**: Simplified deployment, smaller container images, faster startup

#### 2. **EJB to Spring Boot Migration** (Blocker #6)
- **Before**: `@EJB` injection dependent on WebSphere EJB container
- **After**: Spring `@Autowired` dependency injection
- **Files Modified**: `TitleRegistryServlet.java`
- **Benefit**: Framework-agnostic, cloud-native dependency injection

#### 3. **Session Management** (Blocker #25)
- **Before**: WebSphere distributed session management (stateful middleware clustering)
- **After**: Amazon ElastiCache (Redis) for distributed session storage
- **Dependencies Added**:
  - `spring-boot-starter-data-redis`
  - `spring-session-data-redis`
- **Benefit**: Stateless application architecture, horizontal scaling support

#### 4. **Time/Date Standardization** (Blocker #19)
- **Before**: `java.util.Date` with local timezone dependencies
- **After**: `java.time.Instant` with UTC standardization
- **Files Modified**: `HealthCheckServlet.java`
- **Benefit**: Consistent timestamps across distributed cloud services

### AWS Deployment Configuration

#### Environment Variables

```bash
# Server Configuration
SERVER_PORT=8080

# Amazon ElastiCache (Redis) for Session Management
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
REDIS_SSL=true

# AWS Secrets Manager (for database credentials)
DB_SECRET_NAME=ltr/db/credentials
AWS_REGION=us-east-1

# Security
SECURITY_USER=admin
SECURITY_PASSWORD=changeme
```

#### Running Locally

```bash
# Build the application
mvn clean package

# Run the executable JAR
java -jar target/ltr-web-1.0.0.jar
```

#### AWS ECS/Fargate Deployment

```bash
# Build Docker image (Dockerfile not included in this transformation)
docker build -t ltr-web:1.0.0 .

# Push to Amazon ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag ltr-web:1.0.0 <account-id>.dkr.ecr.us-east-1.amazonaws.com/ltr-web:1.0.0
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ltr-web:1.0.0

# Deploy to ECS/Fargate using task definition
```

### Dependencies

#### Added for Cloud-Native Support
- `spring-boot-starter-web` - Embedded Tomcat, Spring MVC
- `spring-boot-starter-security` - Authentication/authorization
- `spring-boot-starter-data-redis` - Redis connectivity
- `spring-session-data-redis` - Distributed session management
- `jackson-datatype-jsr310` - Java 8 date/time JSON serialization

#### Removed
- `com.ibm.websphere.appserver:was_public` - WebSphere-specific APIs
- WAR packaging dependencies

### Health Check Endpoint

The application exposes a health check endpoint at `/health` for AWS load balancer health checks:

```bash
curl http://localhost:8080/health
```

Response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:45",
  "application": "Land Title Registry",
  "version": "1.0.0",
  "checks": {
    "database": { "status": "UP" },
    "ejbContainer": { "status": "UP" }
  }
}
```

### Next Steps

1. **Configure Amazon ElastiCache**: Create a Redis cluster in AWS
2. **Set up AWS Secrets Manager**: Store database credentials securely
3. **Configure Application Load Balancer**: Point to `/health` endpoint
4. **Set up Auto Scaling**: Configure ECS service auto-scaling policies
5. **Enable CloudWatch Logs**: Configure log aggregation for monitoring

### Architecture Benefits

- ✅ **Stateless**: No server affinity required, supports horizontal scaling
- ✅ **Cloud-Native**: Follows 12-factor app principles
- ✅ **Containerizable**: Ready for Docker, ECS, EKS, or Fargate
- ✅ **Portable**: Can run on any cloud provider (AWS, Azure, GCP)
- ✅ **Observable**: Health checks, metrics, and structured logging
- ✅ **Secure**: Externalized secrets, no hardcoded credentials

### Migration Notes

- The servlet-based architecture is maintained for backward compatibility
- Consider migrating to Spring REST Controllers (`@RestController`) for better cloud-native patterns
- Session data is now stored in Redis instead of in-memory or WebSphere clustering
- All timestamps use UTC timezone for consistency across distributed services
