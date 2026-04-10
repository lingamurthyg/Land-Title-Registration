package com.trianz.ltr.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trianz.ltr.ejb.LandTitleException;
import com.trianz.ltr.ejb.LandTitleRegistryLocal;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TitleRegistryServlet - REST API controller for the Land Title Registry.
 *
 * Cloud-native migration:
 * - Replaced @EJB injection with Spring @Autowired
 * - Replaced HttpServletRequest.getUserPrincipal() with Spring SecurityContextHolder
 * - Uses SLF4J for structured logging (cloud-compatible)
 * - Ready for containerization (AWS ECS, EKS, Azure AKS, GCP GKE)
 *
 * MIGRATION NOTE:
 *   Consider migrating to Spring @RestController for better cloud-native patterns:
 *   - Automatic JSON serialization
 *   - Exception handling with @ControllerAdvice
 *   - OpenAPI/Swagger documentation
 *   - Spring Cloud integration
 *
 * URL patterns:
 *   GET  /api/titles/{titleNumber}         → getTitleByNumber
 *   GET  /api/titles?owner={id}            → getTitlesByOwner
 *   GET  /api/titles?status={STATUS}       → getTitlesByStatus
 *   GET  /api/titles?q={keyword}           → searchTitles
 *   POST /api/titles                       → registerTitle
 *   PUT  /api/titles/{titleNumber}         → updateTitle
 *   GET  /api/transfers/pending            → getPendingTransfers
 *   POST /api/transfers                    → initiateTransfer
 *   POST /api/transfers/{id}/approve       → approveTransfer
 *   POST /api/transfers/{id}/reject        → rejectTransfer
 *   GET  /api/transfers?title={titleNum}   → getTransferHistory
 */
