package com.example.finance.dto;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(Long id, BigDecimal amount, LocalDate date, CategoryResponse category,
                                  String description, TransactionType type) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAmount(), t.getTransactionDate(),
                CategoryResponse.from(t.getCategory()), t.getDescription(), t.getType());
    }
}
