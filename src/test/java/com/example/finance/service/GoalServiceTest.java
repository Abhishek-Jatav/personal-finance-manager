package com.example.finance.service;

import com.example.finance.dto.CreateGoalRequest;
import com.example.finance.dto.GoalResponse;
import com.example.finance.dto.UpdateGoalRequest;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.SavingsGoalRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.example.finance.service.ServiceTestSupport.CLOCK;
import static com.example.finance.service.ServiceTestSupport.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    @Mock SavingsGoalRepository goalRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock CurrentUserProvider currentUser;

    GoalService service;

    @BeforeEach
    void setUp() {
        service = new GoalService(goalRepository, transactionRepository, currentUser, CLOCK);
        lenient().when(currentUser.getCurrentUser()).thenReturn(ServiceTestSupport.user(1));
    }

    private SavingsGoal goal(String target) {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(10L);
        goal.setGoalName("Fund");
        goal.setTargetAmount(new BigDecimal(target));
        goal.setStartDate(START);
        goal.setTargetDate(LocalDate.of(2027, 6, 30));
        return goal;
    }

    private void stubTotals(String income, String expenses) {
        when(transactionRepository.sumAmount(1L, TransactionType.INCOME, START, TODAY))
                .thenReturn(income == null ? null : new BigDecimal(income));
        when(transactionRepository.sumAmount(1L, TransactionType.EXPENSE, START, TODAY))
                .thenReturn(expenses == null ? null : new BigDecimal(expenses));
    }

    @Test
    void progress_isIncomeMinusExpenses() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal("10000.00")));
        stubTotals("3500.00", "1000.00");

        GoalResponse response = service.get(10L);

        assertThat(response.progress()).isEqualByComparingTo("2500.00");
        assertThat(response.completionPercentage()).isEqualByComparingTo("25.00");
        assertThat(response.remaining()).isEqualByComparingTo("7500.00");
    }

    @Test
    void progress_canBeNegative_andIsNotClamped() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal("10000.00")));
        stubTotals("0.00", "1000.00");

        GoalResponse response = service.get(10L);

        assertThat(response.progress()).isEqualByComparingTo("-1000.00");
        assertThat(response.completionPercentage()).isEqualByComparingTo("-10.00");
        assertThat(response.remaining()).isEqualByComparingTo("11000.00");
    }

    @Test
    void progress_canExceedTarget_andIsNotClamped() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal("10000.00")));
        stubTotals("25000.00", "5000.00");

        GoalResponse response = service.get(10L);

        assertThat(response.completionPercentage()).isEqualByComparingTo("200.00");
        assertThat(response.remaining()).isEqualByComparingTo("-10000.00");
    }

    @Test
    void progress_withNoTransactions_isZero() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal("10000.00")));
        stubTotals(null, null);

        GoalResponse response = service.get(10L);

        assertThat(response.progress()).isEqualByComparingTo("0.00");
        assertThat(response.completionPercentage()).isEqualByComparingTo("0.00");
        assertThat(response.remaining()).isEqualByComparingTo("10000.00");
    }

    @Test
    void completionPercentage_roundsHalfUpToTwoDecimals() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal("3.00")));
        stubTotals("1.00", null);

        // 1 / 3 * 100 = 33.333... -> 33.33
        assertThat(service.get(10L).completionPercentage()).isEqualByComparingTo("33.33");
    }

    @Test
    void getGoal_notOwned_throwsNotFound() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(10L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createGoal_rejectsFutureStartDate() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Fund", new BigDecimal("100.00"), LocalDate.of(2027, 1, 1), TODAY.plusDays(1));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BadRequestException.class);
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createGoal_rejectsTargetDateBeforeStartDate() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Fund", new BigDecimal("100.00"), START.minusDays(1), START);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void createGoal_savesOwnedGoal_andReturnsProgress() {
        when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        stubTotals("100.00", "40.00");

        GoalResponse response = service.create(new CreateGoalRequest(
                " Fund ", new BigDecimal("200"), LocalDate.of(2027, 1, 1), START));

        assertThat(response.goalName()).isEqualTo("Fund");
        assertThat(response.targetAmount()).isEqualByComparingTo("200.00");
        assertThat(response.progress()).isEqualByComparingTo("60.00");
        assertThat(response.completionPercentage()).isEqualByComparingTo("30.00");
    }

    @Test
    void updateGoal_changesTargetFields_andKeepsNameWhenNull() {
        SavingsGoal existing = goal("10000.00");
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        stubTotals("2500.00", null);

        GoalResponse response = service.update(10L,
                new UpdateGoalRequest(null, new BigDecimal("5000.00"), LocalDate.of(2027, 12, 31)));

        assertThat(response.goalName()).isEqualTo("Fund");
        assertThat(response.targetAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.completionPercentage()).isEqualByComparingTo("50.00");
        assertThat(existing.getTargetDate()).isEqualTo(LocalDate.of(2027, 12, 31));
    }

    @Test
    void updateGoal_rejectsTargetDateBeforeStart() {
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal("10000.00")));

        assertThatThrownBy(() -> service.update(10L,
                new UpdateGoalRequest("New", new BigDecimal("5000.00"), START.minusDays(1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listGoals_returnsOnlyOwnedGoalsWithProgress() {
        when(goalRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(goal("1000.00")));
        stubTotals("100.00", null);

        List<GoalResponse> goals = service.list();

        assertThat(goals).hasSize(1);
        assertThat(goals.get(0).completionPercentage()).isEqualByComparingTo("10.00");
    }

    @Test
    void deleteGoal_deletesOwnedGoal() {
        SavingsGoal existing = goal("1000.00");
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        service.delete(10L);

        verify(goalRepository).delete(existing);
    }
}
