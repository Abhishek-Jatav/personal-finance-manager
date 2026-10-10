package com.example.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportResponse(String period, BigDecimal totalIncome, List<CategoryTotal> incomeByCategory,
                             BigDecimal totalExpenses, List<CategoryTotal> expensesByCategory,
                             BigDecimal netSavings) {
}
