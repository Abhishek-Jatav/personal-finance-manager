package com.example.finance.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of PUT /api/goals/{id}. Every field is optional; fields that are left out stay unchanged. */
public record UpdateGoalRequest(
        @Size(max = 100) String goalName,
        @Positive @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
        LocalDate targetDate) {
}
