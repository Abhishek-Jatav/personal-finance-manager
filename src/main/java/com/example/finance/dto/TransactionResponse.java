package com.example.finance.dto;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A transaction as returned by the API. The category is shown by name. */
public record TransactionResponse(Long id, BigDecimal amount, LocalDate date, String category,
                                  String description, TransactionType type) {

    /** Builds the response from a transaction entity. */
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getAmount(), t.getTransactionDate(),
                t.getCategory().getName(), t.getDescription(), t.getType());
    }
}
