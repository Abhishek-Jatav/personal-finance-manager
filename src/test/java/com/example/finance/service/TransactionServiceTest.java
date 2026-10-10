package com.example.finance.service;

import com.example.finance.dto.CreateTransactionRequest;
import com.example.finance.dto.TransactionResponse;
import com.example.finance.dto.UpdateTransactionRequest;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static com.example.finance.service.ServiceTestSupport.CLOCK;
import static com.example.finance.service.ServiceTestSupport.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CurrentUserProvider currentUser;

    TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, categoryRepository, currentUser, CLOCK);
        lenient().when(currentUser.getCurrentUser()).thenReturn(ServiceTestSupport.user(1));
    }

    private Category category(long id, TransactionType type) {
        Category category = new Category();
        category.setId(id);
        category.setName("Cat" + id);
        category.setType(type);
        return category;
    }

    private CreateTransactionRequest create(LocalDate date, TransactionType type, String amount) {
        return new CreateTransactionRequest(new BigDecimal(amount), date, 7L, "note", type);
    }

    @Test
    void createTransaction_rejectsFutureDate() {
        assertThatThrownBy(() -> service.create(create(TODAY.plusDays(1), TransactionType.EXPENSE, "10.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_rejectsUnknownCategory() {
        when(categoryRepository.findAvailableById(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(create(TODAY, TransactionType.EXPENSE, "10.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTransaction_rejectsCategoryTypeMismatch() {
        when(categoryRepository.findAvailableById(7L, 1L))
                .thenReturn(Optional.of(category(7, TransactionType.INCOME)));

        assertThatThrownBy(() -> service.create(create(TODAY, TransactionType.EXPENSE, "10.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void createTransaction_setsOwnerFromLoggedInUser_andScalesAmount() {
        when(categoryRepository.findAvailableById(7L, 1L))
                .thenReturn(Optional.of(category(7, TransactionType.EXPENSE)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.create(create(TODAY, TransactionType.EXPENSE, "125.5"));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getUser().getId()).isEqualTo(1L);
        assertThat(response.amount()).isEqualTo(new BigDecimal("125.50"));
        assertThat(response.date()).isEqualTo(TODAY);
    }

    @Test
    void listTransactions_rejectsFromAfterTo() {
        assertThatThrownBy(() -> service.list(TODAY, TODAY.minusDays(1), null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateTransaction_rejectsDateChange() {
        Transaction existing = new Transaction();
        existing.setId(3L);
        existing.setTransactionDate(TODAY.minusDays(5));
        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(existing));

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                new BigDecimal("10.00"), TODAY.minusDays(4), 7L, null, TransactionType.EXPENSE);

        assertThatThrownBy(() -> service.update(3L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be changed");
    }

    @Test
    void updateTransaction_appliesNewValues_whenDateUnchangedOrOmitted() {
        Transaction existing = new Transaction();
        existing.setId(3L);
        existing.setTransactionDate(TODAY.minusDays(5));
        existing.setType(TransactionType.EXPENSE);
        existing.setCategory(category(7, TransactionType.EXPENSE));
        existing.setAmount(new BigDecimal("1.00"));
        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findAvailableById(7L, 1L))
                .thenReturn(Optional.of(category(7, TransactionType.EXPENSE)));

        TransactionResponse response = service.update(3L, new UpdateTransactionRequest(
                new BigDecimal("42.00"), null, 7L, "  changed ", TransactionType.EXPENSE));

        assertThat(response.amount()).isEqualTo(new BigDecimal("42.00"));
        assertThat(response.description()).isEqualTo("changed");
        assertThat(response.date()).isEqualTo(TODAY.minusDays(5));
    }

    @Test
    void deleteTransaction_notOwned_throwsNotFound() {
        when(transactionRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransaction_owned_deletesIt() {
        Transaction existing = new Transaction();
        when(transactionRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(existing));

        service.delete(9L);

        verify(transactionRepository).delete(existing);
    }
}
