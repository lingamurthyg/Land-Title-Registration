# ============================================================================
# Multi-Stage Dockerfile for Land Title Registry EAR Application
# Technology: Java 8, Maven, IBM WebSphere (migrating to Open Liberty)
# Target Platform: AWS EKS (Kubernetes)
# Base Image: amazoncorretto:8 (explicitly provided)
# ============================================================================

# ============================================================================
# Stage 1: Builder - Build the multi-module Maven project
# ============================================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy parent POM first for dependency resolution
COPY pom.xml .

# Copy all module POMs for dependency caching
COPY ltr-ejb/pom.xml ltr-ejb/
COPY ltr-web/pom.xml ltr-web/
COPY ltr-ear/pom.xml ltr-ear/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B || true

# Copy entire project source
COPY . .

# Build parent POM (install parent without building modules)
RUN mvn clean install -N -DskipTests

# Build all modules (generates EJB JAR, WAR, and EAR)
RUN mvn clean package -DskipTests

# Verify EAR artifact was created
RUN ls -lh ltr-ear/target/*.ear

# ============================================================================
# Stage 2: Runtime - Amazon Corretto 8 JRE (Explicit Base Image)
# ============================================================================
FROM amazoncorretto:8

# Metadata
LABEL maintainer="Land Title Registry Team"
LABEL application="land-title-registry"
LABEL version="1.0.0"
LABEL description="Land Title Registry EAR Application on Amazon Corretto 8"

# Create non-root user for security
RUN yum install -y shadow-utils && \
    groupadd -r ltrapp && \
    useradd -r -g ltrapp -u 1001 -m -s /sbin/nologin ltrapp && \
    yum clean all

# Set working directory
WORKDIR /opt/ltr

# Copy EAR artifact from builder stage
COPY --from=builder /workspace/ltr-ear/target/land-title-registry.ear ./land-title-registry.ear

# Set ownership
RUN chown -R ltrapp:ltrapp /opt/ltr

# Switch to non-root user
USER ltrapp

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    TZ=UTC \
    PORT=8080

# Expose application port (default 8080, configurable via environment)
EXPOSE 8080

# Note: This EAR requires a Java EE application server (Open Liberty, WildFly, etc.)
# The actual startup command should be provided by the deployment configuration
# Example for Open Liberty: CMD ["server", "run", "defaultServer"]
CMD ["java", "-version"]
