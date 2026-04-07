# ============================================================================
# Multi-Stage Dockerfile for Land Title Registry Web Application (ltr-web)
# ============================================================================
# Technology Stack: Java 8, Maven, Java EE WAR
# Base Image: amazoncorretto:8 (explicitly provided)
# Build Tool: Maven (system command, no wrapper)
# Packaging: WAR file deployed to Open Liberty
# ============================================================================

# ============================================================================
# Stage 1: Builder - Build the entire multi-module project
# ============================================================================
FROM maven:3.9.4-eclipse-temurin-8 AS builder

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

# Build all modules (generates WAR in ltr-web/target/)
RUN mvn clean package -DskipTests

# Verify WAR file was created
RUN ls -lh /workspace/ltr-web/target/*.war

# ============================================================================
# Stage 2: Runtime - Open Liberty with amazoncorretto:8 base
# ============================================================================
FROM amazoncorretto:8

# Install Open Liberty for Java EE application deployment
ENV LIBERTY_VERSION=23.0.0.3
ENV LIBERTY_HOME=/opt/ol
ENV PATH=$LIBERTY_HOME/wlp/bin:$PATH

# Install Open Liberty
RUN yum install -y wget unzip && \
    wget -q https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/${LIBERTY_VERSION}/openliberty-${LIBERTY_VERSION}.zip && \
    unzip -q openliberty-${LIBERTY_VERSION}.zip -d /opt && \
    mv /opt/wlp ${LIBERTY_HOME}/wlp && \
    rm openliberty-${LIBERTY_VERSION}.zip && \
    yum clean all

# Create non-root user for security
RUN groupadd -r ltrapp && useradd -r -g ltrapp -u 1001 ltrapp

# Set up Liberty server
WORKDIR ${LIBERTY_HOME}/wlp/usr/servers
RUN server create ltrServer

# Copy WAR file from builder stage
COPY --from=builder /workspace/ltr-web/target/ltr-web.war ${LIBERTY_HOME}/wlp/usr/servers/ltrServer/apps/

# Configure Liberty server.xml
RUN echo '<?xml version="1.0" encoding="UTF-8"?>\n\
<server description="Land Title Registry Server">\n\
    <featureManager>\n\
        <feature>servlet-3.1</feature>\n\
        <feature>javaee-7.0</feature>\n\
        <feature>ejbLite-3.1</feature>\n\
        <feature>jndi-1.0</feature>\n\
        <feature>jdbc-4.1</feature>\n\
    </featureManager>\n\
    \n\
    <httpEndpoint id="defaultHttpEndpoint"\n\
                  host="*"\n\
                  httpPort="8080"\n\
                  httpsPort="8443" />\n\
    \n\
    <webApplication id="ltr-web"\n\
                    location="ltr-web.war"\n\
                    contextRoot="/" />\n\
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
</server>' > ${LIBERTY_HOME}/wlp/usr/servers/ltrServer/server.xml

# Set ownership
RUN chown -R ltrapp:ltrapp ${LIBERTY_HOME}

# Switch to non-root user
USER ltrapp

# Expose application port
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check using application endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD server status ltrServer || exit 1

# Start Liberty server
CMD ["server", "run", "ltrServer"]
