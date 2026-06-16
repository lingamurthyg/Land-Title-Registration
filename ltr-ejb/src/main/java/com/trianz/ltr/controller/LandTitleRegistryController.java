package com.trianz.ltr.controller;

import com.trianz.ltr.ejb.LandTitleException;
import com.trianz.ltr.ejb.LandTitleRegistryLocal;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LandTitleRegistryController - REST API controller for Land Title Registry.
 *
 * CLOUD-NATIVE MIGRATION:
 *   - Replaces EJB Remote interface with REST APIs
 *   - HTTP/JSON communication instead of RMI-IIOP
 *   - Deployed behind AWS Application Load Balancer
 *   - API Gateway integration for external access
 *
 * AWS DEPLOYMENT:
 *   - Containerized in ECS/EKS with auto-scaling
 *   - Health checks via /actuator/health endpoint
 *   - Metrics exported to CloudWatch
 *   - Session state in ElastiCache (Redis)
 */
@RestController
@RequestMapping("/api/v1/titles")
public class LandTitleRegistryController {

    @Autowired
    private LandTitleRegistryLocal registryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<String> registerTitle(@RequestBody LandTitle title) {
        try {
            String titleNumber = registryService.registerTitle(title);
            return ResponseEntity.status(HttpStatus.CREATED).body(titleNumber);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{titleNumber}")
    public ResponseEntity<LandTitle> getTitleByNumber(@PathVariable String titleNumber) {
        try {
            LandTitle title = registryService.getTitleByNumber(titleNumber);
            return ResponseEntity.ok(title);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<LandTitle> getTitleByParcelId(@PathVariable String parcelId) {
        try {
            LandTitle title = registryService.getTitleByParcelId(parcelId);
            return ResponseEntity.ok(title);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/owner/{ownerNationalId}")
    public ResponseEntity<List<LandTitle>> getTitlesByOwner(@PathVariable String ownerNationalId) {
        try {
            List<LandTitle> titles = registryService.getTitlesByOwner(ownerNationalId);
            return ResponseEntity.ok(titles);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LandTitle>> getTitlesByStatus(@PathVariable TitleStatus status) {
        try {
            List<LandTitle> titles = registryService.getTitlesByStatus(status);
            return ResponseEntity.ok(titles);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<LandTitle>> searchTitles(@RequestParam String keyword) {
        try {
            List<LandTitle> titles = registryService.searchTitles(keyword);
            return ResponseEntity.ok(titles);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<String> updateTitle(@RequestBody LandTitle title) {
        try {
            registryService.updateTitle(title);
            return ResponseEntity.ok("Title updated successfully");
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{titleNumber}/status")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<String> updateTitleStatus(
            @PathVariable String titleNumber,
            @RequestParam TitleStatus newStatus) {
        try {
            registryService.updateTitleStatus(titleNumber, newStatus);
            return ResponseEntity.ok("Title status updated successfully");
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('REGISTRY_OFFICER', 'REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<Long> initiateTransfer(@RequestBody TitleTransfer transfer) {
        try {
            Long transferId = registryService.initiateTransfer(transfer);
            return ResponseEntity.status(HttpStatus.CREATED).body(transferId);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/transfers/{transferId}/approve")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<String> approveTransfer(
            @PathVariable Long transferId,
            @RequestParam String approvedBy) {
        try {
            registryService.approveTransfer(transferId, approvedBy);
            return ResponseEntity.ok("Transfer approved successfully");
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/transfers/{transferId}/reject")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<String> rejectTransfer(
            @PathVariable Long transferId,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        try {
            registryService.rejectTransfer(transferId, rejectedBy, reason);
            return ResponseEntity.ok("Transfer rejected successfully");
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{titleNumber}/transfers")
    public ResponseEntity<List<TitleTransfer>> getTransferHistory(@PathVariable String titleNumber) {
        try {
            List<TitleTransfer> transfers = registryService.getTransferHistory(titleNumber);
            return ResponseEntity.ok(transfers);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/transfers/pending")
    @PreAuthorize("hasAnyRole('REGISTRY_SUPERVISOR', 'REGISTRY_ADMIN')")
    public ResponseEntity<List<TitleTransfer>> getPendingTransfers() {
        try {
            List<TitleTransfer> transfers = registryService.getPendingTransfers();
            return ResponseEntity.ok(transfers);
        } catch (LandTitleException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
