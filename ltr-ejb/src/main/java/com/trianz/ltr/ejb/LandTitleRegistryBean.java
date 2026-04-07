package com.trianz.ltr.ejb;

import com.trianz.ltr.dao.LandTitleDAO;
import com.trianz.ltr.dao.TitleTransferDAO;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.model.TitleTransfer.TransferStatus;
import com.trianz.ltr.util.TitleNumberGenerator;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.*;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LandTitleRegistryBean - Stateless Session Bean implementing the Land Title
 * Registry business logic.
 *
 * WAS-SPECIFIC FEATURES DEMONSTRATED:
 *
 *   1. @Stateless EJB with both @Local and @Remote interfaces
 *   2. Container-Managed Transactions (CMT) — WAS manages XA transaction lifecycle
 *   3. @Resource injection of WAS SessionContext (gives access to caller principal,
 *      WAS security roles, and EJBContext methods)
 *   4. @RolesAllowed mapped to WAS security roles defined in ibm-application-bnd.xml
 *   5. EJBContext.getCallerPrincipal() — WAS LDAP/JAAS-authenticated user
 *   6. TransactionAttributeType.REQUIRED and REQUIRES_NEW — WAS XA coordination
 *
 * JNDI BINDINGS (ibm-ejb-jar-bnd.xml):
 *   Local  → ejblocal:LandTitleRegistryLocal
 *   Remote → ejb/LandTitleRegistryRemote
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * MODERNIZATION NOTE (Concierto Modernize):
 *   - Replace @Stateless + CMT with CDI @ApplicationScoped + @Transactional
 *   - Replace @RolesAllowed + WAS JAAS with MicroProfile JWT or OIDC
 *   - Replace SessionContext.getCallerPrincipal() with
 *     SecurityContext.getCallerPrincipal() (MicroProfile)
 *   - Remove ibm-ejb-jar-bnd.xml; use standard JNDI or CDI injection
 * ──────────────────────────────────────────────────────────────────────────────
 */
@Stateless(name = "LandTitleRegistry", mappedName = "ejb/LandTitleRegistry")
@TransactionManagement(TransactionManagementType.CONTAINER)   // WAS CMT
@RolesAllowed({"REGISTRY_OFFICER", "REGISTRY_SUPERVISOR", "REGISTRY_ADMIN"})
public class LandTitleRegistryBean
        implements LandTitleRegistryLocal, LandTitleRegistryRemote {

    private static final Logger LOGGER = Logger.getLogger(LandTitleRegistryBean.class.getName());

    /** WAS-specific: SessionContext injected by the WAS EJB container */
    @Resource
    private SessionContext sessionContext;   // WAS populates from JAAS/LDAP principal

    private final LandTitleDAO    titleDAO    = new LandTitleDAO();
    private final TitleTransferDAO transferDAO = new TitleTransferDAO();

    // ── Register a New Title ───────────────────────────────────────────────────

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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
            String titleNum = TitleNumberGenerator.generateLocal("REG");
            title.setTitleNumber(titleNum);
            title.setStatus(TitleStatus.ACTIVE);
            title.setRegistrationDate(new Date());

            // WAS-specific: get the authenticated principal name from JAAS
            String callerPrincipal = sessionContext.getCallerPrincipal().getName();
            title.setRegisteredBy(callerPrincipal);

            titleDAO.insert(title);
            LOGGER.info("Title registered: " + titleNum + " by " + callerPrincipal);
            return titleNum;

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "registerTitle failed", e);
            // WAS CMT: mark for rollback on unexpected errors
            sessionContext.setRollbackOnly();
            throw new LandTitleException(
                    LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "Failed to register title: " + e.getMessage(), e);
        }
    }

    // ── Retrieve Title ─────────────────────────────────────────────────────────

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<LandTitle> getTitlesByOwner(String ownerNationalId) throws LandTitleException {
        try {
            return titleDAO.findByOwner(ownerNationalId);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitlesByOwner failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<LandTitle> getTitlesByStatus(TitleStatus status) throws LandTitleException {
        try {
            return titleDAO.findByStatus(status);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTitlesByStatus failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateTitle(LandTitle title) throws LandTitleException {
        try {
            title.setLastModifiedDate(new Date());
            title.setLastModifiedBy(sessionContext.getCallerPrincipal().getName());
            int rows = titleDAO.update(title);
            if (rows == 0) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "No title updated: " + title.getTitleNumber());
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "updateTitle failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"REGISTRY_SUPERVISOR", "REGISTRY_ADMIN"})
    public void updateTitleStatus(String titleNumber, TitleStatus newStatus) throws LandTitleException {
        try {
            String caller = sessionContext.getCallerPrincipal().getName();
            int rows = titleDAO.updateStatus(titleNumber, newStatus, caller);
            if (rows == 0) throw new LandTitleException(
                    LandTitleException.ErrorCode.TITLE_NOT_FOUND,
                    "Title not found for status update: " + titleNumber);
            LOGGER.info("Status updated: " + titleNumber + " → " + newStatus + " by " + caller);
        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "updateTitleStatus failed", e);
        }
    }

    // ── Transfer Workflow ──────────────────────────────────────────────────────

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Long initiateTransfer(TitleTransfer transfer) throws LandTitleException {
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
            transfer.setInitiatedBy(sessionContext.getCallerPrincipal().getName());
            transfer.setTransferDate(new Date());

            // Mark title as PENDING while under review
            titleDAO.updateStatus(transfer.getTitleNumber(), TitleStatus.PENDING,
                    sessionContext.getCallerPrincipal().getName());

            Long id = transferDAO.insert(transfer);
            LOGGER.info("Transfer initiated: " + transfer.getTitleNumber() + " → " + transfer.getNewOwnerName());
            return id;

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "initiateTransfer failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)   // WAS: new XA transaction for approval
    @RolesAllowed({"REGISTRY_SUPERVISOR", "REGISTRY_ADMIN"})
    public void approveTransfer(Long transferId, String approvedByPrincipal) throws LandTitleException {
        try {
            List<TitleTransfer> list = transferDAO.findByTitleNumber("");  // find by id via pending list
            // Look up the specific transfer
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
            title.setLastModifiedDate(new Date());
            title.setLastModifiedBy(approvedByPrincipal);
            titleDAO.update(title);

            // Finalize transfer record
            transferDAO.updateStatus(transferId, TransferStatus.COMPLETED, approvedByPrincipal, null);

            LOGGER.info("Transfer " + transferId + " approved by " + approvedByPrincipal
                    + ". Title " + transfer.getTitleNumber() + " now owned by " + transfer.getNewOwnerName());

        } catch (LandTitleException e) {
            throw e;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "approveTransfer failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"REGISTRY_SUPERVISOR", "REGISTRY_ADMIN"})
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
            sessionContext.setRollbackOnly();
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "rejectTransfer failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TitleTransfer> getTransferHistory(String titleNumber) throws LandTitleException {
        try {
            return transferDAO.findByTitleNumber(titleNumber);
        } catch (Exception e) {
            throw new LandTitleException(LandTitleException.ErrorCode.DATA_ACCESS_ERROR,
                    "getTransferHistory failed", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed({"REGISTRY_SUPERVISOR", "REGISTRY_ADMIN"})
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