@WebServlet(name = "TitleRegistryServlet", urlPatterns = {"/api/titles/*", "/api/transfers/*"})
public class TitleRegistryServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitleRegistryServlet.class);
    private static final long serialVersionUID = 2L; // Incremented for cloud migration

    /** Spring-managed service (injected via @Autowired) */
    @Autowired
    private LandTitleRegistryLocal registryBean;

    private ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.findAndRegisterModules();
        LOGGER.info("TitleRegistryServlet initialized with cloud-native configuration");
    }

    // ── GET ────────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        String servletPath = req.getServletPath();

        try {
            if (servletPath.startsWith("/api/transfers")) {
                handleGetTransfers(req, resp, pathInfo);
            } else {
                handleGetTitles(req, resp, pathInfo);
            }
        } catch (LandTitleException e) {
            sendError(resp, e.getHttpStatusCode(), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unhandled error in doGet", e);
            sendError(resp, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private void handleGetTitles(HttpServletRequest req, HttpServletResponse resp, String pathInfo)
            throws Exception {

        String owner   = req.getParameter("owner");
        String status  = req.getParameter("status");
        String keyword = req.getParameter("q");

        if (pathInfo != null && pathInfo.length() > 1) {
            // GET /api/titles/{titleNumber}
            String titleNumber = pathInfo.substring(1);
            LandTitle title = registryBean.getTitleByNumber(titleNumber);
            sendJson(resp, 200, title);

        } else if (owner != null) {
            List<LandTitle> titles = registryBean.getTitlesByOwner(owner);
            sendJson(resp, 200, titles);

        } else if (status != null) {
            TitleStatus ts = TitleStatus.valueOf(status.toUpperCase());
            List<LandTitle> titles = registryBean.getTitlesByStatus(ts);
            sendJson(resp, 200, titles);

        } else if (keyword != null) {
            List<LandTitle> titles = registryBean.searchTitles(keyword);
            sendJson(resp, 200, titles);

        } else {
            sendError(resp, 400, "BAD_REQUEST", "Provide titleNumber path, ?owner=, ?status=, or ?q=");
        }
    }

    private void handleGetTransfers(HttpServletRequest req, HttpServletResponse resp, String pathInfo)
            throws Exception {

        String titleNum = req.getParameter("title");

        if (pathInfo != null && pathInfo.equals("/pending")) {
            // Check roles using Spring Security
            if (!hasRole("REGISTRY_SUPERVISOR") && !hasRole("REGISTRY_ADMIN")) {
                sendError(resp, 403, "FORBIDDEN", "Insufficient role to view pending transfers");
                return;
            }
            List<TitleTransfer> pending = registryBean.getPendingTransfers();
            sendJson(resp, 200, pending);

        } else if (titleNum != null) {
            List<TitleTransfer> history = registryBean.getTransferHistory(titleNum);
            sendJson(resp, 200, history);

        } else {
            sendError(resp, 400, "BAD_REQUEST", "Provide /pending path or ?title= parameter");
        }
    }

    // ── POST ───────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo   = req.getPathInfo();
        String servletPath = req.getServletPath();

        try {
            if (servletPath.startsWith("/api/transfers")) {
                handlePostTransfer(req, resp, pathInfo);
            } else {
                handlePostTitle(req, resp);
            }
        } catch (LandTitleException e) {
            sendError(resp, e.getHttpStatusCode(), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unhandled error in doPost", e);
            sendError(resp, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private void handlePostTitle(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        LandTitle title = mapper.readValue(req.getInputStream(), LandTitle.class);
        String titleNumber = registryBean.registerTitle(title);

        Map<String, String> result = new HashMap<>();
        result.put("titleNumber", titleNumber);
        result.put("message", "Title registered successfully");
        sendJson(resp, 201, result);
    }

    private void handlePostTransfer(HttpServletRequest req, HttpServletResponse resp, String pathInfo)
            throws Exception {

        // POST /api/transfers/{id}/approve
        // POST /api/transfers/{id}/reject
        // POST /api/transfers   (initiate)

        if (pathInfo != null && pathInfo.matches("/\\d+/approve")) {
            Long transferId = Long.parseLong(pathInfo.split("/")[1]);
            String approver = getCurrentUsername();
            registryBean.approveTransfer(transferId, approver);
            sendJson(resp, 200, singleMessage("Transfer " + transferId + " approved"));

        } else if (pathInfo != null && pathInfo.matches("/\\d+/reject")) {
            Long transferId = Long.parseLong(pathInfo.split("/")[1]);
            String reason = req.getParameter("reason");
            String rejector = getCurrentUsername();
            registryBean.rejectTransfer(transferId, rejector, reason);
            sendJson(resp, 200, singleMessage("Transfer " + transferId + " rejected"));

        } else {
            // Initiate a new transfer
            TitleTransfer transfer = mapper.readValue(req.getInputStream(), TitleTransfer.class);
            Long id = registryBean.initiateTransfer(transfer);
            Map<String, Object> result = new HashMap<>();
            result.put("transferId", id);
            result.put("message", "Transfer initiated and under review");
            sendJson(resp, 201, result);
        }
    }

    // ── PUT ────────────────────────────────────────────────────────────────────

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            LandTitle title = mapper.readValue(req.getInputStream(), LandTitle.class);

            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                title.setTitleNumber(pathInfo.substring(1));
            }

            registryBean.updateTitle(title);
            sendJson(resp, 200, singleMessage("Title updated successfully"));

        } catch (LandTitleException e) {
            sendError(resp, e.getHttpStatusCode(), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unhandled error in doPut", e);
            sendError(resp, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void sendJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            mapper.writeValue(out, payload);
        }
    }

    private void sendError(HttpServletResponse resp, int status, String code, String message)
            throws IOException {
        Map<String, String> err = new HashMap<>();
        err.put("errorCode", code);
        err.put("message", message);
        err.put("timestamp", java.time.Instant.now().toString());
        sendJson(resp, status, err);
    }

    private Map<String, String> singleMessage(String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("message", msg);
        m.put("timestamp", java.time.Instant.now().toString());
        return m;
    }

    /**
     * Get current authenticated username from Spring Security context.
     * Replaces HttpServletRequest.getUserPrincipal().
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get authenticated user from SecurityContext", e);
        }
        return "SYSTEM"; // Fallback
    }

    /**
     * Check if current user has a specific role using Spring Security.
     * Replaces HttpServletRequest.isUserInRole().
     */
    private boolean hasRole(String role) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role) || 
                                     auth.getAuthority().equals(role));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to check role from SecurityContext", e);
        }
        return false;
    }
}
