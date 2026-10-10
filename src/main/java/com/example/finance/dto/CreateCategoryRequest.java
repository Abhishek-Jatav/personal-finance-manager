package com.example.finance.dto;

import com.example.finance.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Part of the Personal Finance Manager API. */
public record CreateCategoryRequest(@NotBlank @Size(max = 50) String name, @NotNull TransactionType type) {
}
