package com.jamlapps.insight_service.exception;

public class UsageServiceUnavailableException extends RuntimeException {

    public UsageServiceUnavailableException(String message) {
        super(message);
    }

    public UsageServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
