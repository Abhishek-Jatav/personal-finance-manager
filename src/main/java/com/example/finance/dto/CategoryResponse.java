package com.example.finance.dto;

import com.example.finance.entity.Category;
import com.example.finance.entity.TransactionType;

public record CategoryResponse(Long id, String name, TransactionType type, boolean defaultCategory) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.isDefaultCategory());
    }
}
