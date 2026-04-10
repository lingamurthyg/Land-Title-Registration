package com.trianz.ltr.ejb;

/**
 * LandTitleException - Application exception for the Land Title Registry.
 * 
 * Cloud-native improvements:
 * - Removed @ApplicationException (EJB-specific annotation)
 * - Standard Java exception that works in any cloud environment
 * - Compatible with Spring @Transactional rollback rules
 * - Can be used with Spring's @ResponseStatus for REST APIs
 */
public class LandTitleException extends Exception {

    private static final long serialVersionUID = 2L; // Incremented for cloud migration

    public enum ErrorCode {
        TITLE_NOT_FOUND,
        DUPLICATE_PARCEL,
        DUPLICATE_TITLE,
        INVALID_TRANSFER,
        TRANSFER_NOT_FOUND,
        TRANSFER_ALREADY_PROCESSED,
        VALIDATION_ERROR,
        DATA_ACCESS_ERROR,
        UNAUTHORIZED,
        CONFIGURATION_ERROR,
        EXTERNAL_SERVICE_ERROR
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

    public ErrorCode getErrorCode() { 
        return errorCode; 
    }

    /**
     * Determine if this exception should trigger a transaction rollback.
     * Used by Spring's @Transactional rollbackFor attribute.
     */
    public boolean shouldRollback() {
        // Rollback for data errors, but not for validation or not-found errors
        switch (errorCode) {
            case DATA_ACCESS_ERROR:
            case EXTERNAL_SERVICE_ERROR:
            case CONFIGURATION_ERROR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get HTTP status code for REST API responses.
     */
    public int getHttpStatusCode() {
        switch (errorCode) {
            case TITLE_NOT_FOUND:
            case TRANSFER_NOT_FOUND:
                return 404;
            case DUPLICATE_PARCEL:
            case DUPLICATE_TITLE:
                return 409;
            case VALIDATION_ERROR:
                return 400;
            case UNAUTHORIZED:
                return 403;
            case TRANSFER_ALREADY_PROCESSED:
            case INVALID_TRANSFER:
                return 422;
            case CONFIGURATION_ERROR:
            case EXTERNAL_SERVICE_ERROR:
            case DATA_ACCESS_ERROR:
            default:
                return 500;
        }
    }

    @Override
    public String toString() {
        return "LandTitleException[" + errorCode + "]: " + getMessage();
    }
}
