package com.example.finance.controller;

import com.example.finance.dto.CreateTransactionRequest;
import com.example.finance.dto.MessageResponse;
import com.example.finance.dto.TransactionListResponse;
import com.example.finance.dto.TransactionResponse;
import com.example.finance.dto.UpdateTransactionRequest;
import com.example.finance.entity.TransactionType;
import com.example.finance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Routes under /api/transactions. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** POST /api/transactions - creates a transaction (201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.create(request);
    }

    /** GET /api/transactions - newest first; optional filters startDate, endDate, categoryId, type. */
    @GetMapping
    public TransactionListResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type) {
        return transactionService.list(startDate, endDate, categoryId, type);
    }

    /** PUT /api/transactions/{id} - updates amount, description and/or category (the date cannot change). */
    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTransactionRequest request) {
        return transactionService.update(id, request);
    }

    /** DELETE /api/transactions/{id} - deletes a transaction (200 with a message). */
    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        transactionService.delete(id);
        return new MessageResponse("Transaction deleted successfully");
    }
}
