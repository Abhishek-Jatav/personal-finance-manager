package com.example.finance.exception;

/** Thrown when an operation is not allowed, such as deleting a default category (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
