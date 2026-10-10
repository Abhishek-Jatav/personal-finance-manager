package com.example.finance.dto;

import com.example.finance.entity.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotNull LocalDate date,
        @NotNull Long categoryId,
        @Size(max = 255) String description,
        @NotNull TransactionType type) {
}
