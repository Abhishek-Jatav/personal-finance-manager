package com.example.finance.service;

import com.example.finance.dto.CategoryListResponse;
import com.example.finance.dto.CategoryResponse;
import com.example.finance.dto.CreateCategoryRequest;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ConflictException;
import com.example.finance.exception.ForbiddenOperationException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for default and custom categories. Category names are compared ignoring case. */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUser;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           CurrentUserProvider currentUser) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.currentUser = currentUser;
    }

    /** Lists the default categories plus the user's own custom categories. */
    @Transactional(readOnly = true)
    public CategoryListResponse list() {
        User user = currentUser.getCurrentUser();
        return new CategoryListResponse(
                categoryRepository.findAvailable(user.getId()).stream().map(CategoryResponse::from).toList());
    }

    /**
     * Creates a custom category.
     *
     * @throws ConflictException if the name is already used by a default category or by one of the user's categories
     */
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        User user = currentUser.getCurrentUser();
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Category name must not be blank", "name");
        }
        if (categoryRepository.existsByDefaultCategoryTrueAndNameIgnoreCase(name)) {
            throw new ConflictException("A default category named '" + name + "' already exists");
        }
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
            throw new ConflictException("You already have a category named '" + name + "'");
        }
        Category category = new Category();
        category.setName(name);
        category.setType(request.type());
        category.setDefaultCategory(false);
        category.setUser(user);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /**
     * Deletes one of the user's custom categories.
     *
     * @throws ForbiddenOperationException if the name belongs to a default category
     * @throws ResourceNotFoundException if the user has no such category
     * @throws BadRequestException if a transaction still uses the category
     */
    @Transactional
    public void delete(String name) {
        User user = currentUser.getCurrentUser();
        String trimmed = name.trim();

        Category category = categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), trimmed)
                .stream().findFirst().orElse(null);
        if (category == null) {
            if (categoryRepository.existsByDefaultCategoryTrueAndNameIgnoreCase(trimmed)) {
                throw new ForbiddenOperationException("Default categories cannot be deleted");
            }
            throw new ResourceNotFoundException("Category not found");
        }
        if (transactionRepository.existsByCategoryId(category.getId())) {
            throw new BadRequestException("Category is used by existing transactions and cannot be deleted");
        }
        categoryRepository.delete(category);
    }
}
