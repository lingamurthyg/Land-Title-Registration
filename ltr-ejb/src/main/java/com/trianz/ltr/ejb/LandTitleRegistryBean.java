package com.trianz.ltr.ejb;

import com.trianz.ltr.dao.LandTitleDAO;
import com.trianz.ltr.dao.TitleTransferDAO;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.model.TitleTransfer.TransferStatus;
import com.trianz.ltr.util.TitleNumberGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * LandTitleRegistryBean - Spring Service implementing the Land Title Registry business logic.
 *
 * CLOUD-NATIVE MIGRATION:
 *   1. Migrated from @Stateless EJB to Spring @Service
 *   2. Replaced CMT with Spring @Transactional
 *   3. Replaced @Resource SessionContext with Spring SecurityContext
 *   4. @Secured replaces @RolesAllowed for Spring Security
 *   5. SecurityContextHolder.getContext().getAuthentication() replaces getCallerPrincipal()
 *   6. Propagation.REQUIRED and REQUIRES_NEW replace TransactionAttributeType
 *   7. Replaced java.util.Date with java.time.Instant for UTC standardization
 *   8. Compatible with AWS RDS, ElastiCache, and cloud-native patterns
 *   9. Stateless design for horizontal scaling in EKS/ECS
 */
@Service
@Transactional
@Secured({"ROLE_REGISTRY_OFFICER", "ROLE_REGISTRY_SUPERVISOR", "ROLE_REGISTRY_ADMIN"})
public class LandTitleRegistryBean
        implements LandTitleRegistryLocal, LandTitleRegistryRemote {

    private static final Logger LOGGER = Logger.getLogger(LandTitleRegistryBean.class.getName());

    @Autowired
    private LandTitleDAO titleDAO;
    
    @Autowired
    private TitleTransferDAO transferDAO;

    // ── Register a New Title ───────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String registerTitle(LandTitle title) throws LandTitleException {

        validateTitle(title);

        try {
            // Check for duplicate parcel
            if (titleDAO.findByParcelId(title.getParcelId()) != null) {
                throw new LandTitleException(
                        LandTitleException.ErrorCode.DUPLICATE_PARCEL,
                        "Parcel already registered: " + title.getParcelId());
            }

            // Generate title number
            String titleNum = TitleNumberGenerator.generateLocal("NRB");
            title.setTitleNumber(titleNum);
            title.setStatus(TitleStatus.PENDING);
            title.setRegistrationDate(Instant.now());
            
            // Spring Security: get the authenticated principal name
            String callerPrincipal = getCurrentUsername();
            title.setRegisteredBy(callerPrincipal);

            titleDAO.insert(title);
            LOGGER.info("Title registered: " + titleNum + " by " + callerPrincipal);
            return titleNum;

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(
                    LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "registerTitle failed", e);
        }
    }

    // ── Retrieve Title ─────────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public LandTitle getTitleByNumber(String titleNumber) throws LandTitleException {
        try {
            LandTitle t = titleDAO.findByTitleNumber(titleNumber);
            if (t == null) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "Title not found: " + titleNumber);
            return t;
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitleByNumber failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public LandTitle getTitleByParcelId(String parcelId) throws LandTitleException {
        try {
            LandTitle t = titleDAO.findByParcelId(parcelId);
            if (t == null) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND, "Parcel not found: " + parcelId);
            return t;
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitleByParcelId failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<LandTitle> getTitlesByOwner(String ownerNationalId) throws LandTitleException {
        try {
            return titleDAO.findByOwner(ownerNationalId);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitlesByOwner failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<LandTitle> getTitlesByStatus(TitleStatus status) throws LandTitleException {
        try {
            return titleDAO.findByStatus(status);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitlesByStatus failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<LandTitle> searchTitles(String keyword) throws LandTitleException {
        try {
            return titleDAO.search(keyword);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "searchTitles failed", e);
        }
    }

    // ── Update Title ───────────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateTitle(LandTitle title) throws LandTitleException {
        try {
            title.setLastModifiedDate(Instant.now());
            title.setLastModifiedBy(getCurrentUsername());
            
            int rows = titleDAO.update(title);
            if (rows == 0) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "No title updated: " + title.getTitleNumber());
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "updateTitle failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @Secured({"ROLE_REGISTRY_SUPERVISOR", "ROLE_REGISTRY_ADMIN"})
    public void updateTitleStatus(String titleNumber, TitleStatus newStatus) throws LandTitleException {
        try {
            String caller = getCurrentUsername();
            int rows = titleDAO.updateStatus(titleNumber, newStatus, caller);
            if (rows == 0) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "Title not found: " + titleNumber);
            LOGGER.info("Title " + titleNumber + " status changed to " + newStatus + " by " + caller);
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "updateTitleStatus failed", e);
        }
    }

    // ── Transfer Management ────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Long initiateTransfer(TitleTransfer transfer) throws LandTitleException {
        try {
            // Validate title exists and is ACTIVE
            LandTitle existing = titleDAO.findByTitleNumber(transfer.getTitleNumber());
            if (existing == null) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "Title not found: " + transfer.getTitleNumber());
            if (existing.getStatus() != TitleStatus.ACTIVE) throw new LandTitleException(
                    LandTitleException.ErrorCode.INVALID_TRANSFER,
                    "Title is not ACTIVE. Current status: " + existing.getStatus());

            // Capture current owner details
            transfer.setPreviousOwnerNationalId(existing.getOwnerNationalId());
            transfer.setPreviousOwnerName(existing.getOwnerFullName());
            transfer.setTransferStatus(TransferStatus.UNDER_REVIEW);
            transfer.setInitiatedBy(getCurrentUsername());
            transfer.setTransferDate(Instant.now());

            // Set title to PENDING during review
            titleDAO.updateStatus(transfer.getTitleNumber(), TitleStatus.PENDING,
                    getCurrentUsername());

            Long id = transferDAO.insert(transfer);
            LOGGER.info("Transfer initiated: " + id + " for title " + transfer.getTitleNumber());
            return id;

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "initiateTransfer failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Secured({"ROLE_REGISTRY_SUPERVISOR", "ROLE_REGISTRY_ADMIN"})
    public void approveTransfer(Long transferId, String approvedByPrincipal) throws LandTitleException {
        try {
            TitleTransfer transfer = findTransferById(transferId);
            if (transfer.getTransferStatus() != TransferStatus.UNDER_REVIEW) {
                throw new LandTitleException(
                        LandTitleException.ErrorCode.TRANSFER_ALREADY_PROCESSED,
                        "Transfer " + transferId + " is already " + transfer.getTransferStatus());
            }

            // Update title with new owner — core registry write
            LandTitle title = titleDAO.findByTitleNumber(transfer.getTitleNumber());
            title.setOwnerNationalId(transfer.getNewOwnerNationalId());
            title.setOwnerFullName(transfer.getNewOwnerName());
            title.setOwnerContactEmail(transfer.getNewOwnerContactEmail());
            title.setOwnerContactPhone(transfer.getNewOwnerContactPhone());
            title.setStatus(TitleStatus.ACTIVE);
            title.setLastModifiedDate(Instant.now());
            title.setLastModifiedBy(approvedByPrincipal);
            titleDAO.update(title);

            // Mark transfer as APPROVED
            transferDAO.updateStatus(transferId, TransferStatus.APPROVED, approvedByPrincipal, null);

            LOGGER.info("Transfer " + transferId + " approved by " + approvedByPrincipal
                    + ". Title " + transfer.getTitleNumber() + " now owned by " + transfer.getNewOwnerName());

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "approveTransfer failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @Secured({"ROLE_REGISTRY_SUPERVISOR", "ROLE_REGISTRY_ADMIN"})
    public void rejectTransfer(Long transferId, String rejectedByPrincipal, String reason)
            throws LandTitleException {
        try {
            TitleTransfer transfer = findTransferById(transferId);
            if (transfer.getTransferStatus() != TransferStatus.UNDER_REVIEW) {
                throw new LandTitleException(
                        LandTitleException.ErrorCode.TRANSFER_ALREADY_PROCESSED,
                        "Transfer " + transferId + " is already " + transfer.getTransferStatus());
            }
            // Revert title to ACTIVE
            titleDAO.updateStatus(transfer.getTitleNumber(), TitleStatus.ACTIVE, rejectedByPrincipal);
            transferDAO.updateStatus(transferId, TransferStatus.REJECTED, rejectedByPrincipal, reason);
            LOGGER.info("Transfer " + transferId + " rejected by " + rejectedByPrincipal);
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "rejectTransfer failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<TitleTransfer> getTransferHistory(String titleNumber) throws LandTitleException {
        try {
            return transferDAO.findByTitleNumber(titleNumber);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTransferHistory failed", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Secured({"ROLE_REGISTRY_SUPERVISOR", "ROLE_REGISTRY_ADMIN"})
    public List<TitleTransfer> getPendingTransfers() throws LandTitleException {
        try {
            return transferDAO.findPendingApprovals();
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getPendingTransfers failed", e);
        }
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private void validateTitle(LandTitle t) throws LandTitleException {
        if (t == null) throw new LandTitleException(
                LandTitleException.ErrorCode.VALIDATION_ERROR, "Title object is null");
        if (isBlank(t.getParcelId())) throw new LandTitleException(
                LandTitleException.ErrorCode.VALIDATION_ERROR, "Parcel ID is required");
        if (isBlank(t.getOwnerNationalId())) throw new LandTitleException(
                LandTitleException.ErrorCode.VALIDATION_ERROR, "Owner National ID is required");
        if (isBlank(t.getOwnerFullName())) throw new LandTitleException(
                LandTitleException.ErrorCode.VALIDATION_ERROR, "Owner full name is required");
    }

    private TitleTransfer findTransferById(Long transferId) throws LandTitleException {
        // Workaround: scan pending and all lists — in production use a direct-by-id query
        try {
            List<TitleTransfer> pending = transferDAO.findPendingApprovals();
            for (TitleTransfer tr : pending) {
                if (transferId.equals(tr.getTransferId())) return tr;
            }
            throw new LandTitleException(LandTitleException.ErrorCode.TRANSFER_NOT_FOUND,
                    "Transfer not found: " + transferId);
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "findTransferById failed", e);
        }
    }

    /**
     * Get current authenticated username from Spring Security context.
     */
    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
