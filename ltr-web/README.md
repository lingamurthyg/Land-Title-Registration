# Land Title Registry - Cloud-Native Application

## Overview

The Land Title Registry application has been successfully migrated from IBM WebSphere Application Server (WAS) with EJB 2.x to a cloud-native Spring Boot application ready for AWS deployment.

## Migration Summary

### Architecture Transformation

| Component | Before (WebSphere) | After (Cloud-Native) |
|-----------|-------------------|---------------------|
| **Packaging** | WAR (Web Archive) | Executable JAR with embedded Tomcat |
| **Application Server** | IBM WebSphere 9.x | Embedded Tomcat (Spring Boot) |
| **EJB Layer** | EJB 2.x Session Beans | Spring Services with @Service |
| **Dependency Injection** | @EJB annotations | Spring @Autowired |
| **DataSource** | WAS JNDI Lookup | HikariCP Connection Pool |
| **Database** | Direct JDBC | Spring Data JPA with HikariCP |
| **Security** | WAS JAAS/LDAP | Spring Security |
| **Configuration** | Hard-coded values | Environment variables |
| **Credentials** | Hard-coded in code | AWS Secrets Manager |
| **Logging** | java.util.logging | SLF4J/Logback with JSON |
| **Time Handling** | java.util.Date | java.time.Instant (UTC) |
| **Health Checks** | Custom Servlet | Spring Boot Actuator |
| **Deployment** | Manual WAR deployment | Docker containers (ECS/EKS) |

### Cloud Readiness Issues Fixed

#### 1. Hard-coded Database Credentials (CRITICAL)
- **Before**: Credentials hard-coded in `WASTransactionUtil.java`
- **After**: Credentials loaded from AWS Secrets Manager
- **File**: `AwsSecretsManagerConfig.java`

#### 2. EJB 2.x Usage (HIGH)
- **Before**: EJB 2.x with `@EJB` injection
- **After**: Spring Services with `@Autowired` injection
- **Files**: 
  - `TitleRegistryServlet.java` → `TitleRegistryController.java`
  - `LandTitleRegistryBean.java` → `LandTitleRegistryService.java`

#### 3. Clock/Time Dependencies (HIGH)
- **Before**: `java.util.Date` with local timezone
- **After**: `java.time.Instant` with UTC standardization
- **Files**: 
  - `LandTitle.java` - Uses `Instant` instead of `Date`
  - `TitleTransfer.java` - Uses `Instant` instead of `Date`
  - `DatabaseHealthIndicator.java` - Uses `Instant` for timestamps

#### 4. Heavy Coupling to Stateful Middleware (HIGH)
- **Before**: WAS-specific DataSource and Transaction Manager
- **After**: Standard HikariCP and Spring Transaction Management
- **Files**:
  - `DataSourceConfig.java` - HikariCP configuration
  - Removed `WASDataSourceUtil.java` dependency
  - Removed `WASTransactionUtil.java` dependency

#### 5. WAR Packaging (MEDIUM)
- **Before**: WAR file requiring external application server
- **After**: Executable JAR with embedded Tomcat
- **File**: `pom.xml` - Changed packaging from `war` to `jar`

#### 6. Synchronous Blocking Operations (MEDIUM)
- **Before**: Synchronous JDBC operations
- **After**: HikariCP connection pooling with async support
- **Configuration**: `application.properties` - Async task execution

### New Cloud-Native Features

1. **AWS Secrets Manager Integration**
   - Secure credential management
   - Automatic secret rotation support
   - IAM role-based authentication

2. **HikariCP Connection Pooling**
   - High-performance connection pooling
   - Connection leak detection
   - Automatic connection validation
   - Configurable pool sizing

3. **Spring Boot Actuator**
   - Health checks for AWS ECS/EKS
   - Metrics for CloudWatch
   - Liveness and readiness probes

4. **Structured JSON Logging**
   - CloudWatch Logs Insights compatible
   - Correlation IDs for distributed tracing
   - UTC timestamps for consistency

5. **Containerization**
   - Docker support with multi-stage builds
   - Optimized for AWS ECS/EKS
   - Non-root user for security

6. **Environment-based Configuration**
   - 12-factor app principles
   - Externalized configuration
   - Cloud-native configuration management

## Project Structure

```
ltr-web/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/trianz/ltr/
│   │   │       ├── LandTitleRegistryApplication.java  # Spring Boot main class
│   │   │       ├── config/
│   │   │       │   ├── AwsSecretsManagerConfig.java   # AWS Secrets Manager
│   │   │       │   └── DataSourceConfig.java          # HikariCP configuration
│   │   │       ├── controller/
│   │   │       │   └── TitleRegistryController.java   # REST API (was Servlet)
│   │   │       ├── service/
│   │   │       │   └── LandTitleRegistryService.java  # Business logic (was EJB)
│   │   │       ├── model/
│   │   │       │   ├── LandTitle.java                 # JPA entity
│   │   │       │   └── TitleTransfer.java             # JPA entity
│   │   │       ├── exception/
│   │   │       │   └── LandTitleException.java        # Business exceptions
│   │   │       └── health/
│   │   │           └── DatabaseHealthIndicator.java   # Health checks
│   │   └── resources/
│   │       ├── application.properties                 # Configuration
│   │       └── logback-spring.xml                     # Logging configuration
│   └── test/
│       └── java/
├── Dockerfile                                         # Container image
├── AWS-DEPLOYMENT.md                                  # AWS deployment guide
├── pom.xml                                            # Maven configuration
└── README.md                                          # This file
```

## Building the Application

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher
- Docker (for containerization)

### Build Executable JAR

```bash
mvn clean package
```

The executable JAR will be created at `target/ltr-web.jar`

