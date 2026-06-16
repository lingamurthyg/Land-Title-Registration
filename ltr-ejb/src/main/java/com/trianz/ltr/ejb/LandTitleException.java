package com.trianz.ltr.ejb;

/**
 * LandTitleException - Checked application exception for the registry service.
 * 
 * CLOUD-NATIVE MIGRATION:
 *   - Removed EJB 2.x ApplicationException
 *   - Standard Java exception for Spring Boot microservices
 *   - Spring @Transactional handles rollback based on exception type configuration
 */
public class LandTitleException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum ErrorCode {
        TITLE_NOT_FOUND,
        DUPLICATE_PARCEL,
        DUPLICATE_TITLE,
        INVALID_TRANSFER,
        TRANSFER_NOT_FOUND,
        TRANSFER_ALREADY_PROCESSED,
        VALIDATION_ERROR,
        DATA_ACCESS_ERROR,
        UNAUTHORIZED
    }

    private final ErrorCode errorCode;

    public LandTitleException(ErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }

    public LandTitleException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = code;
    }

    public ErrorCode getErrorCode() { return errorCode; }

    @Override
    public String toString() {
        return "LandTitleException[" + errorCode + "]: " + getMessage();
    }
}
