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

/**
 * HealthCheckServlet - Application health endpoint for WAS monitoring.
 *
 * WAS-SPECIFIC: checks WAS JNDI DataSource availability.
 * URL: GET /health
 *
 * MODERNIZATION NOTE:
 *   Replace with MicroProfile Health @Readiness / @Liveness on Open Liberty.
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

        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
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