### Run Locally

```bash
java -jar target/ltr-web.jar \
  --DB_HOST=localhost \
  --DB_PORT=5432 \
  --DB_NAME=landtitle \
  --DB_USERNAME=postgres \
  --DB_PASSWORD=postgres
```

### Build Docker Image

```bash
docker build -t land-title-registry:latest .
```

### Run Docker Container

```bash
docker run -p 8080:8080 \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=landtitle \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  land-title-registry:latest
```

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DB_HOST` | Database host | Yes | localhost |
| `DB_PORT` | Database port | No | 5432 |
| `DB_NAME` | Database name | Yes | landtitle |
| `DB_USERNAME` | Database username | Yes* | postgres |
| `DB_PASSWORD` | Database password | Yes* | postgres |
| `AWS_REGION` | AWS region | No | us-east-1 |
| `AWS_SECRETS_ENABLED` | Enable AWS Secrets Manager | No | false |
| `DB_SECRET_ARN` | AWS Secrets Manager ARN | Yes** | - |
| `PORT` | Application port | No | 8080 |
| `LOG_LEVEL` | Logging level | No | INFO |

\* Required if `AWS_SECRETS_ENABLED=false`  
\** Required if `AWS_SECRETS_ENABLED=true`

### AWS Secrets Manager

When deploying to AWS, enable Secrets Manager for secure credential management:

```bash
# Create secret
aws secretsmanager create-secret \
  --name landtitle/db/credentials \
  --secret-string '{"username":"dbuser","password":"dbpass"}'

# Set environment variables
export AWS_SECRETS_ENABLED=true
export DB_SECRET_ARN=arn:aws:secretsmanager:region:account:secret:landtitle/db/credentials
```

## API Endpoints

### Title Operations

- `GET /api/titles/{titleNumber}` - Get title by number
- `GET /api/titles?owner={ownerId}` - Get titles by owner
- `GET /api/titles?status={status}` - Get titles by status
- `GET /api/titles?q={keyword}` - Search titles
- `POST /api/titles` - Register new title
- `PUT /api/titles/{titleNumber}` - Update title

### Transfer Operations

- `GET /api/transfers/pending` - Get pending transfers (supervisor/admin only)
- `GET /api/transfers?title={titleNumber}` - Get transfer history
- `POST /api/transfers` - Initiate transfer
- `POST /api/transfers/{id}/approve` - Approve transfer (supervisor/admin only)
- `POST /api/transfers/{id}/reject` - Reject transfer (supervisor/admin only)

### Health Checks

- `GET /actuator/health` - Overall health status
- `GET /actuator/health/liveness` - Liveness probe
- `GET /actuator/health/readiness` - Readiness probe
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics

## Security

### Authentication

The application uses Spring Security with role-based access control:

- `ROLE_REGISTRY_OFFICER` - Basic read/write access
- `ROLE_REGISTRY_SUPERVISOR` - Approve/reject transfers
- `ROLE_REGISTRY_ADMIN` - Full administrative access

### Default Credentials (Development Only)

- Username: `admin`
- Password: `admin`

**⚠️ WARNING**: Change default credentials before deploying to production!

### Production Security

For production deployment, integrate with:
- AWS Cognito for user authentication
- AWS IAM for service-to-service authentication
- AWS WAF for API protection

## AWS Deployment

See [AWS-DEPLOYMENT.md](AWS-DEPLOYMENT.md) for detailed deployment instructions.

### Quick Deploy to ECS

```bash
# Build and push image
docker build -t land-title-registry:latest .
docker tag land-title-registry:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/land-title-registry:latest

# Deploy to ECS
aws ecs update-service \
  --cluster land-title-cluster \
  --service land-title-service \
  --force-new-deployment
```

## Monitoring

### CloudWatch Logs

Application logs are sent to CloudWatch in JSON format:

```bash
aws logs tail /ecs/land-title-registry --follow
```

### CloudWatch Metrics

Custom metrics are published to CloudWatch:

- HTTP request metrics
- Database connection pool metrics
- JVM metrics
- Custom business metrics

### Health Monitoring

Configure ALB health checks:
- **Path**: `/actuator/health`
- **Port**: 8080
- **Interval**: 30 seconds
- **Timeout**: 5 seconds
- **Healthy threshold**: 2
- **Unhealthy threshold**: 3

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Test Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

### Test API Endpoint

```bash
curl -u admin:admin http://localhost:8080/api/titles/T-2024-001
```

## Migration Notes

### Breaking Changes

1. **API Changes**: Servlet URLs remain the same, but authentication mechanism changed
2. **Configuration**: All configuration now via environment variables
3. **Database**: Schema remains compatible, but timezone handling changed to UTC
4. **Security**: WAS JAAS roles replaced with Spring Security roles

### Compatibility

- **Database**: Compatible with existing schema
- **API**: REST API endpoints unchanged
- **Data**: Existing data compatible (dates converted to UTC)

### Known Issues

None at this time.

## Troubleshooting

### Application won't start

Check logs for errors:
```bash
docker logs <container-id>
```

### Database connection issues

Verify environment variables:
```bash
echo $DB_HOST
echo $DB_PORT
echo $DB_NAME
```

Test database connectivity:
```bash
psql -h $DB_HOST -p $DB_PORT -U $DB_USERNAME -d $DB_NAME
```

### AWS Secrets Manager issues

Verify IAM permissions:
```bash
aws secretsmanager get-secret-value --secret-id landtitle/db/credentials
```

## Support

For issues or questions:
- Cloud Migration Team: cloud-migration@example.com
- AWS Support: aws-support@example.com

## License

Copyright © 2024 Trianz. All rights reserved.
