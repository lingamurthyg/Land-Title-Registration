package com.trianz.ltr.servlet;

import com.trianz.ltr.util.DataSourceUtil;

import javax.naming.InitialContext;
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
 * HealthCheckServlet - Application health endpoint for cloud monitoring.
 *
 * CLOUD-NATIVE MIGRATION:
 *   - Replaced java.util.Date with java.time.Instant for UTC standardization
 *   - Uses DateTimeFormatter with UTC zone for consistent timestamps across distributed services
 *   - Checks database connectivity for AWS RDS health monitoring
 *   - Compatible with AWS Application Load Balancer health checks
 *   - Removed WAS-specific DataSource lookups, uses cloud-native DataSourceUtil
 *
 * URL: GET /health
 *
 * MODERNIZATION NOTE:
 *   Consider replacing with Spring Boot Actuator health endpoints for better cloud-native patterns.
 */
@WebServlet(name = "HealthCheckServlet", urlPatterns = {"/health"})
public class HealthCheckServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean dbOk = false;
        String dbError = null;
        Connection conn = null;

        try {
            conn = DataSourceUtil.getConnection();
            dbOk = conn != null && !conn.isClosed();
        } catch (Exception e) {
            dbError = e.getMessage();
        } finally {
            DataSourceUtil.closeQuietly(conn);
        }

        boolean ejbOk = true; // In Spring Boot, EJB container check is not applicable

        int status = (dbOk) ? 200 : 503;
        resp.setStatus(status);

        // Use java.time API with UTC for consistent timestamps across distributed cloud services
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC);
        String timestamp = formatter.format(Instant.now());
        
        PrintWriter out = resp.getWriter();
        out.printf("{%n" +
                "  \"status\": \"%s\",%n" +
                "  \"timestamp\": \"%s\",%n" +
                "  \"application\": \"Land Title Registry\",%n" +
                "  \"version\": \"1.0.0\",%n" +
                "  \"checks\": {%n" +
                "    \"database\": { \"status\": \"%s\"%s },%n" +
                "    \"application\": { \"status\": \"%s\" }%n" +
                "  }%n" +
                "}%n",
                dbOk ? "UP" : "DOWN",
                timestamp,
                dbOk ? "UP" : "DOWN",
                dbError != null ? ", \"error\": \"" + dbError + "\"" : "",
                ejbOk ? "UP" : "DOWN");
    }
}
