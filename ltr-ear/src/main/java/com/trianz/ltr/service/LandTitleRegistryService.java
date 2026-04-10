package com.trianz.ltr.service;

import com.trianz.ltr.dao.LandTitleDAO;
import com.trianz.ltr.dao.TitleTransferDAO;
import com.trianz.ltr.exception.LandTitleException;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.model.TitleTransfer.TransferStatus;
import com.trianz.ltr.util.TitleNumberGenerator;
import com.trianz.ltr.util.CloudTransactionUtil;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LandTitleRegistryService - Cloud-native service layer for Land Title Registry.
 *
 * CLOUD-READY FEATURES:
 *   - Uses standard JTA @Transactional (no vendor lock-in)
 *   - Stateless design for horizontal scaling
 *   - No EJB dependencies (can run in Spring Boot, Quarkus, Micronaut)
 *   - Structured logging for cloud monitoring
 *   - UTC timestamp handling
 *   - Compatible with AWS ECS, EKS, Lambda
 */
public class LandTitleRegistryService {

    private static final Logger LOGGER = Logger.getLogger(LandTitleRegistryService.class.getName());

    private final LandTitleDAO titleDAO = new LandTitleDAO();
    private final TitleTransferDAO transferDAO = new TitleTransferDAO();

    // ── Register a New Title ───────────────────────────────────────────────────

