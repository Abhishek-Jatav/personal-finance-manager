package com.example.finance.service;

import com.example.finance.dto.CreateGoalRequest;
import com.example.finance.dto.GoalResponse;
import com.example.finance.dto.UpdateGoalRequest;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ForbiddenOperationException;
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
    private static final LocalDate FUTURE = LocalDate.of(2027, 6, 30);

    @Mock SavingsGoalRepository goalRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock CurrentUserProvider currentUser;

    GoalService service;

    @BeforeEach
    void setUp() {
        service = new GoalService(goalRepository, transactionRepository, currentUser, CLOCK);
        lenient().when(currentUser.getCurrentUser()).thenReturn(ServiceTestSupport.user(1));
    }

    private SavingsGoal goal(String target, long ownerId) {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(10L);
        goal.setUser(ServiceTestSupport.user(ownerId));
        goal.setGoalName("Fund");
        goal.setTargetAmount(new BigDecimal(target));
        goal.setStartDate(START);
        goal.setTargetDate(FUTURE);
        return goal;
    }

    private void stubTotals(LocalDate start, String income, String expenses) {
        when(transactionRepository.sumAmount(1L, TransactionType.INCOME, start, TODAY))
                .thenReturn(income == null ? null : new BigDecimal(income));
        when(transactionRepository.sumAmount(1L, TransactionType.EXPENSE, start, TODAY))
                .thenReturn(expenses == null ? null : new BigDecimal(expenses));
    }

    @Test
    void progress_isIncomeMinusExpenses() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("10000.00", 1)));
        stubTotals(START, "3500.00", "1000.00");

        GoalResponse response = service.get(10L);

        assertThat(response.currentProgress()).isEqualByComparingTo("2500.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("25.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void progress_canBeNegative_andIsNotClamped() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("10000.00", 1)));
        stubTotals(START, "0.00", "1000.00");

        GoalResponse response = service.get(10L);

        assertThat(response.currentProgress()).isEqualByComparingTo("-1000.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("-10.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("11000.00");
    }

    @Test
    void progress_canExceedTarget_andIsNotClamped() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("10000.00", 1)));
        stubTotals(START, "25000.00", "5000.00");

        GoalResponse response = service.get(10L);

        assertThat(response.progressPercentage()).isEqualByComparingTo("200.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("-10000.00");
    }

    @Test
    void progress_withNoTransactions_isZero() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("10000.00", 1)));
        stubTotals(START, null, null);

        GoalResponse response = service.get(10L);

        assertThat(response.currentProgress()).isEqualByComparingTo("0.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("0.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void percentage_roundsHalfUpToTwoDecimals() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("3.00", 1)));
        stubTotals(START, "1.00", null);

        // 1 / 3 * 100 = 33.333... -> 33.33
        assertThat(service.get(10L).progressPercentage()).isEqualByComparingTo("33.33");
    }

    @Test
    void getGoal_missing_throwsNotFound() {
        when(goalRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(10L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGoal_ofAnotherUser_throwsForbidden() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("1000.00", 2)));

        assertThatThrownBy(() -> service.get(10L)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createGoal_rejectsFutureStartDate() {
        CreateGoalRequest request = new CreateGoalRequest("Fund", new BigDecimal("100.00"), FUTURE, TODAY.plusDays(1));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BadRequestException.class);
        verify(goalRepository, never()).save(any());
    }

    @Test
    void createGoal_rejectsTargetDateTodayOrPast() {
        assertThatThrownBy(() -> service.create(
                new CreateGoalRequest("Fund", new BigDecimal("100.00"), TODAY, START)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
        assertThatThrownBy(() -> service.create(
                new CreateGoalRequest("Fund", new BigDecimal("100.00"), TODAY.minusDays(1), START)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createGoal_startDateDefaultsToToday() {
        when(goalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        stubTotals(TODAY, "100.00", "40.00");

        GoalResponse response = service.create(new CreateGoalRequest(" Fund ", new BigDecimal("200"), FUTURE, null));

        assertThat(response.startDate()).isEqualTo(TODAY);
        assertThat(response.goalName()).isEqualTo("Fund");
        assertThat(response.targetAmount()).isEqualByComparingTo("200.00");
        assertThat(response.currentProgress()).isEqualByComparingTo("60.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("30.00");
    }

    @Test
    void updateGoal_changesProvidedFields_andKeepsTheRest() {
        SavingsGoal existing = goal("10000.00", 1);
        when(goalRepository.findById(10L)).thenReturn(Optional.of(existing));
        stubTotals(START, "2500.00", null);

        GoalResponse response = service.update(10L,
                new UpdateGoalRequest(null, new BigDecimal("5000.00"), LocalDate.of(2027, 12, 31)));

        assertThat(response.goalName()).isEqualTo("Fund");
        assertThat(response.targetAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("50.00");
        assertThat(existing.getTargetDate()).isEqualTo(LocalDate.of(2027, 12, 31));
    }

    @Test
    void updateGoal_canRenameAndLeaveDateUnchanged() {
        SavingsGoal existing = goal("10000.00", 1);
        when(goalRepository.findById(10L)).thenReturn(Optional.of(existing));
        stubTotals(START, null, null);

        GoalResponse response = service.update(10L, new UpdateGoalRequest(" New name ", null, null));

        assertThat(response.goalName()).isEqualTo("New name");
        assertThat(response.targetDate()).isEqualTo(FUTURE);
    }

    @Test
    void updateGoal_rejectsPastTargetDate() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("10000.00", 1)));

        assertThatThrownBy(() -> service.update(10L, new UpdateGoalRequest(null, null, TODAY)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateGoal_ofAnotherUser_throwsForbidden() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal("10000.00", 2)));

        assertThatThrownBy(() -> service.update(10L, new UpdateGoalRequest(null, new BigDecimal("1"), null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void listGoals_returnsOnlyOwnedGoalsWithProgress() {
        when(goalRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(goal("1000.00", 1)));
        stubTotals(START, "100.00", null);

        var response = service.list();

        assertThat(response.goals()).hasSize(1);
        assertThat(response.goals().get(0).progressPercentage()).isEqualByComparingTo("10.00");
    }

    @Test
    void deleteGoal_deletesOwnedGoal_andRejectsOthers() {
        SavingsGoal mine = goal("1000.00", 1);
        when(goalRepository.findById(10L)).thenReturn(Optional.of(mine));
        service.delete(10L);
        verify(goalRepository).delete(mine);

        when(goalRepository.findById(11L)).thenReturn(Optional.of(goal("1000.00", 2)));
        assertThatThrownBy(() -> service.delete(11L)).isInstanceOf(ForbiddenOperationException.class);
    }
}
