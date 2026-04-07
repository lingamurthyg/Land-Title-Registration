# ============================================================================
# Multi-Stage Dockerfile for Land Title Registry System
# Java 8 Maven Multi-Module Project (EAR packaging)
# Target Platform: AWS EKS
# Base Image: amazoncorretto:8 (explicitly specified)
# ============================================================================

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

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B || true

# Copy entire project source code
COPY . .

# Build parent POM first (install parent without building modules)
RUN mvn clean install -N -DskipTests

# Build all modules (generates EAR with embedded EJB and WAR)
RUN mvn clean package -DskipTests

# Verify the EAR artifact was created
RUN ls -lh /workspace/ltr-ear/target/*.ear

# ============================================================================
# Stage 2: Runtime - Deploy EAR to application server
# ============================================================================
FROM amazoncorretto:8

# Set metadata
LABEL maintainer="Land Title Registry Team"
LABEL application="land-title-registry"
LABEL version="1.0.0"
LABEL description="Enterprise Land Title Registry System - Containerized for AWS EKS"

# Install OpenLiberty (lightweight Java EE application server)
ENV LIBERTY_VERSION=23.0.0.3
ENV LIBERTY_HOME=/opt/ol
ENV LIBERTY_SERVER_NAME=defaultServer

# Create non-root user for security
RUN groupadd -r liberty && useradd -r -g liberty liberty

# Install OpenLiberty
RUN yum install -y wget unzip && \
    mkdir -p ${LIBERTY_HOME} && \
    wget -q https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/23.0.0.3/openliberty-23.0.0.3.zip -O /tmp/openliberty.zip && \
    unzip -q /tmp/openliberty.zip -d /tmp && \
    mv /tmp/wlp/* ${LIBERTY_HOME}/ && \
    rm -rf /tmp/openliberty.zip /tmp/wlp && \
    yum clean all && \
    rm -rf /var/cache/yum

# Create Liberty server
RUN ${LIBERTY_HOME}/bin/server create ${LIBERTY_SERVER_NAME}

# Configure Liberty server
RUN mkdir -p ${LIBERTY_HOME}/usr/servers/${LIBERTY_SERVER_NAME}/apps && \
    mkdir -p ${LIBERTY_HOME}/usr/servers/${LIBERTY_SERVER_NAME}/logs && \
    mkdir -p ${LIBERTY_HOME}/usr/servers/${LIBERTY_SERVER_NAME}/configDropins/overrides

# Copy EAR artifact from builder stage
COPY --from=builder /workspace/ltr-ear/target/land-title-registry.ear ${LIBERTY_HOME}/usr/servers/${LIBERTY_SERVER_NAME}/apps/

# Create server.xml configuration
RUN echo '<?xml version="1.0" encoding="UTF-8"?>\n\
<server description="Land Title Registry Server">\n\
    <featureManager>\n\
        <feature>javaee-7.0</feature>\n\
        <feature>localConnector-1.0</feature>\n\
    </featureManager>\n\
    \n\
    <httpEndpoint id="defaultHttpEndpoint"\n\
                  host="*"\n\
                  httpPort="8080"\n\
                  httpsPort="8443" />\n\
    \n\
    <application id="land-title-registry"\n\
                 location="land-title-registry.ear"\n\
                 name="land-title-registry"\n\
                 type="ear">\n\
        <classloader delegation="parentLast" />\n\
    </application>\n\
    \n\
    <dataSource id="LandTitleDS" jndiName="jdbc/LandTitleDS">\n\
        <jdbcDriver libraryRef="db2-library"/>\n\
        <properties.db2.jcc\n\
            serverName="${env.DB_HOST}"\n\
            portNumber="${env.DB_PORT}"\n\
            databaseName="${env.DB_NAME}"\n\
            user="${env.DB_USER}"\n\
            password="${env.DB_PASSWORD}"/>\n\
    </dataSource>\n\
    \n\
    <library id="db2-library">\n\
        <fileset dir="${server.config.dir}/lib" includes="*.jar"/>\n\
    </library>\n\
    \n\
    <logging consoleLogLevel="INFO" />\n\
    \n\
    <webContainer trustHostHeaderPort="true" />\n\
</server>' > ${LIBERTY_HOME}/usr/servers/${LIBERTY_SERVER_NAME}/server.xml

# Set ownership to non-root user
RUN chown -R liberty:liberty ${LIBERTY_HOME}

# Switch to non-root user
USER liberty

# Set working directory
WORKDIR ${LIBERTY_HOME}

# Expose application port
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV TZ=UTC
ENV PORT=8080

# Health check endpoint
# Note: Health checks are handled by Kubernetes probes, not Docker HEALTHCHECK

# Start Liberty server in foreground
CMD ["sh", "-c", "${LIBERTY_HOME}/bin/server run ${LIBERTY_SERVER_NAME}"]
