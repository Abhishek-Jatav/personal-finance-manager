package com.example.finance.dto;

import com.example.finance.entity.Category;
import com.example.finance.entity.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A category as returned by the API. isCustom is false for the built-in defaults. */
public record CategoryResponse(String name, TransactionType type, @JsonProperty("isCustom") boolean isCustom) {

    /** Builds the response from a category entity. */
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getName(), c.getType(), !c.isDefaultCategory());
    }
}
