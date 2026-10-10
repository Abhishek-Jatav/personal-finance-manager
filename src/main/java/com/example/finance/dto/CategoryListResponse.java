package com.example.finance.dto;

import java.util.List;

/** Response of GET /api/categories. */
public record CategoryListResponse(List<CategoryResponse> categories) {
}
