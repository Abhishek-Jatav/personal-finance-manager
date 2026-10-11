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
import java.util.List;
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

    private Category category(long id, String name, TransactionType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }

    private CreateTransactionRequest create(LocalDate date, String categoryName, String amount) {
        return new CreateTransactionRequest(new BigDecimal(amount), date, categoryName, "note");
    }

    private Transaction existing(Category category) {
        Transaction transaction = new Transaction();
        transaction.setId(3L);
        transaction.setTransactionDate(TODAY.minusDays(5));
        transaction.setType(category.getType());
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal("1.00"));
        return transaction;
    }

    @Test
    void createTransaction_rejectsFutureDate() {
        assertThatThrownBy(() -> service.create(create(TODAY.plusDays(1), "Food", "10.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_rejectsUnknownCategory() {
        when(categoryRepository.findAvailableByName("Nope", 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(create(TODAY, " Nope ", "10.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void createTransaction_takesTypeFromCategory_setsOwner_andScalesAmount() {
        when(categoryRepository.findAvailableByName("Salary", 1L))
                .thenReturn(List.of(category(7, "Salary", TransactionType.INCOME)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.create(create(TODAY, "Salary", "125.5"));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getUser().getId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(response.category()).isEqualTo("Salary");
        assertThat(response.amount()).isEqualTo(new BigDecimal("125.50"));
        assertThat(response.date()).isEqualTo(TODAY);
    }

    @Test
    void listTransactions_rejectsStartAfterEnd() {
        assertThatThrownBy(() -> service.list(TODAY, TODAY.minusDays(1), null, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateTransaction_ignoresDateInRequest() {
        Transaction tx = existing(category(7, "Food", TransactionType.EXPENSE));
        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(tx));

        TransactionResponse response = service.update(3L,
                new UpdateTransactionRequest(new BigDecimal("9.00"), TODAY.minusDays(1), null, null));

        assertThat(response.date()).isEqualTo(TODAY.minusDays(5));
        assertThat(response.amount()).isEqualTo(new BigDecimal("9.00"));
    }

    @Test
    void updateTransaction_changesOnlyProvidedFields() {
        Transaction tx = existing(category(7, "Food", TransactionType.EXPENSE));
        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(tx));

        TransactionResponse response = service.update(3L,
                new UpdateTransactionRequest(new BigDecimal("42"), tx.getTransactionDate(), null, "  changed "));

        assertThat(response.amount()).isEqualTo(new BigDecimal("42.00"));
        assertThat(response.description()).isEqualTo("changed");
        assertThat(response.category()).isEqualTo("Food");
        assertThat(response.date()).isEqualTo(TODAY.minusDays(5));
    }

    @Test
    void updateTransaction_changingCategoryChangesType() {
        Transaction tx = existing(category(7, "Food", TransactionType.EXPENSE));
        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(tx));
        when(categoryRepository.findAvailableByName("Salary", 1L))
                .thenReturn(List.of(category(8, "Salary", TransactionType.INCOME)));

        TransactionResponse response = service.update(3L, new UpdateTransactionRequest(null, null, "Salary", null));

        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(response.category()).isEqualTo("Salary");
    }

    @Test
    void updateTransaction_notOwned_throwsNotFound() {
        when(transactionRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(3L, new UpdateTransactionRequest(null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTransaction_notOwned_throwsNotFound() {
        when(transactionRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransaction_owned_deletesIt() {
        Transaction tx = new Transaction();
        when(transactionRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(tx));

        service.delete(9L);

        verify(transactionRepository).delete(tx);
    }
}
