package com.example.finance.service;

import com.example.finance.dto.CreateGoalRequest;
import com.example.finance.dto.GoalResponse;
import com.example.finance.dto.UpdateGoalRequest;
import com.example.finance.entity.SavingsGoal;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
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
import java.util.List;

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

    @Transactional
    public GoalResponse create(CreateGoalRequest request) {
        User user = currentUser.getCurrentUser();
        LocalDate today = LocalDate.now(clock);
        if (request.startDate().isAfter(today)) {
            throw new BadRequestException("Start date cannot be in the future", "startDate");
        }
        if (request.targetDate().isBefore(request.startDate())) {
            throw new BadRequestException("Target date must be on or after the start date", "targetDate");
        }
        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setGoalName(request.goalName().trim());
        goal.setTargetAmount(request.targetAmount().setScale(2, RoundingMode.HALF_UP));
        goal.setTargetDate(request.targetDate());
        goal.setStartDate(request.startDate());
        return toResponse(goalRepository.save(goal), user.getId(), today);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list() {
        User user = currentUser.getCurrentUser();
        LocalDate today = LocalDate.now(clock);
        return goalRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .map(goal -> toResponse(goal, user.getId(), today))
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse get(Long id) {
        User user = currentUser.getCurrentUser();
        return toResponse(findOwned(id, user.getId()), user.getId(), LocalDate.now(clock));
    }

    @Transactional
    public GoalResponse update(Long id, UpdateGoalRequest request) {
        User user = currentUser.getCurrentUser();
        SavingsGoal goal = findOwned(id, user.getId());
        if (request.targetDate().isBefore(goal.getStartDate())) {
            throw new BadRequestException("Target date must be on or after the start date", "targetDate");
        }
        if (request.goalName() != null && !request.goalName().isBlank()) {
            goal.setGoalName(request.goalName().trim());
        }
        goal.setTargetAmount(request.targetAmount().setScale(2, RoundingMode.HALF_UP));
        goal.setTargetDate(request.targetDate());
        return toResponse(goal, user.getId(), LocalDate.now(clock));
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUser.getCurrentUser();
        goalRepository.delete(findOwned(id, user.getId()));
    }

    private SavingsGoal findOwned(Long id, Long userId) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    /** progress = income - expenses from the goal start date through today (inclusive). Never clamped. */
    private GoalResponse toResponse(SavingsGoal goal, Long userId, LocalDate today) {
        BigDecimal income = orZero(transactionRepository.sumAmount(
                userId, TransactionType.INCOME, goal.getStartDate(), today));
        BigDecimal expenses = orZero(transactionRepository.sumAmount(
                userId, TransactionType.EXPENSE, goal.getStartDate(), today));
        BigDecimal progress = income.subtract(expenses);
        BigDecimal completion = progress.multiply(HUNDRED)
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = goal.getTargetAmount().subtract(progress);
        return new GoalResponse(goal.getId(), goal.getGoalName(), goal.getTargetAmount(), goal.getTargetDate(),
                goal.getStartDate(), progress, completion, remaining);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }
}
