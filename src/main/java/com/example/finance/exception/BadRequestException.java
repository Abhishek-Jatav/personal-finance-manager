package com.example.finance.exception;

/** Thrown when a request breaks a business rule (HTTP 400). May name the offending field. */
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String message) {
        this(message, null);
    }

    public BadRequestException(String message, String field) {
        super(message);
        this.field = field;
    }

    /** The request field the problem relates to, or null. */
    public String getField() {
        return field;
    }
}
