# ============================================================================
# Multi-stage Dockerfile for Land Title Registry
# ============================================================================
# Builds executable JAR and creates optimized container image for AWS deployment.
#
# AWS Deployment Targets:
# - Amazon ECS (Elastic Container Service)
# - Amazon EKS (Elastic Kubernetes Service)
# - AWS Fargate (serverless containers)
# - AWS App Runner
#
# Build:
#   docker build -t land-title-registry:latest .
#
# Run:
#   docker run -p 8080:8080 \
#     -e DB_HOST=mydb.rds.amazonaws.com \
#     -e DB_PORT=5432 \
#     -e DB_NAME=landtitle \
#     -e DB_USERNAME=dbuser \
#     -e DB_PASSWORD=dbpass \
#     -e AWS_REGION=us-east-1 \
#     land-title-registry:latest
# ============================================================================

# Stage 1: Build stage
FROM maven:3.8.6-eclipse-temurin-11 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build executable JAR
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:11-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/ltr-web.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom \
               -Duser.timezone=UTC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
