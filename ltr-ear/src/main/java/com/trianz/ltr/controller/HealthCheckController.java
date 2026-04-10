package com.trianz.ltr.controller;

import com.trianz.ltr.util.CloudDataSourceUtil;

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
 * HealthCheckController - Cloud-native health check endpoint.
 *
 * CLOUD-READY FEATURES:
 *   - Standard health check format for AWS ELB, ALB, ECS
 *   - UTC timestamp for distributed systems
 *   - JSON response format
 *   - Database connectivity check
 *   - Compatible with Kubernetes liveness/readiness probes
 *
 * URL: GET /health
 */
@WebServlet(name = "HealthCheckController", urlPatterns = {"/health"})
public class HealthCheckController extends HttpServlet {

    private static final DateTimeFormatter ISO_FORMATTER = 
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean dbOk = false;
        String dbError = null;
        Connection conn = null;

        try {
            conn = CloudDataSourceUtil.getConnection();
            dbOk = conn != null && !conn.isClosed();
        } catch (Exception e) {
            dbError = e.getMessage();
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }

        // Overall status
        int status = (dbOk) ? 200 : 503;
        resp.setStatus(status);

        // UTC timestamp
        String timestamp = ISO_FORMATTER.format(Instant.now());
        
        PrintWriter out = resp.getWriter();
        out.printf("{%n" +
                "  \"status\": \"%s\",%n" +
                "  \"timestamp\": \"%s\",%n" +
                "  \"application\": \"Land Title Registry\",%n" +
                "  \"version\": \"2.0.0-cloud\",%n" +
                "  \"environment\": \"%s\",%n" +
                "  \"checks\": {%n" +
                "    \"database\": { \"status\": \"%s\"%s }%n" +
                "  }%n" +
                "}%n",
                dbOk ? "UP" : "DOWN",
                timestamp,
                getEnvironment(),
                dbOk ? "UP" : "DOWN",
                dbError != null ? ", \"error\": \"" + escapeJson(dbError) + "\"" : "");
    }

    private String getEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        return (env != null && !env.isEmpty()) ? env : "development";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
