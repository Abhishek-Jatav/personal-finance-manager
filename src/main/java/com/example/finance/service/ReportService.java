package com.example.finance.service;

import com.example.finance.dto.CategoryTotal;
import com.example.finance.dto.ReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional(readOnly = true)
    public ReportResponse monthly(int year, int month) {
        validateYear(year);
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12", "month");
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return build(String.format("%04d-%02d", year, month), start, end);
    }

    @Transactional(readOnly = true)
    public ReportResponse yearly(int year) {
        validateYear(year);
        return build(String.format("%04d", year), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private ReportResponse build(String period, LocalDate start, LocalDate end) {
        User user = currentUser.getCurrentUser();
        List<CategoryTotal> income = totals(user.getId(), TransactionType.INCOME, start, end);
        List<CategoryTotal> expenses = totals(user.getId(), TransactionType.EXPENSE, start, end);
        BigDecimal totalIncome = sum(income);
        BigDecimal totalExpenses = sum(expenses);
        return new ReportResponse(period, totalIncome, income, totalExpenses, expenses,
                totalIncome.subtract(totalExpenses));
    }

    private List<CategoryTotal> totals(Long userId, TransactionType type, LocalDate start, LocalDate end) {
        List<CategoryTotal> result = new ArrayList<>();
        for (Object[] row : transactionRepository.totalsByCategory(userId, type, start, end)) {
            result.add(new CategoryTotal((String) row[0], (BigDecimal) row[1]));
        }
        return result;
    }

    private BigDecimal sum(List<CategoryTotal> totals) {
        return totals.stream().map(CategoryTotal::total).reduce(ZERO, BigDecimal::add);
    }

    private void validateYear(int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BadRequestException("Year must be between " + MIN_YEAR + " and " + MAX_YEAR, "year");
        }
    }
}