    @Transactional
    public String registerTitle(LandTitle title, String registeredBy) throws LandTitleException {
        CloudTransactionUtil.logTransactionStart("registerTitle");
        
        validateTitle(title);

        try {
            // Check for duplicate parcel
            if (titleDAO.findByParcelId(title.getParcelId()) != null) {
                throw new LandTitleException(
                        LandTitleException.ErrorCode.DUPLICATE_PARCEL,
                        "Parcel already registered: " + title.getParcelId());
            }

            // Generate title number
            String titleNum = TitleNumberGenerator.generateLocal("REG");
            title.setTitleNumber(titleNum);
            title.setStatus(TitleStatus.ACTIVE);
            title.setRegistrationDate(Instant.now());
            title.setRegisteredBy(registeredBy);

            titleDAO.insert(title);
            
            CloudTransactionUtil.logTransactionCommit("registerTitle");
            LOGGER.info("Title registered: " + titleNum + " by " + registeredBy);
            return titleNum;

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "registerTitle failed", e);
            CloudTransactionUtil.logTransactionRollback("registerTitle", e);
            throw new LandTitleException(
                    LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "Failed to register title: " + e.getMessage(), e);
        }
    }

    // ── Retrieve Title ─────────────────────────────────────────────────────────

    public LandTitle getTitleByNumber(String titleNumber) throws LandTitleException {
        try {
            LandTitle t = titleDAO.findByTitleNumber(titleNumber);
            if (t == null) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND, "Title not found: " + titleNumber);
            return t;
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitleByNumber failed", e);
        }
    }

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

    public List<LandTitle> getTitlesByOwner(String ownerNationalId) throws LandTitleException {
        try {
            return titleDAO.findByOwner(ownerNationalId);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitlesByOwner failed", e);
        }
    }

    public List<LandTitle> getTitlesByStatus(TitleStatus status) throws LandTitleException {
        try {
            return titleDAO.findByStatus(status);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitlesByStatus failed", e);
        }
    }

    public List<LandTitle> searchTitles(String keyword) throws LandTitleException {
        try {
            return titleDAO.search(keyword);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "searchTitles failed", e);
        }
    }

    // ── Update Title ───────────────────────────────────────────────────────────

    @Transactional
    public void updateTitle(LandTitle title, String modifiedBy) throws LandTitleException {
        CloudTransactionUtil.logTransactionStart("updateTitle");
        
        try {
            title.setLastModifiedDate(Instant.now());
            title.setLastModifiedBy(modifiedBy);
            int rows = titleDAO.update(title);
            if (rows == 0) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "No title updated: " + title.getTitleNumber());
            
            CloudTransactionUtil.logTransactionCommit("updateTitle");
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            CloudTransactionUtil.logTransactionRollback("updateTitle", e);
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "updateTitle failed", e);
        }
    }

    @Transactional
    public void updateTitleStatus(String titleNumber, TitleStatus newStatus, String modifiedBy) 
            throws LandTitleException {
        CloudTransactionUtil.logTransactionStart("updateTitleStatus");
        
        try {
            int rows = titleDAO.updateStatus(titleNumber, newStatus, modifiedBy);
            if (rows == 0) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "Title not found for status update: " + titleNumber);
            
            CloudTransactionUtil.logTransactionCommit("updateTitleStatus");
            LOGGER.info("Status updated: " + titleNumber + " → " + newStatus + " by " + modifiedBy);
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            CloudTransactionUtil.logTransactionRollback("updateTitleStatus", e);
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "updateTitleStatus failed", e);
        }
    }

    // ── Transfer Workflow ──────────────────────────────────────────────────────

    @Transactional
    public Long initiateTransfer(TitleTransfer transfer, String initiatedBy) throws LandTitleException {
        CloudTransactionUtil.logTransactionStart("initiateTransfer");
        
        try {
            // Validate source title exists and is ACTIVE
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
            transfer.setInitiatedBy(initiatedBy);
            transfer.setTransferDate(Instant.now());

            // Mark title as PENDING while under review
            titleDAO.updateStatus(transfer.getTitleNumber(), TitleStatus.PENDING, initiatedBy);

            Long id = transferDAO.insert(transfer);
            
            CloudTransactionUtil.logTransactionCommit("initiateTransfer");
            LOGGER.info("Transfer initiated: " + transfer.getTitleNumber() + " → " + transfer.getNewOwnerName());
            return id;

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            CloudTransactionUtil.logTransactionRollback("initiateTransfer", e);
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "initiateTransfer failed", e);
        }
    }

    @Transactional
    public void approveTransfer(Long transferId, String approvedBy) throws LandTitleException {
        CloudTransactionUtil.logTransactionStart("approveTransfer");
        
        try {
            TitleTransfer transfer = findTransferById(transferId);

            if (transfer.getTransferStatus() != TransferStatus.UNDER_REVIEW) {
                throw new LandTitleException(
                        LandTitleException.ErrorCode.TRANSFER_ALREADY_PROCESSED,
                        "Transfer " + transferId + " is already " + transfer.getTransferStatus());
            }

            // Update title with new owner
            LandTitle title = titleDAO.findByTitleNumber(transfer.getTitleNumber());
            title.setOwnerNationalId(transfer.getNewOwnerNationalId());
            title.setOwnerFullName(transfer.getNewOwnerName());
            title.setOwnerContactEmail(transfer.getNewOwnerContactEmail());
            title.setOwnerContactPhone(transfer.getNewOwnerContactPhone());
            title.setStatus(TitleStatus.ACTIVE);
            title.setLastModifiedDate(Instant.now());
            title.setLastModifiedBy(approvedBy);
            titleDAO.update(title);

            // Finalize transfer record
            transferDAO.updateStatus(transferId, TransferStatus.COMPLETED, approvedBy, null);

            CloudTransactionUtil.logTransactionCommit("approveTransfer");
            LOGGER.info("Transfer " + transferId + " approved by " + approvedBy
                    + ". Title " + transfer.getTitleNumber() + " now owned by " + transfer.getNewOwnerName());

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            CloudTransactionUtil.logTransactionRollback("approveTransfer", e);
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "approveTransfer failed", e);
        }
    }

    @Transactional
    public void rejectTransfer(Long transferId, String rejectedBy, String reason)
            throws LandTitleException {
        CloudTransactionUtil.logTransactionStart("rejectTransfer");
        
        try {
            TitleTransfer transfer = findTransferById(transferId);
            if (transfer.getTransferStatus() != TransferStatus.UNDER_REVIEW) {
                throw new LandTitleException(
                        LandTitleException.ErrorCode.TRANSFER_ALREADY_PROCESSED,
                        "Transfer " + transferId + " is already " + transfer.getTransferStatus());
            }
            // Revert title to ACTIVE
            titleDAO.updateStatus(transfer.getTitleNumber(), TitleStatus.ACTIVE, rejectedBy);
            transferDAO.updateStatus(transferId, TransferStatus.REJECTED, rejectedBy, reason);
            
            CloudTransactionUtil.logTransactionCommit("rejectTransfer");
            LOGGER.info("Transfer " + transferId + " rejected by " + rejectedBy);
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            CloudTransactionUtil.logTransactionRollback("rejectTransfer", e);
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "rejectTransfer failed", e);
        }
    }

    public List<TitleTransfer> getTransferHistory(String titleNumber) throws LandTitleException {
        try {
            return transferDAO.findByTitleNumber(titleNumber);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTransferHistory failed", e);
        }
    }

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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
