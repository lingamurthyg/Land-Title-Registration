package com.trianz.ltr.service;

import com.trianz.ltr.exception.LandTitleException;
import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;

import java.util.List;

/**
 * LandTitleRegistryService - Service interface for Land Title operations
 *
 * Replaces EJB Local/Remote interfaces with Spring Service interface.
 * Provides business logic layer for cloud-native architecture.
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
public interface LandTitleRegistryService {

    /**
     * Register a new land title
     */
    String registerTitle(LandTitle title) throws LandTitleException;

    /**
     * Update an existing land title
     */
    void updateTitle(LandTitle title) throws LandTitleException;

    /**
     * Get title by title number
     */
    LandTitle getTitleByNumber(String titleNumber) throws LandTitleException;

    /**
     * Get all titles owned by a specific owner
     */
    List<LandTitle> getTitlesByOwner(String ownerId) throws LandTitleException;

    /**
     * Get all titles with a specific status
     */
    List<LandTitle> getTitlesByStatus(TitleStatus status) throws LandTitleException;

    /**
     * Search titles by keyword
     */
    List<LandTitle> searchTitles(String keyword) throws LandTitleException;

    /**
     * Initiate a title transfer
     */
    Long initiateTransfer(TitleTransfer transfer) throws LandTitleException;

    /**
     * Approve a pending transfer
     */
    void approveTransfer(Long transferId, String approver) throws LandTitleException;

    /**
     * Reject a pending transfer
     */
    void rejectTransfer(Long transferId, String rejector, String reason) throws LandTitleException;

    /**
     * Get all pending transfers
     */
    List<TitleTransfer> getPendingTransfers() throws LandTitleException;

    /**
     * Get transfer history for a title
     */
    List<TitleTransfer> getTransferHistory(String titleNumber) throws LandTitleException;
}
