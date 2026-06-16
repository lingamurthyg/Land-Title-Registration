package com.trianz.ltr.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trianz.ltr.ejb.LandTitleException;
import com.trianz.ltr.ejb.LandTitleRegistryLocal;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TitleRegistryServlet - Front-controller servlet for the Land Title Registry.
 *
 * CLOUD-NATIVE MIGRATION:
 *   - Replaced @EJB with Spring @Autowired for dependency injection
 *   - HttpServletRequest.getUserPrincipal() returns Spring Security authenticated user
 *   - HttpServletRequest.isUserInRole() checks Spring Security roles
 *   - Migrated from WebSphere EJB container to Spring Boot microservices
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
 *
 * NOTE: Consider migrating to Spring REST Controllers (@RestController) for better cloud-native patterns.
 */
@WebServlet(name = "TitleRegistryServlet", urlPatterns = {"/api/titles/*", "/api/transfers/*"})
@Controller
public class TitleRegistryServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TitleRegistryServlet.class.getName());
    private static final long serialVersionUID = 1L;

    /** Spring dependency injection replaces EJB @EJB injection */
    @Autowired
    private LandTitleRegistryLocal registryBean;

    private ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.findAndRegisterModules();
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
            sendError(resp, mapStatusCode(e), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in doGet", e);
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
            // Only supervisors/admins
            if (!req.isUserInRole("REGISTRY_SUPERVISOR") && !req.isUserInRole("REGISTRY_ADMIN")) {
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
            sendError(resp, mapStatusCode(e), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in doPost", e);
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
            String approver = req.getUserPrincipal() != null
                    ? req.getUserPrincipal().getName() : "SYSTEM";
            registryBean.approveTransfer(transferId, approver);
            sendJson(resp, 200, singleMessage("Transfer " + transferId + " approved"));

        } else if (pathInfo != null && pathInfo.matches("/\\d+/reject")) {
            Long transferId = Long.parseLong(pathInfo.split("/")[1]);
            String reason = req.getParameter("reason");
            String rejector = req.getUserPrincipal() != null
                    ? req.getUserPrincipal().getName() : "SYSTEM";
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
            sendError(resp, mapStatusCode(e), e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in doPut", e);
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
        sendJson(resp, status, err);
    }

    private Map<String, String> singleMessage(String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("message", msg);
        return m;
    }

    private int mapStatusCode(LandTitleException e) {
        switch (e.getErrorCode()) {
            case TITLE_NOT_FOUND:
            case TRANSFER_NOT_FOUND:    return 404;
            case DUPLICATE_PARCEL:
            case DUPLICATE_TITLE:       return 409;
            case VALIDATION_ERROR:      return 400;
            case UNAUTHORIZED:          return 403;
            case TRANSFER_ALREADY_PROCESSED:
            case INVALID_TRANSFER:      return 422;
            default:                    return 500;
        }
    }
}
