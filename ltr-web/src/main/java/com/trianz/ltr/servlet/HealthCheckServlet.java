package com.trianz.ltr.servlet;

import com.trianz.ltr.util.CloudDataSourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * HealthCheckServlet - Cloud-native application health endpoint.
 *
 * Cloud-native improvements:
 * - Replaced WASDataSourceUtil with CloudDataSourceUtil
 * - Uses java.time.Instant for UTC timestamps (timezone-safe)
 * - Removed EJB container checks (not needed in Spring)
 * - Compatible with AWS ALB health checks, Kubernetes liveness/readiness probes
 * - Structured JSON response for cloud monitoring
 *
 * URL: GET /health
 *
 * MIGRATION NOTE:
 *   Consider migrating to Spring Boot Actuator for comprehensive health checks:
 *   - /actuator/health (liveness and readiness)
 *   - /actuator/metrics (CloudWatch integration)
 *   - /actuator/info (application metadata)
 *   - Custom health indicators for AWS services
 */
@WebServlet(name = "HealthCheckServlet", urlPatterns = {"/health"})
public class HealthCheckServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckServlet.class);
    private static final long serialVersionUID = 2L; // Incremented for cloud migration
    
    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean dbOk = false;
        String dbError = null;
        long dbResponseTimeMs = 0;
        Connection conn = null;

        try {
            long startTime = System.currentTimeMillis();
            conn = CloudDataSourceUtil.getConnection();
            dbOk = conn != null && !conn.isClosed();
            
            // Test database connectivity with a simple query
            if (dbOk) {
                conn.createStatement().execute("SELECT 1");
            }
            
            dbResponseTimeMs = System.currentTimeMillis() - startTime;
            
        } catch (Exception e) {
            dbError = e.getMessage();
            LOGGER.error("Database health check failed", e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }

        // Get connection pool statistics
        String poolStats = "N/A";
        try {
            poolStats = CloudDataSourceUtil.getPoolStats();
        } catch (Exception e) {
            LOGGER.warn("Failed to get pool statistics", e);
        }

        // Overall health status
        boolean healthy = dbOk;
        int status = healthy ? 200 : 503;
        resp.setStatus(status);

        // Generate UTC timestamp
        String timestamp = ISO_FORMATTER.format(Instant.now());
        
        // Write JSON response
        PrintWriter out = resp.getWriter();
        out.printf("{%n");
        out.printf("  \"status\": \"%s\",%n", healthy ? "UP" : "DOWN");
        out.printf("  \"timestamp\": \"%s\",%n", timestamp);
        out.printf("  \"application\": \"Land Title Registry\",%n");
        out.printf("  \"version\": \"2.0.0-cloud-native\",%n");
        out.printf("  \"environment\": \"%s\",%n", getEnvironment());
        out.printf("  \"checks\": {%n");
        out.printf("    \"database\": {%n");
        out.printf("      \"status\": \"%s\",%n", dbOk ? "UP" : "DOWN");
        out.printf("      \"responseTimeMs\": %d%s%n", dbResponseTimeMs, 
                   dbError != null ? "," : "");
        if (dbError != null) {
            out.printf("      \"error\": \"%s\"%n", escapeJson(dbError));
        }
        out.printf("    },%n");
        out.printf("    \"connectionPool\": {%n");
        out.printf("      \"status\": \"UP\",%n");
        out.printf("      \"details\": \"%s\"%n", escapeJson(poolStats));
        out.printf("    }%n");
        out.printf("  },%n");
        out.printf("  \"cloudProvider\": \"AWS\",%n");
        out.printf("  \"region\": \"%s\"%n", System.getenv().getOrDefault("AWS_REGION", "unknown"));
        out.printf("}%n");
        
        LOGGER.debug("Health check completed: status={}, dbOk={}, responseTime={}ms", 
                    healthy ? "UP" : "DOWN", dbOk, dbResponseTimeMs);
    }

    /**
     * Determine the current environment from environment variables.
     */
    private String getEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        if (env != null) return env;
        
        env = System.getenv("SPRING_PROFILES_ACTIVE");
        if (env != null) return env;
        
        return "unknown";
    }

    /**
     * Escape special characters for JSON strings.
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
