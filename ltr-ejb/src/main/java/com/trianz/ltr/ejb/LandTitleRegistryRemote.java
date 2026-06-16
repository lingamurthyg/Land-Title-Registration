package com.trianz.ltr.ejb;

import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;

import java.util.List;

/**
 * LandTitleRegistryRemote - Remote Business Interface (exposed via REST API).
 * 
 * CLOUD-NATIVE MIGRATION:
 *   - Removed EJB 2.x @Remote annotation
 *   - Standard Java interface for Spring Boot microservices
 *   - In cloud-native architecture, this interface is exposed via Spring REST controllers
 *   - Compatible with AWS API Gateway and Application Load Balancer
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
     * Approve a pending transfer — updates title ownership in a single XA transaction.
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
