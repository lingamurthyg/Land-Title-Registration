package com.trianz.ltr.ejb;

import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;

import java.util.List;

/**
 * LandTitleRegistryRemote - Remote service interface for Land Title Registry.
 *
 * Cloud-native migration:
 * - Removed @Remote EJB annotation (not needed in cloud environments)
 * - In cloud deployments, expose via REST API instead of RMI-IIOP
 * - Use Spring @RestController with this interface as service layer
 * - Compatible with AWS API Gateway, Azure API Management, GCP Cloud Endpoints
 *
 * MIGRATION PATH:
 *   - Replace RMI-IIOP remote calls with REST API calls
 *   - Use Spring Cloud OpenFeign for service-to-service communication
 *   - Implement REST controllers that delegate to this service interface
 *   - Use AWS ALB/NLB for load balancing instead of WAS clustering
 */
public interface LandTitleRegistryRemote {

    /**
     * Register a new land title into the registry.
     * @return the assigned title number
     */
    String registerTitle(LandTitle title) throws LandTitleException;

    /**
     * Retrieve a title by its unique title number.
     */
    LandTitle getTitleByNumber(String titleNumber) throws LandTitleException;

    /**
     * Retrieve a title by its parcel/survey ID.
     */
    LandTitle getTitleByParcelId(String parcelId) throws LandTitleException;

    /**
     * Retrieve all titles owned by a given national ID.
     */
    List<LandTitle> getTitlesByOwner(String ownerNationalId) throws LandTitleException;

    /**
     * Retrieve titles by registry status.
     */
    List<LandTitle> getTitlesByStatus(TitleStatus status) throws LandTitleException;

    /**
     * Full-text search across owner name, city, and parcel ID.
     */
    List<LandTitle> searchTitles(String keyword) throws LandTitleException;

    /**
     * Update mutable fields of an existing title (valuation, encumbrance, remarks).
     */
    void updateTitle(LandTitle title) throws LandTitleException;

    /**
     * Change the registration status of a title (e.g. ACTIVE → ENCUMBERED).
     */
    void updateTitleStatus(String titleNumber, TitleStatus newStatus) throws LandTitleException;

    /**
     * Initiate a title transfer (ownership change).
     * Creates a transfer record and sets the title to PENDING.
     * @return the generated transfer ID
     */
    Long initiateTransfer(TitleTransfer transfer) throws LandTitleException;

    /**
     * Approve a pending transfer — updates title ownership in a single transaction.
     */
    void approveTransfer(Long transferId, String approvedByPrincipal) throws LandTitleException;

    /**
     * Reject a pending transfer with a reason.
     */
    void rejectTransfer(Long transferId, String rejectedByPrincipal,
                        String reason) throws LandTitleException;

    /**
     * Retrieve the full chain of ownership for a title.
     */
    List<TitleTransfer> getTransferHistory(String titleNumber) throws LandTitleException;

    /**
     * Retrieve all transfers awaiting approval (UNDER_REVIEW).
     */
    List<TitleTransfer> getPendingTransfers() throws LandTitleException;
}
