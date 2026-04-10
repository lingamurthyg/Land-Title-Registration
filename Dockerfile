# Multi-stage Dockerfile for Land Title Registry Cloud-Native Application
# Optimized for AWS ECS, EKS, Azure AKS, GCP GKE

# Stage 1: Build
FROM maven:3.8.6-eclipse-temurin-11 AS builder

WORKDIR /build

# Copy pom files for dependency resolution
COPY pom.xml .
COPY ltr-ejb/pom.xml ltr-ejb/
COPY ltr-web/pom.xml ltr-web/
COPY ltr-ear/pom.xml ltr-ear/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY ltr-ejb/src ltr-ejb/src
COPY ltr-web/src ltr-web/src
COPY ltr-ear/src ltr-ear/src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:11-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 ltr && \
    adduser -D -u 1001 -G ltr ltr

# Create log directory
RUN mkdir -p /var/log/ltr && \
    chown -R ltr:ltr /var/log/ltr

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/ltr-web/target/ltr-web.jar app.jar

# Change ownership
RUN chown -R ltr:ltr /app

# Switch to non-root user
USER ltr

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# JVM options for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
