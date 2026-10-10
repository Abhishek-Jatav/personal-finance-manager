package com.example.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank @Size(max = 100) String goalName,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
        @NotNull LocalDate targetDate,
        @NotNull LocalDate startDate) {
}
