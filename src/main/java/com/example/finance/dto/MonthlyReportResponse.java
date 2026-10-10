package com.example.finance.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Income and expense totals per category for one month. */
public record MonthlyReportResponse(int month, int year, Map<String, BigDecimal> totalIncome,
                                    Map<String, BigDecimal> totalExpenses, BigDecimal netSavings) {
}
