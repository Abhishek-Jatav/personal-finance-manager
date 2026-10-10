package com.example.finance.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionApiTest extends IntegrationTestBase {

    @Test
    void create_returnsCreatedTransaction() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        doPost(s, "/api/transactions", txBody("EXPENSE", "125.50", "2026-10-05", food, "Groceries"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(125.50))
                .andExpect(jsonPath("$.date").value("2026-10-05"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.category.name").value("Food"))
                .andExpect(jsonPath("$.description").value("Groceries"));
    }

    @Test
    void create_todayIsAllowed_tomorrowIsRejected() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        doPost(s, "/api/transactions", txBody("EXPENSE", "10.00", "2026-10-10", food, "today"))
                .andExpect(status().isCreated());
        doPost(s, "/api/transactions", txBody("EXPENSE", "10.00", "2026-10-11", food, "tomorrow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
    }

    @Test
    void create_rejectsZeroAndNegativeAmount() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        doPost(s, "/api/transactions", txBody("EXPENSE", "0", "2026-10-05", food, "zero"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/transactions", txBody("EXPENSE", "-5.00", "2026-10-05", food, "negative"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejectsInvalidTypeAndMalformedDate() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        doPost(s, "/api/transactions", txBody("OTHER", "5.00", "2026-10-05", food, "bad type"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/transactions", txBody("EXPENSE", "5.00", "05-10-2026", food, "bad date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejectsCategoryTypeMismatch() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long salary = categoryId(s, "Salary");
        doPost(s, "/api/transactions", txBody("EXPENSE", "5.00", "2026-10-05", salary, "mismatch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
    }

    @Test
    void create_unknownCategory_returnsNotFound() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/transactions", txBody("EXPENSE", "5.00", "2026-10-05", 999999L, "ghost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_isNewestFirst() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        createTransaction(s, "EXPENSE", "1.00", "2026-10-01", food);
        createTransaction(s, "EXPENSE", "2.00", "2026-10-05", food);
        createTransaction(s, "EXPENSE", "3.00", "2026-10-03", food);
        doGet(s, "/api/transactions").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].date").value("2026-10-05"))
                .andExpect(jsonPath("$[1].date").value("2026-10-03"))
                .andExpect(jsonPath("$[2].date").value("2026-10-01"));
    }

    @Test
    void list_filtersByDateCategoryAndType() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        Long rent = categoryId(s, "Rent");
        Long salary = categoryId(s, "Salary");
        createTransaction(s, "EXPENSE", "10.00", "2026-10-01", food);
        createTransaction(s, "EXPENSE", "20.00", "2026-10-05", rent);
        createTransaction(s, "INCOME", "30.00", "2026-10-05", salary);

        doGet(s, "/api/transactions?from=2026-10-05&to=2026-10-05").andExpect(jsonPath("$", hasSize(2)));
        doGet(s, "/api/transactions?from=2026-10-02").andExpect(jsonPath("$", hasSize(2)));
        doGet(s, "/api/transactions?to=2026-10-01").andExpect(jsonPath("$", hasSize(1)));
        doGet(s, "/api/transactions?categoryId=" + rent).andExpect(jsonPath("$", hasSize(1)));
        doGet(s, "/api/transactions?type=INCOME").andExpect(jsonPath("$", hasSize(1)));
        doGet(s, "/api/transactions?type=EXPENSE&from=2026-10-02").andExpect(jsonPath("$", hasSize(1)));
        doGet(s, "/api/transactions?from=2025-01-01&to=2025-01-31").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void list_rejectsBadFilters() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doGet(s, "/api/transactions?from=2026-10-05&to=2026-10-01").andExpect(status().isBadRequest());
        doGet(s, "/api/transactions?from=yesterday").andExpect(status().isBadRequest());
        doGet(s, "/api/transactions?type=OTHER").andExpect(status().isBadRequest());
    }

    @Test
    void update_changesMutableFields() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        Long rent = categoryId(s, "Rent");
        Long id = createTransaction(s, "EXPENSE", "10.00", "2026-10-05", food);

        doPut(s, "/api/transactions/" + id, txBody("EXPENSE", "99.99", "2026-10-05", rent, "updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.category.name").value("Rent"))
                .andExpect(jsonPath("$.description").value("updated"));
    }

    @Test
    void update_rejectsDateChange() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long food = categoryId(s, "Food");
        Long id = createTransaction(s, "EXPENSE", "10.00", "2026-10-05", food);
        doPut(s, "/api/transactions/" + id, txBody("EXPENSE", "10.00", "2026-10-06", food, "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
    }

    @Test
    void updateAndDelete_otherUsersTransaction_returnNotFound() throws Exception {
        MockHttpSession owner = registerAndLogin(uniqueEmail());
        MockHttpSession other = registerAndLogin(uniqueEmail());
        Long id = createTransaction(owner, "EXPENSE", "10.00", "2026-10-05", categoryId(owner, "Food"));

        doPut(other, "/api/transactions/" + id,
                txBody("EXPENSE", "1.00", "2026-10-05", categoryId(other, "Food"), "hijack"))
                .andExpect(status().isNotFound());
        doDelete(other, "/api/transactions/" + id).andExpect(status().isNotFound());
        doGet(other, "/api/transactions").andExpect(jsonPath("$", hasSize(0)));
        doGet(owner, "/api/transactions").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void delete_removesTransactionAndIsNotFoundAfterwards() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "EXPENSE", "10.00", "2026-10-05", categoryId(s, "Food"));
        doDelete(s, "/api/transactions/" + id).andExpect(status().isNoContent());
        doGet(s, "/api/transactions").andExpect(jsonPath("$", hasSize(0)));
        doDelete(s, "/api/transactions/" + id).andExpect(status().isNotFound());
    }

    @Test
    void create_ignoresClientSuppliedUserId() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        Map<String, Object> body = txBody("EXPENSE", "10.00", "2026-10-05", categoryId(a, "Food"), "mine");
        body.put("userId", 1);
        doPost(a, "/api/transactions", body).andExpect(status().isCreated());
        doGet(a, "/api/transactions").andExpect(jsonPath("$", hasSize(1)));
        doGet(b, "/api/transactions").andExpect(jsonPath("$", hasSize(0)));
    }
}
