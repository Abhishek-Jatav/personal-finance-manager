package com.example.finance.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Income and expense totals per category for one year. */
public record YearlyReportResponse(int year, Map<String, BigDecimal> totalIncome,
                                   Map<String, BigDecimal> totalExpenses, BigDecimal netSavings) {
}
