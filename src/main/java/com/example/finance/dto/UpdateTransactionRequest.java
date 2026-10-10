package com.example.finance.dto;

import com.example.finance.entity.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Full replacement of the mutable fields. The date is optional, but if sent it must match the stored date. */
public record UpdateTransactionRequest(
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        LocalDate date,
        @NotNull Long categoryId,
        @Size(max = 255) String description,
        @NotNull TransactionType type) {
}
