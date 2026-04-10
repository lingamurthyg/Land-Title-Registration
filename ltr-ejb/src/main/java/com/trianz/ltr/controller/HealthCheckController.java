package com.trianz.ltr.controller;

import com.trianz.ltr.util.CloudDataSourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthCheckController - Cloud-native health check endpoint for AWS ECS/EKS.
 * 
 * Provides:
 * - Liveness probe: Is the application running?
 * - Readiness probe: Is the application ready to serve traffic?
 * - Database connectivity check
 * - Connection pool statistics
 * 
 * Used by:
 * - AWS ECS/EKS health checks
 * - AWS Application Load Balancer target health
 * - Kubernetes liveness/readiness probes
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckController.class);

    /**
     * Basic liveness check - is the application running?
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("application", "land-title-registry");
        health.put("version", "2.0.0-cloud-native");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Readiness check - is the application ready to serve traffic?
     * Includes database connectivity check.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", Instant.now().toString());
        
        // Check database connectivity
        boolean dbHealthy = checkDatabaseHealth();
        
        if (dbHealthy) {
            health.put("status", "UP");
            health.put("database", "UP");
            health.put("poolStats", CloudDataSourceUtil.getPoolStats());
            return ResponseEntity.ok(health);
        } else {
            health.put("status", "DOWN");
            health.put("database", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    /**
     * Detailed health check with component status.
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", Instant.now().toString());
        health.put("application", "land-title-registry");
        
        // Database health
        boolean dbHealthy = checkDatabaseHealth();
        Map<String, Object> dbHealth = new HashMap<>();
        dbHealth.put("status", dbHealthy ? "UP" : "DOWN");
        dbHealth.put("poolStats", CloudDataSourceUtil.getPoolStats());
        health.put("database", dbHealth);
        
        // Overall status
        health.put("status", dbHealthy ? "UP" : "DOWN");
        
        HttpStatus httpStatus = dbHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

    /**
     * Check database connectivity by attempting to get a connection.
     */
    private boolean checkDatabaseHealth() {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            // Execute a simple query to verify connectivity
            conn.createStatement().execute("SELECT 1");
            return true;
        } catch (Exception e) {
            LOGGER.error("Database health check failed", e);
            return false;
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }
}
