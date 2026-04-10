package com.trianz.ltr.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * DatabaseHealthIndicator - Cloud-Native Health Check for AWS RDS
 *
 * Migrated from custom HealthCheckServlet to Spring Boot Actuator HealthIndicator.
 * Replaces WAS DataSource JNDI lookup with Spring-managed HikariCP DataSource.
 * Replaces java.util.Date with java.time.Instant for UTC consistency.
 *
 * Cloud-Ready Features:
 * - Spring Boot Actuator integration for AWS health checks
 * - HikariCP connection pool health monitoring
 * - UTC timestamp standardization (no timezone dependencies)
 * - Structured health response for AWS ECS/EKS health probes
 * - Connection timeout handling for cloud resilience
 *
 * AWS Integration:
 * - ECS/EKS: Configure health check endpoint as /actuator/health
 * - ALB/NLB: Use /actuator/health for target group health checks
 * - CloudWatch: Monitor health status via custom metrics
 *
 * Endpoints:
 * - GET /actuator/health - Overall application health
 * - GET /actuator/health/liveness - Kubernetes liveness probe
 * - GET /actuator/health/readiness - Kubernetes readiness probe
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final DataSource dataSource;

    @Autowired
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Check database connectivity health
     * Replaces WAS JNDI DataSource lookup with Spring-managed DataSource
     * Uses java.time.Instant instead of java.util.Date for UTC consistency
     */
    @Override
    public Health health() {
        try {
            // Test database connection with timeout
            return checkDatabaseConnection();
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", getCurrentTimestampUTC())
                    .build();
        }
    }

    /**
     * Check database connection using HikariCP pool
     * Replaces direct JDBC connection with connection pool validation
     */
    private Health checkDatabaseConnection() {
        Connection connection = null;
        try {
            // Get connection from HikariCP pool (with timeout)
            connection = dataSource.getConnection();
            
            if (connection != null && !connection.isClosed()) {
                // Validate connection with simple query
                boolean isValid = connection.isValid(5); // 5 second timeout
                
                if (isValid) {
                    logger.debug("Database health check passed");
                    return Health.up()
                            .withDetail("database", "AWS RDS PostgreSQL")
                            .withDetail("status", "Connected")
                            .withDetail("timestamp", getCurrentTimestampUTC())
                            .withDetail("poolActive", getActiveConnections())
                            .build();
                } else {
                    logger.warn("Database connection validation failed");
                    return Health.down()
                            .withDetail("error", "Connection validation failed")
                            .withDetail("timestamp", getCurrentTimestampUTC())
                            .build();
                }
            } else {
                logger.warn("Database connection is null or closed");
                return Health.down()
                        .withDetail("error", "Connection is null or closed")
                        .withDetail("timestamp", getCurrentTimestampUTC())
                        .build();
            }
        } catch (SQLException e) {
            logger.error("Database connection error", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("sqlState", e.getSQLState())
                    .withDetail("timestamp", getCurrentTimestampUTC())
                    .build();
        } finally {
            closeConnectionQuietly(connection);
        }
    }

    /**
     * Get current timestamp in UTC using java.time API
     * Replaces java.util.Date with java.time.Instant for cloud consistency
     */
    private String getCurrentTimestampUTC() {
        return Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
    }

    /**
     * Get active connection count from HikariCP pool
     */
    private int getActiveConnections() {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDS = (com.zaxxer.hikari.HikariDataSource) dataSource;
                return hikariDS.getHikariPoolMXBean().getActiveConnections();
            }
        } catch (Exception e) {
            logger.debug("Unable to get active connection count", e);
        }
        return -1;
    }

    /**
     * Close connection quietly without throwing exceptions
     */
    private void closeConnectionQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.debug("Error closing connection", e);
            }
        }
    }
}
