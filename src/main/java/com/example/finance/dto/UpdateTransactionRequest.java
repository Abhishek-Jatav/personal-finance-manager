package com.example.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of PUT /api/transactions/{id}. Every field is optional; fields that are left out stay unchanged.
 * A date may be sent but it is ignored, because the date of a transaction can never change.
 */
public record UpdateTransactionRequest(
        @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        LocalDate date,
        @Size(max = 50) String category,
        @Size(max = 255) String description) {
}
