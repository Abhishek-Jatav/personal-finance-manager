package com.example.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(Long id, String goalName, BigDecimal targetAmount, LocalDate targetDate,
                           LocalDate startDate, BigDecimal progress, BigDecimal completionPercentage,
                           BigDecimal remaining) {
}
