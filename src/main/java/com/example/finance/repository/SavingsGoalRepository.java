package com.example.finance.repository;

import com.example.finance.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    List<SavingsGoal> findByUserIdOrderByIdAsc(Long userId);
}
