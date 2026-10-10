package com.example.finance.dto;

import java.util.List;

/** Response of GET /api/transactions. */
public record TransactionListResponse(List<TransactionResponse> transactions) {
}
