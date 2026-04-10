# Land Title Registry - Cloud Native Application

## Overview
Cloud-native Land Title Registry System migrated from IBM WebSphere to AWS-ready architecture.

## Technology Stack
- **Java**: 11
- **Framework**: Spring Boot 2.7.18
- **Database**: PostgreSQL (AWS RDS)
- **Connection Pool**: HikariCP
- **Packaging**: Executable JAR
- **Container**: Docker
- **Cloud Platform**: AWS (ECS, EKS, Elastic Beanstalk)

## Quick Start

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Docker (for containerization)
- PostgreSQL database

### Build
```bash
mvn clean package
```

### Run Locally
```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/ltrdb
export DB_USERNAME=ltr_user
export DB_PASSWORD=changeme

# Run application
java -jar target/ltr-cloud-native.jar
```

### Run with Docker
```bash
# Build image
docker build -t ltr-cloud-native:latest .

# Run container
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ltrdb \
  -e DB_USERNAME=ltr_user \
  -e DB_PASSWORD=changeme \
  ltr-cloud-native:latest
```

## API Endpoints

### Health Check
```bash
GET http://localhost:8080/ltr/health
```

### Title Management
```bash
# Get title by number
GET http://localhost:8080/ltr/api/titles/{titleNumber}

# Get title by parcel ID
GET http://localhost:8080/ltr/api/titles/parcel/{parcelId}

# Get titles by owner
GET http://localhost:8080/ltr/api/titles/owner/{ownerId}

# Register new title
POST http://localhost:8080/ltr/api/titles/register
```

### Transfer Management
```bash
# Get transfer history
GET http://localhost:8080/ltr/api/transfers/{titleNumber}

# Initiate transfer
POST http://localhost:8080/ltr/api/transfers/initiate
```

## Configuration

### Environment Variables
See `CLOUD_DEPLOYMENT.md` for complete list of environment variables.

### Application Properties
Configuration is in `src/main/resources/application.yml`

## Cloud Deployment
See `CLOUD_DEPLOYMENT.md` for detailed AWS deployment instructions.

## Development

### Project Structure
```
src/main/java/com/trianz/ltr/
├── CloudNativeApplication.java    # Spring Boot main class
├── controller/                     # REST controllers
│   ├── LandTitleRegistryController.java
│   └── HealthCheckController.java
├── service/                        # Business logic
│   └── LandTitleRegistryService.java
├── dao/                           # Data access
│   ├── LandTitleDAO.java
│   └── TitleTransferDAO.java
├── model/                         # Domain models
│   ├── LandTitle.java
│   └── TitleTransfer.java
├── util/                          # Utilities
│   ├── CloudDataSourceUtil.java
│   ├── CloudTransactionUtil.java
│   └── TitleNumberGenerator.java
└── exception/                     # Custom exceptions
    └── LandTitleException.java
```

## Testing
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify
```

## License
Proprietary - Trianz Corporation
