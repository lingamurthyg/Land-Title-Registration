# Multi-stage Dockerfile for Land Title Registry EJB Module
# Base Image: amazoncorretto:8 (explicitly provided)
# Build Tool: Maven
# Java Version: 1.8

# ============================================================================
# Stage 1: Builder - Build the entire multi-module project
# ============================================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy parent POM first for dependency resolution
COPY pom.xml .

# Copy all module POMs for dependency caching
COPY ltr-ejb/pom.xml ltr-ejb/
COPY ltr-web/pom.xml ltr-web/
COPY ltr-ear/pom.xml ltr-ear/

# Download dependencies (leverages Docker layer caching)
RUN mvn dependency:go-offline -B || true

# Copy entire project source code
COPY . .

# Build parent POM first (install parent without building modules)
RUN mvn clean install -N -DskipTests

# Build all modules (generates all JARs including ltr-ejb)
RUN mvn clean package -DskipTests

# ============================================================================
# Stage 2: Runtime - Use explicit base image amazoncorretto:8
# ============================================================================
FROM amazoncorretto:8

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the EJB JAR from builder stage
COPY --from=builder /workspace/ltr-ejb/target/*.jar /app/ltr-ejb.jar

# Set ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Set timezone
ENV TZ=UTC

# Expose default EJB/Application port (configurable via environment)
EXPOSE 8080

# Health check endpoint (application-specific)
# Note: EJB modules typically don't have HTTP endpoints
# This is a placeholder - adjust based on actual deployment

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ltr-ejb.jar"]
