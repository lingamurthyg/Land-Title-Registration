package com.trianz.ltr.controller;

import com.trianz.ltr.ejb.LandTitleException;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.service.LandTitleRegistryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LandTitleRegistryController - REST API controller for Land Title Registry.
 * 
 * Replaces EJB Remote interface with RESTful HTTP endpoints:
 * - No RMI-IIOP dependency
 * - Standard HTTP/JSON communication
 * - Cloud-native API design
 * - Compatible with API Gateway, Load Balancers
 * - Supports AWS ALB, CloudFront, API Gateway integration
 */
@RestController
@RequestMapping("/api/v1/titles")
public class LandTitleRegistryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LandTitleRegistryController.class);

    @Autowired
    private LandTitleRegistryService registryService;

    // ── Register Title ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Map<String, String>> registerTitle(@RequestBody LandTitle title) {
        try {
            String titleNumber = registryService.registerTitle(title);
            Map<String, String> response = new HashMap<>();
            response.put("titleNumber", titleNumber);
            response.put("message", "Title registered successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LandTitleException e) {
            LOGGER.error("Failed to register title", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    // ── Retrieve Title ─────────────────────────────────────────────────────────

    @GetMapping("/{titleNumber}")
    public ResponseEntity<?> getTitleByNumber(@PathVariable String titleNumber) {
        try {
            LandTitle title = registryService.getTitleByNumber(titleNumber);
            return ResponseEntity.ok(title);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<?> getTitleByParcelId(@PathVariable String parcelId) {
        try {
            LandTitle title = registryService.getTitleByParcelId(parcelId);
            return ResponseEntity.ok(title);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @GetMapping("/owner/{ownerNationalId}")
    public ResponseEntity<?> getTitlesByOwner(@PathVariable String ownerNationalId) {
        try {
            List<LandTitle> titles = registryService.getTitlesByOwner(ownerNationalId);
            return ResponseEntity.ok(titles);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getTitlesByStatus(@PathVariable TitleStatus status) {
        try {
            List<LandTitle> titles = registryService.getTitlesByStatus(status);
            return ResponseEntity.ok(titles);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTitles(@RequestParam String keyword) {
        try {
            List<LandTitle> titles = registryService.searchTitles(keyword);
            return ResponseEntity.ok(titles);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    // ── Update Title ───────────────────────────────────────────────────────────

    @PutMapping("/{titleNumber}")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<?> updateTitle(@PathVariable String titleNumber, @RequestBody LandTitle title) {
        try {
            title.setTitleNumber(titleNumber);
            registryService.updateTitle(title);
            return ResponseEntity.ok(Map.of("message", "Title updated successfully"));
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @PatchMapping("/{titleNumber}/status")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<?> updateTitleStatus(@PathVariable String titleNumber, 
                                               @RequestParam TitleStatus status) {
        try {
            registryService.updateTitleStatus(titleNumber, status);
            return ResponseEntity.ok(Map.of("message", "Title status updated successfully"));
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    // ── Transfer Workflow ──────────────────────────────────────────────────────

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<?> initiateTransfer(@RequestBody TitleTransfer transfer) {
        try {
            Long transferId = registryService.initiateTransfer(transfer);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("transferId", transferId, "message", "Transfer initiated successfully"));
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @PostMapping("/transfers/{transferId}/approve")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<?> approveTransfer(@PathVariable Long transferId, 
                                            @RequestParam String approvedBy) {
        try {
            registryService.approveTransfer(transferId, approvedBy);
            return ResponseEntity.ok(Map.of("message", "Transfer approved successfully"));
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @PostMapping("/transfers/{transferId}/reject")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<?> rejectTransfer(@PathVariable Long transferId,
                                           @RequestParam String rejectedBy,
                                           @RequestParam String reason) {
        try {
            registryService.rejectTransfer(transferId, rejectedBy, reason);
            return ResponseEntity.ok(Map.of("message", "Transfer rejected successfully"));
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @GetMapping("/{titleNumber}/transfers")
    public ResponseEntity<?> getTransferHistory(@PathVariable String titleNumber) {
        try {
            List<TitleTransfer> transfers = registryService.getTransferHistory(titleNumber);
            return ResponseEntity.ok(transfers);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }

    @GetMapping("/transfers/pending")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<?> getPendingTransfers() {
        try {
            List<TitleTransfer> transfers = registryService.getPendingTransfers();
            return ResponseEntity.ok(transfers);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getErrorCode().name(), "message", e.getMessage()));
        }
    }
}
