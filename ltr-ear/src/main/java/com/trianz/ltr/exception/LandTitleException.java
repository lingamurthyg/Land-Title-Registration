package com.trianz.ltr.exception;

/**
 * LandTitleException - Cloud-ready exception for Land Title Registry operations.
 * 
 * CLOUD-READY FEATURES:
 *   - Standard exception handling (no vendor dependencies)
 *   - Structured error codes for API responses
 *   - Compatible with REST API error handling
 */
public class LandTitleException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum ErrorCode {
        TITLE_NOT_FOUND,
        DUPLICATE_PARCEL,
        VALIDATION_ERROR,
        DATA_ACCESS_ERROR,
        INVALID_TRANSFER,
        TRANSFER_NOT_FOUND,
        TRANSFER_ALREADY_PROCESSED,
        UNAUTHORIZED,
        INTERNAL_ERROR
    }

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

    @Override
    public String toString() {
        return String.format("LandTitleException[%s]: %s", errorCode, getMessage());
    }
}
