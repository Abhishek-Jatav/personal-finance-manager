package com.example.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of POST /api/transactions. The type (INCOME or EXPENSE) comes from the chosen category. */
public record CreateTransactionRequest(
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 50) String category,
        @Size(max = 255) String description) {
}
