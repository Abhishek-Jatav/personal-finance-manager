package com.example.finance.repository;

import com.example.finance.entity.Category;
import com.example.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** A category the user may use: a global default or one of their own. */
    @Query("select c from Category c left join c.user u where c.id = :id and (c.defaultCategory = true or u.id = :userId)")
    Optional<Category> findAvailableById(Long id, Long userId);

    @Query("select c from Category c left join c.user u where c.defaultCategory = true or u.id = :userId order by c.type, c.name")
    List<Category> findAvailable(Long userId);

    List<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndTypeAndNameIgnoreCase(Long userId, TransactionType type, String name);

    boolean existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(TransactionType type, String name);

    boolean existsByDefaultCategoryTrueAndNameIgnoreCase(String name);
}
