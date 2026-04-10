package com.trianz.ltr.controller;

import com.trianz.ltr.exception.LandTitleException;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.service.LandTitleRegistryService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LandTitleRegistryController - Cloud-ready REST API controller.
 *
 * CLOUD-READY FEATURES:
 *   - RESTful API design
 *   - JSON responses for cloud-native clients
 *   - Stateless request handling
 *   - Structured error responses
 *   - Compatible with API Gateway, Load Balancers
 *
 * ENDPOINTS:
 *   GET  /api/titles/{titleNumber}
 *   GET  /api/titles/parcel/{parcelId}
 *   GET  /api/titles/owner/{ownerId}
 *   POST /api/titles/register
 *   PUT  /api/titles/{titleNumber}
 *   GET  /api/transfers/{titleNumber}
 *   POST /api/transfers/initiate
 */
@WebServlet(name = "LandTitleRegistryController", urlPatterns = {"/api/*"})
public class LandTitleRegistryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LandTitleRegistryController.class.getName());
    
    private final LandTitleRegistryService service = new LandTitleRegistryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json;charset=UTF-8");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(resp, 400, "Invalid API endpoint");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            
            if (pathParts.length >= 3 && "titles".equals(pathParts[1])) {
                handleGetTitle(pathParts, req, resp);
            } else if (pathParts.length >= 3 && "transfers".equals(pathParts[1])) {
                handleGetTransfers(pathParts[2], resp);
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "API error", e);
            sendError(resp, 500, "Internal server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json;charset=UTF-8");
        
        try {
            if (pathInfo == null) {
                sendError(resp, 400, "Invalid API endpoint");
                return;
            }

            if (pathInfo.startsWith("/titles/register")) {
                handleRegisterTitle(req, resp);
            } else if (pathInfo.startsWith("/transfers/initiate")) {
                handleInitiateTransfer(req, resp);
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "API error", e);
            sendError(resp, 500, "Internal server error: " + e.getMessage());
        }
    }

    // ── Handler Methods ────────────────────────────────────────────────────────

    private void handleGetTitle(String[] pathParts, HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        try {
            if (pathParts.length == 3) {
                // GET /api/titles/{titleNumber}
                String titleNumber = pathParts[2];
                LandTitle title = service.getTitleByNumber(titleNumber);
                sendJsonResponse(resp, 200, toJson(title));
            } else if (pathParts.length == 4 && "parcel".equals(pathParts[2])) {
                // GET /api/titles/parcel/{parcelId}
                String parcelId = pathParts[3];
                LandTitle title = service.getTitleByParcelId(parcelId);
                sendJsonResponse(resp, 200, toJson(title));
            } else if (pathParts.length == 4 && "owner".equals(pathParts[2])) {
                // GET /api/titles/owner/{ownerId}
                String ownerId = pathParts[3];
                List<LandTitle> titles = service.getTitlesByOwner(ownerId);
                sendJsonResponse(resp, 200, toJsonArray(titles));
            } else {
                sendError(resp, 400, "Invalid title endpoint");
            }
        } catch (LandTitleException e) {
            sendError(resp, 404, e.getMessage());
        }
    }

    private void handleGetTransfers(String titleNumber, HttpServletResponse resp) throws IOException {
        try {
            List<TitleTransfer> transfers = service.getTransferHistory(titleNumber);
            sendJsonResponse(resp, 200, toJsonArray(transfers));
        } catch (LandTitleException e) {
            sendError(resp, 404, e.getMessage());
        }
    }

    private void handleRegisterTitle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // In production, parse JSON from request body
            String registeredBy = req.getParameter("registeredBy");
            if (registeredBy == null) registeredBy = "system";
            
            // Create sample title (in production, parse from JSON)
            LandTitle title = new LandTitle();
            title.setParcelId(req.getParameter("parcelId"));
            title.setOwnerNationalId(req.getParameter("ownerNationalId"));
            title.setOwnerFullName(req.getParameter("ownerFullName"));
            
            String titleNumber = service.registerTitle(title, registeredBy);
            sendJsonResponse(resp, 201, "{\"titleNumber\":\"" + titleNumber + "\",\"status\":\"registered\"}");
        } catch (LandTitleException e) {
            sendError(resp, 400, e.getMessage());
        }
    }

    private void handleInitiateTransfer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String initiatedBy = req.getParameter("initiatedBy");
            if (initiatedBy == null) initiatedBy = "system";
            
            // Create sample transfer (in production, parse from JSON)
            TitleTransfer transfer = new TitleTransfer();
            transfer.setTitleNumber(req.getParameter("titleNumber"));
            transfer.setNewOwnerNationalId(req.getParameter("newOwnerNationalId"));
            transfer.setNewOwnerName(req.getParameter("newOwnerName"));
            
            Long transferId = service.initiateTransfer(transfer, initiatedBy);
            sendJsonResponse(resp, 201, "{\"transferId\":" + transferId + ",\"status\":\"initiated\"}");
        } catch (LandTitleException e) {
            sendError(resp, 400, e.getMessage());
        }
    }

    // ── Response Helpers ───────────────────────────────────────────────────────

    private void sendJsonResponse(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        PrintWriter out = resp.getWriter();
        out.printf("{\"error\":\"%s\",\"status\":%d}", escapeJson(message), status);
        out.flush();
    }

    private String toJson(LandTitle title) {
        if (title == null) return "{}";
        return String.format("{\"titleNumber\":\"%s\",\"parcelId\":\"%s\",\"owner\":\"%s\",\"status\":\"%s\"}",
            title.getTitleNumber(), title.getParcelId(), title.getOwnerFullName(), title.getStatus());
    }

    private String toJsonArray(List<?> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            if (list.get(i) instanceof LandTitle) {
                sb.append(toJson((LandTitle) list.get(i)));
            } else if (list.get(i) instanceof TitleTransfer) {
                sb.append(toJson((TitleTransfer) list.get(i)));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJson(TitleTransfer transfer) {
        if (transfer == null) return "{}";
        return String.format("{\"transferId\":%d,\"titleNumber\":\"%s\",\"newOwner\":\"%s\",\"status\":\"%s\"}",
            transfer.getTransferId(), transfer.getTitleNumber(), 
            transfer.getNewOwnerName(), transfer.getTransferStatus());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
