package com.example.finance.config;

import com.example.finance.entity.Category;
import com.example.finance.entity.TransactionType;
import com.example.finance.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds the default categories. Safe to run many times: existing defaults are skipped. */
@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("Salary", TransactionType.INCOME);
        seed("Food", TransactionType.EXPENSE);
        seed("Rent", TransactionType.EXPENSE);
        seed("Transportation", TransactionType.EXPENSE);
        seed("Entertainment", TransactionType.EXPENSE);
        seed("Healthcare", TransactionType.EXPENSE);
        seed("Utilities", TransactionType.EXPENSE);
    }

    private void seed(String name, TransactionType type) {
        if (categoryRepository.existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(type, name)) {
            return;
        }
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setDefaultCategory(true);
        categoryRepository.save(category);
    }
}
