package com.example.finance.integration;

import com.example.finance.entity.Category;
import com.example.finance.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionApiTest extends IntegrationTestBase {

    @Autowired
    private CategoryRepository categoryRepository;

    private Long defaultCategoryId(String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.isDefaultCategory() && c.getName().equals(name))
                .map(Category::getId).findFirst().orElseThrow();
    }

    @Test
    void create_incomeAndExpense_typeComesFromCategory() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/transactions", txBody("Salary", "50000.00", "2026-10-05", "October Salary"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(50000.0))
                .andExpect(jsonPath("$.date").value("2026-10-05"))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.description").value("October Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"));
        doPost(s, "/api/transactions", txBody("food", "125.50", "2026-10-05", "Groceries"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(125.50))
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void create_descriptionIsOptional() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Map<String, Object> body = txBody("Food", "10.00", "2026-10-05", null);
        body.remove("description");
        doPost(s, "/api/transactions", body).andExpect(status().isCreated());
    }

    @Test
    void create_todayIsAllowed_tomorrowIsRejected() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/transactions", txBody("Food", "10.00", "2026-10-10", "today"))
                .andExpect(status().isCreated());
        doPost(s, "/api/transactions", txBody("Food", "10.00", "2026-10-11", "tomorrow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
    }

    @Test
    void create_rejectsInvalidInput() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/transactions", txBody("Food", "0", "2026-10-05", "zero")).andExpect(status().isBadRequest());
        doPost(s, "/api/transactions", txBody("Food", "-5.00", "2026-10-05", "neg")).andExpect(status().isBadRequest());
        doPost(s, "/api/transactions", txBody("Food", "1.005", "2026-10-05", "3 decimals"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/transactions", txBody("Food", "5.00", "05-10-2026", "bad date"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/transactions", txBody("", "5.00", "2026-10-05", "no category"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownCategory_returnsBadRequest() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/transactions", txBody("DoesNotExist", "5.00", "2026-10-05", "ghost"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
    }

    @Test
    void create_otherUsersCustomCategory_returnsBadRequest() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        doPost(a, "/api/categories", Map.of("name", "Secret", "type", "EXPENSE")).andExpect(status().isCreated());
        doPost(a, "/api/transactions", txBody("Secret", "5.00", "2026-10-05", "mine")).andExpect(status().isCreated());
        doPost(b, "/api/transactions", txBody("Secret", "5.00", "2026-10-05", "steal"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_isNewestFirst_andWrappedInTransactionsKey() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        createTransaction(s, "Food", "1.00", "2026-10-01");
        createTransaction(s, "Food", "2.00", "2026-10-05");
        createTransaction(s, "Food", "3.00", "2026-10-03");
        doGet(s, "/api/transactions").andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(3)))
                .andExpect(jsonPath("$.transactions[0].date").value("2026-10-05"))
                .andExpect(jsonPath("$.transactions[1].date").value("2026-10-03"))
                .andExpect(jsonPath("$.transactions[2].date").value("2026-10-01"));
    }

    @Test
    void list_filtersByDateCategoryAndType() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        createTransaction(s, "Food", "10.00", "2026-10-01");
        createTransaction(s, "Rent", "20.00", "2026-10-05");
        createTransaction(s, "Salary", "30.00", "2026-10-05");

        doGet(s, "/api/transactions?startDate=2026-10-05&endDate=2026-10-05")
                .andExpect(jsonPath("$.transactions", hasSize(2)));
        doGet(s, "/api/transactions?startDate=2026-10-02").andExpect(jsonPath("$.transactions", hasSize(2)));
        doGet(s, "/api/transactions?endDate=2026-10-01").andExpect(jsonPath("$.transactions", hasSize(1)));
        doGet(s, "/api/transactions?categoryId=" + defaultCategoryId("Rent"))
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].category").value("Rent"));
        doGet(s, "/api/transactions?category=food").andExpect(jsonPath("$.transactions", hasSize(1)));
        doGet(s, "/api/transactions?category=Salary").andExpect(jsonPath("$.transactions", hasSize(1)));
        doGet(s, "/api/transactions?type=INCOME").andExpect(jsonPath("$.transactions", hasSize(1)));
        doGet(s, "/api/transactions?type=EXPENSE&startDate=2026-10-02")
                .andExpect(jsonPath("$.transactions", hasSize(1)));
        doGet(s, "/api/transactions?startDate=2025-01-01&endDate=2025-01-31")
                .andExpect(jsonPath("$.transactions", hasSize(0)));
    }

    @Test
    void list_rejectsBadFilters() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doGet(s, "/api/transactions?startDate=2026-10-05&endDate=2026-10-01").andExpect(status().isBadRequest());
        doGet(s, "/api/transactions?startDate=yesterday").andExpect(status().isBadRequest());
        doGet(s, "/api/transactions?type=OTHER").andExpect(status().isBadRequest());
        doGet(s, "/api/transactions?categoryId=abc").andExpect(status().isBadRequest());
    }

    @Test
    void update_changesOnlySentFields() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "Salary", "50000.00", "2026-10-05");

        doPut(s, "/api/transactions/" + id, Map.of("amount", 60000.00, "description", "Updated Salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60000.0))
                .andExpect(jsonPath("$.description").value("Updated Salary"))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.date").value("2026-10-05"))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void update_changingCategory_updatesType() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "Food", "10.00", "2026-10-05");
        doPut(s, "/api/transactions/" + id, Map.of("category", "Salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"));
        doPut(s, "/api/transactions/" + id, Map.of("category", "Nope")).andExpect(status().isBadRequest());
    }

    @Test
    void update_ignoresDateField() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "Food", "10.00", "2026-10-05");
        doPut(s, "/api/transactions/" + id, Map.of("date", "2026-10-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-05"));
        doPut(s, "/api/transactions/" + id, Map.of("date", "2026-10-06", "amount", 12.5, "description", "changed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-05"))
                .andExpect(jsonPath("$.amount").value(12.5))
                .andExpect(jsonPath("$.description").value("changed"));
    }

    @Test
    void update_invalidAmount_returnsBadRequest() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "Food", "10.00", "2026-10-05");
        doPut(s, "/api/transactions/" + id, Map.of("amount", -1)).andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete_otherUsersOrMissingTransaction_returnNotFound() throws Exception {
        MockHttpSession owner = registerAndLogin(uniqueEmail());
        MockHttpSession other = registerAndLogin(uniqueEmail());
        Long id = createTransaction(owner, "Food", "10.00", "2026-10-05");

        doPut(other, "/api/transactions/" + id, Map.of("amount", 1)).andExpect(status().isNotFound());
        doDelete(other, "/api/transactions/" + id).andExpect(status().isNotFound());
        doPut(owner, "/api/transactions/999999", Map.of("amount", 1)).andExpect(status().isNotFound());
        doGet(other, "/api/transactions").andExpect(jsonPath("$.transactions", hasSize(0)));
        doGet(owner, "/api/transactions").andExpect(jsonPath("$.transactions", hasSize(1)));
    }

    @Test
    void delete_returnsMessage_andTransactionIsGone() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "Food", "10.00", "2026-10-05");
        doDelete(s, "/api/transactions/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
        doGet(s, "/api/transactions").andExpect(jsonPath("$.transactions", hasSize(0)));
        doDelete(s, "/api/transactions/" + id).andExpect(status().isNotFound());
    }

    @Test
    void create_ignoresClientSuppliedUserId() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        Map<String, Object> body = txBody("Food", "10.00", "2026-10-05", "mine");
        body.put("userId", 1);
        doPost(a, "/api/transactions", body).andExpect(status().isCreated());
        doGet(a, "/api/transactions").andExpect(jsonPath("$.transactions", hasSize(1)));
        doGet(b, "/api/transactions").andExpect(jsonPath("$.transactions", hasSize(0)));
    }
}
