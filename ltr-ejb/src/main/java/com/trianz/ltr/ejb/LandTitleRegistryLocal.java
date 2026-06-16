package com.trianz.ltr.ejb;

import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.TitleTransfer;

import java.util.List;

/**
 * LandTitleRegistryLocal - Service Business Interface.
 * 
 * CLOUD-NATIVE MIGRATION:
 *   - Removed @Local annotation (EJB-specific)
 *   - Standard Java interface for Spring services
 *   - Used by REST controllers and other services within the same application
 */
public interface LandTitleRegistryLocal {

    String registerTitle(LandTitle title) throws LandTitleException;
    LandTitle getTitleByNumber(String titleNumber) throws LandTitleException;
    LandTitle getTitleByParcelId(String parcelId) throws LandTitleException;
    List<LandTitle> getTitlesByOwner(String ownerNationalId) throws LandTitleException;
    List<LandTitle> getTitlesByStatus(TitleStatus status) throws LandTitleException;
    List<LandTitle> searchTitles(String keyword) throws LandTitleException;
    void updateTitle(LandTitle title) throws LandTitleException;
    void updateTitleStatus(String titleNumber, TitleStatus newStatus) throws LandTitleException;
    Long initiateTransfer(TitleTransfer transfer) throws LandTitleException;
    void approveTransfer(Long transferId, String approvedByPrincipal) throws LandTitleException;
    void rejectTransfer(Long transferId, String rejectedByPrincipal, String reason) throws LandTitleException;
    List<TitleTransfer> getTransferHistory(String titleNumber) throws LandTitleException;
    List<TitleTransfer> getPendingTransfers() throws LandTitleException;
}
