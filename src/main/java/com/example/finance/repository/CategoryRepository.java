package com.example.finance.repository;

import com.example.finance.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Database access for categories. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Finds a category by name (ignoring case) among the defaults and the given user's own categories. */
    @Query("select c from Category c left join c.user u where lower(c.name) = lower(:name) "
            + "and (c.defaultCategory = true or u.id = :userId)")
    List<Category> findAvailableByName(String name, Long userId);

    /** All categories the user may use: the defaults plus their own. */
    @Query("select c from Category c left join c.user u where c.defaultCategory = true or u.id = :userId "
            + "order by c.type, c.name")
    List<Category> findAvailable(Long userId);

    List<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByDefaultCategoryTrueAndNameIgnoreCase(String name);
}
