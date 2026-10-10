package com.example.finance.repository;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Database access for transactions, including the sums used by goals and reports. */
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    /** Sum of amounts for one user and type between two dates (both inclusive). Null when nothing matches. */
    @Query("select sum(t.amount) from Transaction t where t.user.id = :userId and t.type = :type "
            + "and t.transactionDate >= :startDate and t.transactionDate <= :endDate")
    BigDecimal sumAmount(Long userId, TransactionType type, LocalDate startDate, LocalDate endDate);

    /** Rows of [categoryName, total] for one user and type between two dates (both inclusive). */
    @Query("select c.name, sum(t.amount) from Transaction t join t.category c "
            + "where t.user.id = :userId and t.type = :type "
            + "and t.transactionDate >= :startDate and t.transactionDate <= :endDate "
            + "group by c.name order by c.name")
    List<Object[]> totalsByCategory(Long userId, TransactionType type, LocalDate startDate, LocalDate endDate);
}
