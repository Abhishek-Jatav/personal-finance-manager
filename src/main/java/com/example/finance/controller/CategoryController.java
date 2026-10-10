package com.example.finance.controller;

import com.example.finance.dto.CategoryListResponse;
import com.example.finance.dto.CategoryResponse;
import com.example.finance.dto.CreateCategoryRequest;
import com.example.finance.dto.MessageResponse;
import com.example.finance.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Routes under /api/categories. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** GET /api/categories - default categories plus the user's own. */
    @GetMapping
    public CategoryListResponse list() {
        return categoryService.list();
    }

    /** POST /api/categories - creates a custom category (201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    /** DELETE /api/categories/{name} - deletes a custom category (200 with a message). */
    @DeleteMapping("/{name}")
    public MessageResponse delete(@PathVariable String name) {
        categoryService.delete(name);
        return new MessageResponse("Category deleted successfully");
    }
}
