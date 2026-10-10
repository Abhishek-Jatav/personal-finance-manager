package com.example.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A savings goal with its progress, which is calculated from transactions every time. */
public record GoalResponse(Long id, String goalName, BigDecimal targetAmount, LocalDate targetDate,
                           LocalDate startDate, BigDecimal currentProgress, BigDecimal progressPercentage,
                           BigDecimal remainingAmount) {
}
