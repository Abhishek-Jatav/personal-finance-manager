package com.example.finance.repository;

import com.example.finance.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Database access for savings goals. */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByIdAsc(Long userId);
}
