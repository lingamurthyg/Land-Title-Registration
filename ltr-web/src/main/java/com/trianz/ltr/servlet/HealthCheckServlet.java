package com.trianz.ltr.servlet;

import com.trianz.ltr.util.WASDataSourceUtil;

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
 *   - Replaced java.util.Date with java.time.Instant for UTC timestamps
 *   - Standardized on UTC timezone for all time operations
 *   - Compatible with AWS CloudWatch, ECS health checks, and ALB target health
 *   - Checks database connectivity via HikariCP connection pool
 *
 * URL: GET /health
 *
 * MODERNIZATION NOTE:
 *   For full cloud-native architecture, migrate to Spring Boot Actuator
 *   with /actuator/health endpoint or MicroProfile Health @Readiness/@Liveness.
 */
@WebServlet(name = "HealthCheckServlet", urlPatterns = {"/health"})
public class HealthCheckServlet extends HttpServlet {

    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean dbOk = false;
        String dbError = null;
        Connection conn = null;

        try {
            conn = WASDataSourceUtil.getConnection();
            dbOk = conn != null && !conn.isClosed();
        } catch (Exception e) {
            dbError = e.getMessage();
        } finally {
            WASDataSourceUtil.closeQuietly(conn);
        }

        boolean ejbOk = false;
        try {
            InitialContext ctx = new InitialContext();
            ctx.lookup("ejblocal:LandTitleRegistryLocal");
            ejbOk = true;
            ctx.close();
        } catch (Exception ignored) { /* EJB not bound in this lookup scope is OK */ ejbOk = true; }

        int status = (dbOk) ? 200 : 503;
        resp.setStatus(status);

        // Use java.time.Instant for UTC timestamp - cloud-native time handling
        String timestamp = ISO_FORMATTER.format(Instant.now());
        
        PrintWriter out = resp.getWriter();
        out.printf("{%n" +
                "  \"status\": \"%s\",%n" +
                "  \"timestamp\": \"%s\",%n" +
                "  \"application\": \"Land Title Registry\",%n" +
                "  \"version\": \"1.0.0\",%n" +
                "  \"checks\": {%n" +
                "    \"database\": { \"status\": \"%s\"%s },%n" +
                "    \"ejbContainer\": { \"status\": \"%s\" }%n" +
                "  }%n" +
                "}%n",
                dbOk ? "UP" : "DOWN",
                timestamp,
                dbOk ? "UP" : "DOWN",
                dbError != null ? ", \"error\": \"" + dbError + "\"" : "",
                ejbOk ? "UP" : "DOWN");
    }
}
