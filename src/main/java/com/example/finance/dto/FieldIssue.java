package com.example.finance.dto;

/** One field-level problem inside an error response. */
public record FieldIssue(String field, String message) {
}
