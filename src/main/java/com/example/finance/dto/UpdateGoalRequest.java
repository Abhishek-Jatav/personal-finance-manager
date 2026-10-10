package com.example.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Target amount and target date are required. The name is optional (left unchanged when null). */
public record UpdateGoalRequest(
        @Size(max = 100) String goalName,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
        @NotNull LocalDate targetDate) {
}
