package com.trianz.ltr.exception;

/**
 * LandTitleException - Business exception for Land Title operations
 *
 * Migrated from EJB exception to standard Spring exception.
 * Used for business logic validation and error handling.
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
public class LandTitleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public LandTitleException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LandTitleException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Error codes for Land Title operations
     */
    public enum ErrorCode {
        TITLE_NOT_FOUND,
        TRANSFER_NOT_FOUND,
        DUPLICATE_PARCEL,
        DUPLICATE_TITLE,
        VALIDATION_ERROR,
        UNAUTHORIZED,
        TRANSFER_ALREADY_PROCESSED,
        INVALID_TRANSFER,
        DATABASE_ERROR,
        INTERNAL_ERROR
    }
}
