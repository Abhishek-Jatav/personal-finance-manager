package com.example.finance.service;

import com.example.finance.dto.CreateTransactionRequest;
import com.example.finance.dto.TransactionResponse;
import com.example.finance.dto.UpdateTransactionRequest;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.TransactionSpecifications;
import com.example.finance.security.CurrentUserProvider;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              CurrentUserProvider currentUser,
                              Clock clock) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        User user = currentUser.getCurrentUser();
        if (request.date().isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date cannot be in the future", "date");
        }
        Category category = resolveCategory(request.categoryId(), user.getId(), request.type());

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setTransactionDate(request.date());
        apply(transaction, request.amount(), request.description(), request.type(), category);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(LocalDate from, LocalDate to, Long categoryId, TransactionType type) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'", "from");
        }
        User user = currentUser.getCurrentUser();
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"));
        return transactionRepository
                .findAll(TransactionSpecifications.filter(user.getId(), from, to, categoryId, type), newestFirst)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request) {
        User user = currentUser.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (request.date() != null && !request.date().equals(transaction.getTransactionDate())) {
            throw new BadRequestException("The date of a transaction cannot be changed", "date");
        }
        Category category = resolveCategory(request.categoryId(), user.getId(), request.type());
        apply(transaction, request.amount(), request.description(), request.type(), category);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUser.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(Long categoryId, Long userId, TransactionType type) {
        Category category = categoryRepository.findAvailableById(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getType() != type) {
            throw new BadRequestException(
                    "Category type " + category.getType() + " does not match transaction type " + type, "type");
        }
        return category;
    }

    private void apply(Transaction transaction, BigDecimal amount, String description,
                       TransactionType type, Category category) {
        transaction.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        transaction.setDescription(description == null ? null : description.trim());
        transaction.setType(type);
        transaction.setCategory(category);
    }
}
