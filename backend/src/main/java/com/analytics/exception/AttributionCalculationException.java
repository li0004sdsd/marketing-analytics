package com.analytics.exception;

public class AttributionCalculationException extends RuntimeException {
    public AttributionCalculationException(String message) {
        super(message);
    }

    public AttributionCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
