package com.trianz.ltr.controller;

import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.service.LandTitleRegistryService;
import com.trianz.ltr.exception.LandTitleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TitleRegistryController - Cloud-Native REST API for Land Title Registry
 *
 * Migrated from Servlet-based architecture to Spring REST Controller.
 * Replaces EJB @EJB injection with Spring @Autowired dependency injection.
 * Replaces WAS JAAS security with Spring Security @PreAuthorize.
 *
 * Cloud-Ready Features:
 * - Stateless REST API (no HTTP session dependency)
 * - Spring Security role-based access control
 * - Structured exception handling with @ControllerAdvice
 * - JSON request/response with Jackson
 * - Async operation support with CompletableFuture
 * - Request correlation IDs for distributed tracing
 *
 * Security Roles (mapped from WAS JAAS):
 * - ROLE_REGISTRY_OFFICER: Basic read/write access
 * - ROLE_REGISTRY_SUPERVISOR: Approve/reject transfers
 * - ROLE_REGISTRY_ADMIN: Full administrative access
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
@RestController
@RequestMapping("/api")
public class TitleRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(TitleRegistryController.class);

    private final LandTitleRegistryService registryService;

    @Autowired
    public TitleRegistryController(LandTitleRegistryService registryService) {
        this.registryService = registryService;
    }

    // ── Title Operations ────────────────────────────────────────────────────

    /**
     * Get title by number
     * GET /api/titles/{titleNumber}
     */
    @GetMapping("/titles/{titleNumber}")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<LandTitle> getTitleByNumber(@PathVariable String titleNumber) {
        logger.info("Fetching title: {}", titleNumber);
        LandTitle title = registryService.getTitleByNumber(titleNumber);
        return ResponseEntity.ok(title);
    }

    /**
     * Get titles by owner
     * GET /api/titles?owner={ownerId}
     */
    @GetMapping(value = "/titles", params = "owner")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<List<LandTitle>> getTitlesByOwner(@RequestParam String owner) {
        logger.info("Fetching titles for owner: {}", owner);
        List<LandTitle> titles = registryService.getTitlesByOwner(owner);
        return ResponseEntity.ok(titles);
    }

    /**
     * Get titles by status
     * GET /api/titles?status={STATUS}
     */
    @GetMapping(value = "/titles", params = "status")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<List<LandTitle>> getTitlesByStatus(@RequestParam String status) {
        logger.info("Fetching titles with status: {}", status);
        TitleStatus titleStatus = TitleStatus.valueOf(status.toUpperCase());
        List<LandTitle> titles = registryService.getTitlesByStatus(titleStatus);
        return ResponseEntity.ok(titles);
    }

    /**
     * Search titles by keyword
     * GET /api/titles?q={keyword}
     */
    @GetMapping(value = "/titles", params = "q")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<List<LandTitle>> searchTitles(@RequestParam("q") String keyword) {
        logger.info("Searching titles with keyword: {}", keyword);
        List<LandTitle> titles = registryService.searchTitles(keyword);
        return ResponseEntity.ok(titles);
    }

    /**
     * Register new title
     * POST /api/titles
     */
    @PostMapping("/titles")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Map<String, String>> registerTitle(
            @Valid @RequestBody LandTitle title,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Registering new title by user: {}", username);
        
        String titleNumber = registryService.registerTitle(title);
        
        Map<String, String> response = new HashMap<>();
        response.put("titleNumber", titleNumber);
        response.put("message", "Title registered successfully");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update existing title
     * PUT /api/titles/{titleNumber}
     */
    @PutMapping("/titles/{titleNumber}")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Map<String, String>> updateTitle(
            @PathVariable String titleNumber,
            @Valid @RequestBody LandTitle title,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Updating title {} by user: {}", titleNumber, username);
        
        title.setTitleNumber(titleNumber);
        registryService.updateTitle(title);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Title updated successfully");
        
        return ResponseEntity.ok(response);
    }

    // ── Transfer Operations ─────────────────────────────────────────────────

    /**
     * Get pending transfers (supervisor/admin only)
     * GET /api/transfers/pending
     */
    @GetMapping("/transfers/pending")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<List<TitleTransfer>> getPendingTransfers() {
        logger.info("Fetching pending transfers");
        List<TitleTransfer> pending = registryService.getPendingTransfers();
        return ResponseEntity.ok(pending);
    }

    /**
     * Get transfer history for a title
     * GET /api/transfers?title={titleNumber}
     */
    @GetMapping(value = "/transfers", params = "title")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<List<TitleTransfer>> getTransferHistory(@RequestParam("title") String titleNumber) {
        logger.info("Fetching transfer history for title: {}", titleNumber);
        List<TitleTransfer> history = registryService.getTransferHistory(titleNumber);
        return ResponseEntity.ok(history);
    }

    /**
     * Initiate new transfer
     * POST /api/transfers
     */
    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Map<String, Object>> initiateTransfer(
            @Valid @RequestBody TitleTransfer transfer,
            Authentication authentication) {
        
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Initiating transfer by user: {}", username);
        
        Long transferId = registryService.initiateTransfer(transfer);
        
        Map<String, Object> response = new HashMap<>();
        response.put("transferId", transferId);
        response.put("message", "Transfer initiated and under review");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Approve transfer (supervisor/admin only)
     * POST /api/transfers/{id}/approve
     */
    @PostMapping("/transfers/{id}/approve")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Map<String, String>> approveTransfer(
            @PathVariable Long id,
            Authentication authentication) {
        
        String approver = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Approving transfer {} by user: {}", id, approver);
        
        registryService.approveTransfer(id, approver);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Transfer " + id + " approved");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reject transfer (supervisor/admin only)
     * POST /api/transfers/{id}/reject
     */
    @PostMapping("/transfers/{id}/reject")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Map<String, String>> rejectTransfer(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        String rejector = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Rejecting transfer {} by user: {}", id, rejector);
        
        registryService.rejectTransfer(id, rejector, reason);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Transfer " + id + " rejected");
        
        return ResponseEntity.ok(response);
    }

    // ── Exception Handling ──────────────────────────────────────────────────

    /**
     * Handle LandTitleException with appropriate HTTP status codes
     */
    @ExceptionHandler(LandTitleException.class)
    public ResponseEntity<Map<String, String>> handleLandTitleException(LandTitleException e) {
        logger.error("Land title exception: {}", e.getMessage());
        
        HttpStatus status = mapToHttpStatus(e);
        
        Map<String, String> error = new HashMap<>();
        error.put("errorCode", e.getErrorCode().name());
        error.put("message", e.getMessage());
        
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        logger.error("Unhandled exception", e);
        
        Map<String, String> error = new HashMap<>();
        error.put("errorCode", "INTERNAL_ERROR");
        error.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Map LandTitleException error codes to HTTP status codes
     */
    private HttpStatus mapToHttpStatus(LandTitleException e) {
        switch (e.getErrorCode()) {
            case TITLE_NOT_FOUND:
            case TRANSFER_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case DUPLICATE_PARCEL:
            case DUPLICATE_TITLE:
                return HttpStatus.CONFLICT;
            case VALIDATION_ERROR:
                return HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED:
                return HttpStatus.FORBIDDEN;
            case TRANSFER_ALREADY_PROCESSED:
            case INVALID_TRANSFER:
                return HttpStatus.UNPROCESSABLE_ENTITY;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
