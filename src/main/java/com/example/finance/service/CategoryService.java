package com.example.finance.service;

import com.example.finance.dto.CategoryResponse;
import com.example.finance.dto.CreateCategoryRequest;
import com.example.finance.entity.Category;
import com.example.finance.entity.TransactionType;
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

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        User user = currentUser.getCurrentUser();
        return categoryRepository.findAvailable(user.getId()).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        User user = currentUser.getCurrentUser();
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Category name must not be blank", "name");
        }
        if (categoryRepository.existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(request.type(), name)) {
            throw new ConflictException("A default " + request.type() + " category named '" + name + "' already exists");
        }
        if (categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(user.getId(), request.type(), name)) {
            throw new ConflictException("You already have a " + request.type() + " category named '" + name + "'");
        }
        Category category = new Category();
        category.setName(name);
        category.setType(request.type());
        category.setDefaultCategory(false);
        category.setUser(user);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /** Deletes one of the caller's custom categories by name. 'type' is only needed if two categories share a name. */
    @Transactional
    public void delete(String name, TransactionType type) {
        User user = currentUser.getCurrentUser();
        String trimmed = name.trim();

        List<Category> owned = categoryRepository.findByUserIdAndNameIgnoreCase(user.getId(), trimmed);
        if (type != null) {
            owned = owned.stream().filter(c -> c.getType() == type).toList();
        }
        if (owned.isEmpty()) {
            boolean isDefault = type == null
                    ? categoryRepository.existsByDefaultCategoryTrueAndNameIgnoreCase(trimmed)
                    : categoryRepository.existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(type, trimmed);
            if (isDefault) {
                throw new ForbiddenOperationException("Default categories cannot be deleted");
            }
            throw new ResourceNotFoundException("Category not found");
        }
        if (owned.size() > 1) {
            throw new BadRequestException(
                    "More than one category has this name; add the 'type' query parameter", "type");
        }
        Category category = owned.get(0);
        if (transactionRepository.existsByCategoryId(category.getId())) {
            throw new ConflictException("Category is used by existing transactions and cannot be deleted");
        }
        categoryRepository.delete(category);
    }
}
