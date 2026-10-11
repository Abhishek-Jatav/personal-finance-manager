package com.example.finance.service;

import com.example.finance.dto.CreateTransactionRequest;
import com.example.finance.dto.TransactionListResponse;
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

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Business rules for transactions. Every operation is limited to the logged-in user's own data. */
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

    /**
     * Creates a transaction. The date must not be in the future, and the category (given by name)
     * must be a default category or one of the user's own. The type is taken from the category.
     */
    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        User user = currentUser.getCurrentUser();
        if (request.date().isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Date cannot be in the future", "date");
        }
        Category category = resolveCategory(request.category(), user.getId());

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setTransactionDate(request.date());
        transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        transaction.setDescription(clean(request.description()));
        transaction.setCategory(category);
        transaction.setType(category.getType());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    /** Lists the user's transactions, newest first, with optional filters (null means no filter). */
    @Transactional(readOnly = true)
    public TransactionListResponse list(LocalDate startDate, LocalDate endDate, Long categoryId,
                                        String category, TransactionType type) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("'startDate' must not be after 'endDate'", "startDate");
        }
        User user = currentUser.getCurrentUser();
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"));
        List<TransactionResponse> items = transactionRepository
                .findAll(TransactionSpecifications.filter(user.getId(), startDate, endDate, categoryId, category, type),
                        newestFirst)
                .stream()
                .map(TransactionResponse::from)
                .toList();
        return new TransactionListResponse(items);
    }

    /**
     * Updates amount, description and/or category. The date can never change: a date in the request is ignored.
     *
     * @throws ResourceNotFoundException if the transaction does not exist or is not the user's
     */
    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request) {
        User user = currentUser.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (request.category() != null && !request.category().isBlank()) {
            Category category = resolveCategory(request.category(), user.getId());
            transaction.setCategory(category);
            transaction.setType(category.getType());
        }
        if (request.amount() != null) {
            transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.description() != null) {
            transaction.setDescription(clean(request.description()));
        }
        return TransactionResponse.from(transaction);
    }

    /**
     * Deletes a transaction. It disappears from goals and reports immediately because those are calculated live.
     *
     * @throws ResourceNotFoundException if the transaction does not exist or is not the user's
     */
    @Transactional
    public void delete(Long id) {
        User user = currentUser.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(String name, Long userId) {
        String trimmed = name.trim();
        return categoryRepository.findAvailableByName(trimmed, userId).stream().findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Category '" + trimmed + "' does not exist or is not available to you", "category"));
    }

    private String clean(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
