package com.example.finance.service;

import com.example.finance.dto.CreateGoalRequest;
import com.example.finance.dto.GoalListResponse;
import com.example.finance.dto.GoalResponse;
import com.example.finance.dto.UpdateGoalRequest;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ForbiddenOperationException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.SavingsGoalRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

/** Business rules for savings goals. Progress is calculated from current transactions, never stored. */
@Service
public class GoalService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public GoalService(SavingsGoalRepository goalRepository,
                       TransactionRepository transactionRepository,
                       CurrentUserProvider currentUser,
                       Clock clock) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /**
     * Creates a goal. The start date defaults to today. The target date must be in the future
     * and not before the start date.
     */
    @Transactional
    public GoalResponse create(CreateGoalRequest request) {
        User user = currentUser.getCurrentUser();
        LocalDate today = LocalDate.now(clock);
        LocalDate start = request.startDate() == null ? today : request.startDate();
        if (start.isAfter(today)) {
            throw new BadRequestException("Start date cannot be in the future", "startDate");
        }
        validateTargetDate(request.targetDate(), start, today);

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setGoalName(request.goalName().trim());
        goal.setTargetAmount(request.targetAmount().setScale(2, RoundingMode.HALF_UP));
        goal.setTargetDate(request.targetDate());
        goal.setStartDate(start);
        return toResponse(goalRepository.save(goal), user.getId(), today);
    }

    /** Lists all goals of the user with their progress. */
    @Transactional(readOnly = true)
    public GoalListResponse list() {
        User user = currentUser.getCurrentUser();
        LocalDate today = LocalDate.now(clock);
        return new GoalListResponse(goalRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .map(goal -> toResponse(goal, user.getId(), today))
                .toList());
    }

    /**
     * Returns one goal.
     *
     * @throws ResourceNotFoundException if no goal has this id
     * @throws ForbiddenOperationException if the goal belongs to another user
     */
    @Transactional(readOnly = true)
    public GoalResponse get(Long id) {
        User user = currentUser.getCurrentUser();
        return toResponse(findOwned(id, user.getId()), user.getId(), LocalDate.now(clock));
    }

    /** Updates the target amount, target date and/or name. Fields left out stay unchanged. */
    @Transactional
    public GoalResponse update(Long id, UpdateGoalRequest request) {
        User user = currentUser.getCurrentUser();
        LocalDate today = LocalDate.now(clock);
        SavingsGoal goal = findOwned(id, user.getId());
        if (request.targetDate() != null) {
            validateTargetDate(request.targetDate(), goal.getStartDate(), today);
            goal.setTargetDate(request.targetDate());
        }
        if (request.targetAmount() != null) {
            goal.setTargetAmount(request.targetAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.goalName() != null && !request.goalName().isBlank()) {
            goal.setGoalName(request.goalName().trim());
        }
        return toResponse(goal, user.getId(), today);
    }

    /** Deletes a goal. Transactions are not affected. */
    @Transactional
    public void delete(Long id) {
        User user = currentUser.getCurrentUser();
        goalRepository.delete(findOwned(id, user.getId()));
    }

    private void validateTargetDate(LocalDate targetDate, LocalDate start, LocalDate today) {
        if (!targetDate.isAfter(today)) {
            throw new BadRequestException("Target date must be a future date", "targetDate");
        }
        if (targetDate.isBefore(start)) {
            throw new BadRequestException("Target date must be on or after the start date", "targetDate");
        }
    }

    private SavingsGoal findOwned(Long id, Long userId) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        if (!goal.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("You do not have access to this goal");
        }
        return goal;
    }

    /** progress = income - expenses from the goal start date through today (inclusive). Never clamped. */
    private GoalResponse toResponse(SavingsGoal goal, Long userId, LocalDate today) {
        BigDecimal income = orZero(transactionRepository.sumAmount(
                userId, TransactionType.INCOME, goal.getStartDate(), today));
        BigDecimal expenses = orZero(transactionRepository.sumAmount(
                userId, TransactionType.EXPENSE, goal.getStartDate(), today));
        BigDecimal progress = income.subtract(expenses);
        BigDecimal percentage = progress.multiply(HUNDRED)
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = goal.getTargetAmount().subtract(progress);
        return new GoalResponse(goal.getId(), goal.getGoalName(), goal.getTargetAmount(), goal.getTargetDate(),
                goal.getStartDate(), progress, percentage.doubleValue(), remaining);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
