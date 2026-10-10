package com.example.finance.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String error, String code, String message,
                       List<FieldIssue> fieldErrors, String path) {
}
