# Land Title Registry - Cloud-Native Microservice

## Overview

This is a cloud-native Land Title Registry application transformed from IBM WebSphere EJB 2.x to Spring Boot microservice architecture, optimized for AWS deployment.

## Technology Stack

### Before (Legacy)
- IBM WebSphere Application Server 9.x
- EJB 2.x (Stateless Session Beans)
- WebSphere UOW Manager (proprietary transactions)
- WebSphere JNDI DataSource
- JAAS authentication
- WAR packaging
- RMI-IIOP remote interfaces

### After (Cloud-Native)
- Spring Boot 2.7.x
- Spring Data JPA
- Spring Security
- HikariCP connection pooling
- AWS Secrets Manager integration
- RESTful HTTP/JSON APIs
- Executable JAR with embedded Tomcat
- Docker containerization

## Key Features

- ✅ **Cloud-Ready**: Designed for AWS ECS, EKS, and Fargate
- ✅ **Stateless**: Horizontal scaling friendly
- ✅ **Secure**: AWS Secrets Manager for credentials
- ✅ **Observable**: Structured JSON logging for CloudWatch
- ✅ **Resilient**: Connection pooling with timeouts and retries
- ✅ **Standards-Based**: Uses java.time API (UTC timestamps)
- ✅ **Containerized**: Docker support with multi-stage builds

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     AWS Cloud                                │
│                                                              │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │     ALB      │─────▶│   ECS/EKS    │                    │
│  │ (Port 443)   │      │  (Fargate)   │                    │
│  └──────────────┘      └──────┬───────┘                    │
│                               │                              │
│                               ▼                              │
│                    ┌──────────────────┐                     │
│                    │  Spring Boot App │                     │
│                    │  (Port 8080)     │                     │
│                    └────────┬─────────┘                     │
│                             │                                │
│              ┌──────────────┼──────────────┐                │
│              ▼              ▼              ▼                │
│      ┌──────────┐   ┌──────────┐   ┌──────────┐           │
│      │   RDS    │   │ Secrets  │   │CloudWatch│           │
│      │PostgreSQL│   │ Manager  │   │   Logs   │           │
│      └──────────┘   └──────────┘   └──────────┘           │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites
- Java 11+
- Maven 3.6+
- Docker (optional)
- AWS CLI (for deployment)

### Local Development

1. **Set environment variables**:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=landtitle
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export AWS_REGION=us-east-1
```

2. **Build the application**:
```bash
mvn clean package
```

3. **Run the application**:
```bash
java -jar target/ltr-ejb-1.0.0.jar
```

4. **Access the API**:
```bash
curl http://localhost:8080/health
```

### Docker Build

```bash
docker build -t land-title-registry:latest .
docker run -p 8080:8080 \
  -e DB_HOST=your-db-host \
  -e DB_SECRET_NAME=ltr/db/credentials \
  -e AWS_REGION=us-east-1 \
  land-title-registry:latest
```

## API Endpoints

### Health Checks
- `GET /health` - Basic liveness check
- `GET /health/ready` - Readiness check with DB connectivity
- `GET /health/detailed` - Detailed health status

### Title Management
- `POST /api/v1/titles` - Register new title
- `GET /api/v1/titles/{titleNumber}` - Get title by number
- `GET /api/v1/titles/parcel/{parcelId}` - Get title by parcel ID
- `GET /api/v1/titles/owner/{ownerNationalId}` - Get titles by owner
- `GET /api/v1/titles/status/{status}` - Get titles by status
- `GET /api/v1/titles/search?keyword={keyword}` - Search titles
- `PUT /api/v1/titles/{titleNumber}` - Update title
- `PATCH /api/v1/titles/{titleNumber}/status` - Update title status

### Transfer Management
- `POST /api/v1/titles/transfers` - Initiate transfer
- `POST /api/v1/titles/transfers/{id}/approve` - Approve transfer
- `POST /api/v1/titles/transfers/{id}/reject` - Reject transfer
- `GET /api/v1/titles/{titleNumber}/transfers` - Get transfer history
- `GET /api/v1/titles/transfers/pending` - Get pending transfers

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database hostname | localhost |
| `DB_PORT` | Database port | 5432 |
| `DB_NAME` | Database name | landtitle |
| `DB_SECRET_NAME` | AWS Secrets Manager secret name | ltr/db/credentials |
| `AWS_REGION` | AWS region | us-east-1 |
| `DB_POOL_MAX_SIZE` | Max connection pool size | 20 |
| `DB_POOL_MIN_IDLE` | Min idle connections | 5 |
| `PORT` | Application port | 8080 |
| `LOG_FORMAT` | Log format (CONSOLE or CONSOLE_JSON) | CONSOLE |

### Database Schema

The application requires PostgreSQL 12+ with the following tables:
- `LAND_TITLE` - Main title registry
- `TITLE_TRANSFER_HISTORY` - Transfer records

See `schema.sql` for complete DDL.

## Cloud Deployment

See [AWS_DEPLOYMENT.md](AWS_DEPLOYMENT.md) for detailed AWS deployment instructions.

## Monitoring

### Logs
- Structured JSON logging for CloudWatch
- Correlation IDs for request tracing
- Log levels: DEBUG, INFO, WARN, ERROR

### Metrics
- Connection pool statistics
- Request latency
- Error rates
- Database query performance

### Health Checks
- Liveness probe: `/health`
- Readiness probe: `/health/ready`
- Includes database connectivity check

## Security

- Spring Security with role-based access control
- AWS Secrets Manager for credential management
- Stateless JWT-ready authentication
- HTTPS/TLS termination at ALB
- Security groups for network isolation

## Performance

- HikariCP connection pooling (20 max connections)
- Prepared statement caching
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max connection lifetime: 30 minutes

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn clean test jacoco:report
```

## Troubleshooting

### Database Connection Issues
1. Check security group rules
2. Verify Secrets Manager permissions
3. Check RDS endpoint and port
4. Review CloudWatch logs

### Health Check Failures
1. Verify database connectivity
2. Check connection pool exhaustion
3. Review application logs

## Migration Notes

This application was migrated from WebSphere EJB 2.x to Spring Boot. Key changes:

1. **EJB → Spring Service**: `@Stateless` → `@Service`
2. **CMT → Spring Transactions**: EJB CMT → `@Transactional`
3. **JNDI → HikariCP**: WAS DataSource → HikariCP pool
4. **JAAS → Spring Security**: SessionContext → SecurityContextHolder
5. **Date → Instant**: java.util.Date → java.time.Instant (UTC)
6. **WAR → JAR**: External app server → Embedded Tomcat
7. **RMI → REST**: EJB Remote → HTTP/JSON APIs

## License

Copyright © 2024 Trianz. All rights reserved.

## Support

For issues or questions, contact the development team.
