# ============================================================================
# Dockerfile for Land Title Registry - Cloud Native
# ============================================================================
# Multi-stage build for optimized container size
# Compatible with AWS ECS, EKS, and Elastic Beanstalk
# ============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app

# Copy POM and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:11-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 ltr && \
    adduser -D -u 1001 -G ltr ltr

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/ltr-cloud-native.jar ./app.jar

# Create log directory
RUN mkdir -p /var/log/ltr && \
    chown -R ltr:ltr /var/log/ltr /app

# Switch to non-root user
USER ltr

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/ltr/health || exit 1

# Environment variables (override in ECS/EKS task definition)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SERVER_PORT=8080 \
    ENVIRONMENT=production \
    LOG_LEVEL=INFO

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
