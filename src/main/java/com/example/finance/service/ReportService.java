package com.example.finance.service;

import com.example.finance.dto.MonthlyReportResponse;
import com.example.finance.dto.YearlyReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Monthly and yearly reports, calculated from the user's current transactions. */
@Service
public class ReportService {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 9999;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUser;

    public ReportService(TransactionRepository transactionRepository, CurrentUserProvider currentUser) {
        this.transactionRepository = transactionRepository;
        this.currentUser = currentUser;
    }

    /** Report for one calendar month (first to last day, both included). */
    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(int year, int month) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12", "month");
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        User user = currentUser.getCurrentUser();
        Map<String, BigDecimal> income = totals(user.getId(), TransactionType.INCOME, start, end);
        Map<String, BigDecimal> expenses = totals(user.getId(), TransactionType.EXPENSE, start, end);
        return new MonthlyReportResponse(month, year, income, expenses, sum(income).subtract(sum(expenses)));
    }

    /** Report for one calendar year (January 1 to December 31, both included). */
    @Transactional(readOnly = true)
    public YearlyReportResponse yearly(int year) {
        validateYear(year);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        User user = currentUser.getCurrentUser();
        Map<String, BigDecimal> income = totals(user.getId(), TransactionType.INCOME, start, end);
        Map<String, BigDecimal> expenses = totals(user.getId(), TransactionType.EXPENSE, start, end);
        return new YearlyReportResponse(year, income, expenses, sum(income).subtract(sum(expenses)));
    }

    private Map<String, BigDecimal> totals(Long userId, TransactionType type, LocalDate start, LocalDate end) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : transactionRepository.totalsByCategory(userId, type, start, end)) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    private BigDecimal sum(Map<String, BigDecimal> totals) {
        return totals.values().stream().reduce(ZERO, BigDecimal::add);
    }

    private void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BadRequestException("Year must be between " + MIN_YEAR + " and " + MAX_YEAR, "year");
        }
    }
}
