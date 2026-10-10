package com.example.finance.dto;

import java.util.List;

/** Response of GET /api/goals. */
public record GoalListResponse(List<GoalResponse> goals) {
}
