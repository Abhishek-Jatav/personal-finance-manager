package com.example.finance.service;

import com.example.finance.dto.CategoryResponse;
import com.example.finance.dto.CreateCategoryRequest;
import com.example.finance.entity.Category;
import com.example.finance.entity.TransactionType;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ConflictException;
import com.example.finance.exception.ForbiddenOperationException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock CurrentUserProvider currentUser;

    CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, transactionRepository, currentUser);
        lenient().when(currentUser.getCurrentUser()).thenReturn(ServiceTestSupport.user(1));
    }

    private Category category(long id, String name, TransactionType type) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        return category;
    }

    @Test
    void createCategory_trimsName_andSavesAsCustom() {
        when(categoryRepository.existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(TransactionType.EXPENSE, "Pets"))
                .thenReturn(false);
        when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(1L, TransactionType.EXPENSE, "Pets"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.create(new CreateCategoryRequest("  Pets  ", TransactionType.EXPENSE));

        assertThat(response.name()).isEqualTo("Pets");
        assertThat(response.defaultCategory()).isFalse();
    }

    @Test
    void createCategory_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("   ", TransactionType.EXPENSE)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createCategory_rejectsDuplicateOfDefault() {
        when(categoryRepository.existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(TransactionType.EXPENSE, "food"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("food", TransactionType.EXPENSE)))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_rejectsDuplicateCustomName() {
        when(categoryRepository.existsByUserIdAndTypeAndNameIgnoreCase(1L, TransactionType.EXPENSE, "Pets"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Pets", TransactionType.EXPENSE)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteCategory_default_isForbidden() {
        when(categoryRepository.findByUserIdAndNameIgnoreCase(1L, "Food")).thenReturn(List.of());
        when(categoryRepository.existsByDefaultCategoryTrueAndNameIgnoreCase("Food")).thenReturn(true);

        assertThatThrownBy(() -> service.delete("Food", null)).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteCategory_defaultWithType_isForbidden() {
        when(categoryRepository.findByUserIdAndNameIgnoreCase(1L, "Food")).thenReturn(List.of());
        when(categoryRepository.existsByDefaultCategoryTrueAndTypeAndNameIgnoreCase(TransactionType.EXPENSE, "Food"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete("Food", TransactionType.EXPENSE))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteCategory_unknown_isNotFound() {
        when(categoryRepository.findByUserIdAndNameIgnoreCase(1L, "Nope")).thenReturn(List.of());

        assertThatThrownBy(() -> service.delete("Nope", null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategory_referenced_isConflict() {
        Category pets = category(5, "Pets", TransactionType.EXPENSE);
        when(categoryRepository.findByUserIdAndNameIgnoreCase(1L, "Pets")).thenReturn(List.of(pets));
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete("Pets", null)).isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_unreferenced_isDeleted() {
        Category pets = category(5, "Pets", TransactionType.EXPENSE);
        when(categoryRepository.findByUserIdAndNameIgnoreCase(1L, "Pets")).thenReturn(List.of(pets));
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(false);

        service.delete(" Pets ", null);

        verify(categoryRepository).delete(pets);
    }

    @Test
    void deleteCategory_sameNameInBothTypes_needsTypeParameter() {
        Category expense = category(5, "Misc", TransactionType.EXPENSE);
        Category income = category(6, "Misc", TransactionType.INCOME);
        when(categoryRepository.findByUserIdAndNameIgnoreCase(1L, "Misc")).thenReturn(List.of(expense, income));

        assertThatThrownBy(() -> service.delete("Misc", null)).isInstanceOf(BadRequestException.class);

        when(transactionRepository.existsByCategoryId(6L)).thenReturn(false);
        service.delete("Misc", TransactionType.INCOME);
        verify(categoryRepository).delete(income);
    }

    @Test
    void listCategories_mapsResults() {
        when(categoryRepository.findAvailable(1L)).thenReturn(List.of(category(1, "Food", TransactionType.EXPENSE)));

        assertThat(service.list()).hasSize(1);
        assertThat(service.list().get(0).name()).isEqualTo("Food");
    }
}
