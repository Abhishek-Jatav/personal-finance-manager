package com.example.finance.service;

import com.example.finance.dto.MonthlyReportResponse;
import com.example.finance.dto.YearlyReportResponse;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.BadRequestException;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock CurrentUserProvider currentUser;

    ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(transactionRepository, currentUser);
        lenient().when(currentUser.getCurrentUser()).thenReturn(ServiceTestSupport.user(1));
    }

    private List<Object[]> rows(Object[]... rows) {
        List<Object[]> list = new ArrayList<>();
        for (Object[] row : rows) {
            list.add(row);
        }
        return list;
    }

    @Test
    void monthlyReport_totalsByCategoryAndNetSavings() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        when(transactionRepository.totalsByCategory(1L, TransactionType.INCOME, start, end)).thenReturn(rows(
                new Object[]{"Freelance", new BigDecimal("500.00")},
                new Object[]{"Salary", new BigDecimal("3000.00")}));
        when(transactionRepository.totalsByCategory(1L, TransactionType.EXPENSE, start, end)).thenReturn(rows(
                new Object[]{"Food", new BigDecimal("250.00")},
                new Object[]{"Rent", new BigDecimal("750.00")}));

        MonthlyReportResponse report = service.monthly(2026, 10);

        assertThat(report.month()).isEqualTo(10);
        assertThat(report.year()).isEqualTo(2026);
        assertThat(report.totalIncome()).containsOnlyKeys("Freelance", "Salary");
        assertThat(report.totalIncome().get("Salary")).isEqualByComparingTo("3000.00");
        assertThat(report.totalExpenses().get("Rent")).isEqualByComparingTo("750.00");
        assertThat(report.netSavings()).isEqualByComparingTo("2500.00");
    }

    @Test
    void monthlyReport_emptyPeriod_returnsZeroAndEmptyMaps() {
        MonthlyReportResponse report = service.monthly(2026, 9);

        assertThat(report.totalIncome()).isEmpty();
        assertThat(report.totalExpenses()).isEmpty();
        assertThat(report.netSavings()).isEqualByComparingTo("0.00");
    }

    @Test
    void monthlyReport_leapFebruaryEndsOnTwentyNinth() {
        service.monthly(2024, 2);

        verify(transactionRepository).totalsByCategory(
                1L, TransactionType.INCOME, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
    }

    @Test
    void monthlyReport_nonLeapFebruaryEndsOnTwentyEighth() {
        service.monthly(2026, 2);

        verify(transactionRepository).totalsByCategory(
                1L, TransactionType.EXPENSE, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    void yearlyReport_coversJanuaryFirstToDecemberThirtyFirst() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        when(transactionRepository.totalsByCategory(1L, TransactionType.INCOME, start, end)).thenReturn(rows());
        when(transactionRepository.totalsByCategory(1L, TransactionType.EXPENSE, start, end))
                .thenReturn(rows(new Object[]{"Food", new BigDecimal("100.00")}, new Object[]{"Rent", new BigDecimal("50.00")}));

        YearlyReportResponse report = service.yearly(2026);

        assertThat(report.year()).isEqualTo(2026);
        assertThat(report.netSavings()).isEqualByComparingTo("-150.00");
    }

    @Test
    void monthlyReport_rejectsInvalidMonths() {
        assertThatThrownBy(() -> service.monthly(2026, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.monthly(2026, 13)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void reports_rejectUnsupportedYears() {
        assertThatThrownBy(() -> service.yearly(1800)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.monthly(10000, 1)).isInstanceOf(BadRequestException.class);
    }
}
