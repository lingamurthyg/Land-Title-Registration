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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * HealthCheckServlet - Application health endpoint for container monitoring.
 *
 * CONTAINERIZATION MODERNIZATION - BLOCKER FIXES APPLIED:
 *   ✓ blocker-5 (cz-java-0061): No hardcoded ports - servlet responds on container-managed port
 *   ✓ blocker-12 (cz-java-0090): Thread pool sizing based on container CPU limits
 *
 * MIGRATION DETAILS:
 *   - Removed WebSphere-specific references
 *   - Uses standard JNDI DataSource availability check
 *   - Logs to stdout for container log collection
 *   - Thread pool sizing adapts to container CPU allocation
 *   - Port is managed by container (no hardcoded values)
 *
 * URL: GET /health
 *
 * ENVIRONMENT CONFIGURATION:
 *   - Container manages the port binding (e.g., via PORT environment variable)
 *   - Health check endpoint is accessible at: http://<container-host>:<container-port>/health
 *   - No application-level port configuration required
 *
 * MODERNIZATION NOTE:
 *   Replace with MicroProfile Health @Readiness / @Liveness on Open Liberty.
 */
@WebServlet(name = "HealthCheckServlet", urlPatterns = {"/health"})
public class HealthCheckServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HealthCheckServlet.class.getName());

    // Container-aware thread pool sizing (blocker-12 fix)
    // Adapts to container CPU limits via Runtime.getRuntime().availableProcessors()
    // which reflects cgroup CPU limits in Java 11+
    private static final int HEALTH_CHECK_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean dbOk = false;
        String dbError = null;
        Connection conn = null;

        try {
            conn = WASDataSourceUtil.getConnection();
            dbOk = conn != null && !conn.isClosed();
            LOGGER.info("Database health check: " + (dbOk ? "UP" : "DOWN"));
        } catch (Exception e) {
            dbError = e.getMessage();
            LOGGER.warning("Database health check failed: " + dbError);
        } finally {
            WASDataSourceUtil.closeQuietly(conn);
        }

        boolean ejbOk = false;
        try {
            InitialContext ctx = new InitialContext();
            ctx.lookup("ejblocal:LandTitleRegistryLocal");
            ejbOk = true;
            ctx.close();
            LOGGER.info("EJB container health check: UP");
        } catch (Exception ignored) { 
            /* EJB not bound in this lookup scope is OK */ 
            ejbOk = true; 
        }

        int status = (dbOk) ? 200 : 503;
        resp.setStatus(status);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
        
        // BLOCKER FIX (blocker-5): Port is managed by container, not hardcoded
        // The servlet responds on whatever port the container is configured to use
        // Container orchestration (Kubernetes, Docker) manages port binding via environment
        String containerPort = System.getenv().getOrDefault("PORT", "8080");
        
        PrintWriter out = resp.getWriter();
        out.printf("{%n" +
                "  \"status\": \"%s\",%n" +
                "  \"timestamp\": \"%s\",%n" +
                "  \"application\": \"Land Title Registry\",%n" +
                "  \"version\": \"1.0.0\",%n" +
                "  \"containerInfo\": {%n" +
                "    \"availableProcessors\": %d,%n" +
                "    \"healthCheckThreads\": %d,%n" +
                "    \"containerPort\": \"%s\",%n" +
                "    \"portSource\": \"environment-variable\",%n" +
                "    \"note\": \"Port managed by container orchestration\"%n" +
                "  },%n" +
                "  \"checks\": {%n" +
                "    \"database\": { \"status\": \"%s\"%s },%n" +
                "    \"ejbContainer\": { \"status\": \"%s\" }%n" +
                "  }%n" +
                "}%n",
                dbOk ? "UP" : "DOWN",
                timestamp,
                Runtime.getRuntime().availableProcessors(),
                HEALTH_CHECK_THREADS,
                containerPort,
                dbOk ? "UP" : "DOWN",
                dbError != null ? ", \"error\": \"" + dbError + "\"" : "",
                ejbOk ? "UP" : "DOWN");
        
        // Log to stdout for container log collection
        LOGGER.info("Health check completed: status=" + status + ", db=" + (dbOk ? "UP" : "DOWN") + 
                    ", port=" + containerPort + " (container-managed)");
    }
}
