package com.example.finance.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalAndReportApiTest extends IntegrationTestBase {

    private Map<String, Object> goalBody(String name, String target, String targetDate, String startDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("goalName", name);
        body.put("targetAmount", new java.math.BigDecimal(target));
        body.put("targetDate", targetDate);
        body.put("startDate", startDate);
        return body;
    }

    /** Income 3000 + 500, expenses 250 + 750, all inside October 2026. */
    private MockHttpSession userWithFixtureData() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Freelance", "type", "INCOME")).andExpect(status().isCreated());
        createTransaction(s, "INCOME", "3000.00", "2026-10-02", categoryId(s, "Salary"));
        createTransaction(s, "INCOME", "500.00", "2026-10-03", categoryId(s, "Freelance"));
        createTransaction(s, "EXPENSE", "250.00", "2026-10-04", categoryId(s, "Food"));
        createTransaction(s, "EXPENSE", "750.00", "2026-10-05", categoryId(s, "Rent"));
        return s;
    }

    @Test
    void goal_progressMatchesFixture() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doPost(s, "/api/goals", goalBody("Emergency fund", "10000.00", "2027-06-30", "2026-10-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalName").value("Emergency fund"))
                .andExpect(jsonPath("$.progress").value(2500.0))
                .andExpect(jsonPath("$.completionPercentage").value(25.0))
                .andExpect(jsonPath("$.remaining").value(7500.0));
    }

    @Test
    void goal_ignoresTransactionsBeforeStartDate() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doPost(s, "/api/goals", goalBody("Later", "10000.00", "2027-06-30", "2026-10-05"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progress").value(-750.0))
                .andExpect(jsonPath("$.completionPercentage").value(-7.5))
                .andExpect(jsonPath("$.remaining").value(10750.0));
    }

    @Test
    void goal_crud_andOwnership() throws Exception {
        MockHttpSession owner = userWithFixtureData();
        MockHttpSession other = registerAndLogin(uniqueEmail());

        String created = doPost(owner, "/api/goals", goalBody("Trip", "1000.00", "2027-01-01", "2026-10-01"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        doGet(owner, "/api/goals").andExpect(jsonPath("$", hasSize(1)));
        doGet(owner, "/api/goals/" + id).andExpect(status().isOk());

        // 2500 saved of 1000 target: over 100% is valid and not clamped
        doGet(owner, "/api/goals/" + id)
                .andExpect(jsonPath("$.completionPercentage").value(250.0))
                .andExpect(jsonPath("$.remaining").value(-1500.0));

        doPut(owner, "/api/goals/" + id, Map.of("targetAmount", 5000, "targetDate", "2027-02-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(5000.0))
                .andExpect(jsonPath("$.completionPercentage").value(50.0));

        doGet(other, "/api/goals/" + id).andExpect(status().isNotFound());
        doPut(other, "/api/goals/" + id, Map.of("targetAmount", 1, "targetDate", "2027-02-01"))
                .andExpect(status().isNotFound());
        doDelete(other, "/api/goals/" + id).andExpect(status().isNotFound());
        doGet(other, "/api/goals").andExpect(jsonPath("$", hasSize(0)));

        doDelete(owner, "/api/goals/" + id).andExpect(status().isNoContent());
        doGet(owner, "/api/goals/" + id).andExpect(status().isNotFound());
    }

    @Test
    void goal_validationRules() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/goals", goalBody("Bad", "0", "2027-01-01", "2026-10-01")).andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody("Bad", "100.00", "2026-09-01", "2026-10-01")).andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody("Bad", "100.00", "2027-01-01", "2026-10-11")).andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody(" ", "100.00", "2027-01-01", "2026-10-01")).andExpect(status().isBadRequest());
    }

    @Test
    void goal_changesWhenTransactionIsDeleted() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "INCOME", "1000.00", "2026-10-02", categoryId(s, "Salary"));
        doPost(s, "/api/goals", goalBody("Goal", "2000.00", "2027-01-01", "2026-10-01"))
                .andExpect(jsonPath("$.progress").value(1000.0));
        doDelete(s, "/api/transactions/" + id).andExpect(status().isNoContent());
        doGet(s, "/api/goals").andExpect(jsonPath("$[0].progress").value(0.0));
    }

    @Test
    void monthlyReport_matchesFixture() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doGet(s, "/api/reports/monthly/2026/10").andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2026-10"))
                .andExpect(jsonPath("$.totalIncome").value(3500.0))
                .andExpect(jsonPath("$.totalExpenses").value(1000.0))
                .andExpect(jsonPath("$.netSavings").value(2500.0))
                .andExpect(jsonPath("$.incomeByCategory", hasSize(2)))
                .andExpect(jsonPath("$.expensesByCategory", hasSize(2)));
    }

    @Test
    void yearlyReport_matchesFixture_andEmptyPeriodIsZero() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doGet(s, "/api/reports/yearly/2026").andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2026"))
                .andExpect(jsonPath("$.netSavings").value(2500.0));
        doGet(s, "/api/reports/monthly/2026/09").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0.0))
                .andExpect(jsonPath("$.totalExpenses").value(0.0))
                .andExpect(jsonPath("$.netSavings").value(0.0))
                .andExpect(jsonPath("$.incomeByCategory", hasSize(0)))
                .andExpect(jsonPath("$.expensesByCategory", hasSize(0)));
    }

    @Test
    void report_rejectsInvalidMonthAndYear() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doGet(s, "/api/reports/monthly/2026/0").andExpect(status().isBadRequest());
        doGet(s, "/api/reports/monthly/2026/13").andExpect(status().isBadRequest());
        doGet(s, "/api/reports/monthly/abc/10").andExpect(status().isBadRequest());
        doGet(s, "/api/reports/yearly/1800").andExpect(status().isBadRequest());
    }

    @Test
    void report_neverIncludesOtherUsersData_andReflectsDeletes() throws Exception {
        MockHttpSession a = userWithFixtureData();
        MockHttpSession b = registerAndLogin(uniqueEmail());
        Long bigId = createTransaction(b, "INCOME", "99999.00", "2026-10-02", categoryId(b, "Salary"));

        doGet(a, "/api/reports/monthly/2026/10").andExpect(jsonPath("$.totalIncome").value(3500.0));
        doGet(b, "/api/reports/monthly/2026/10").andExpect(jsonPath("$.totalIncome").value(99999.0));

        doDelete(b, "/api/transactions/" + bigId).andExpect(status().isNoContent());
        doGet(b, "/api/reports/monthly/2026/10").andExpect(jsonPath("$.totalIncome").value(0.0));
    }

    @Test
    void report_leapDayBelongsToFebruary() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        createTransaction(s, "EXPENSE", "10.00", "2024-02-29", categoryId(s, "Food"));
        createTransaction(s, "EXPENSE", "20.00", "2024-03-01", categoryId(s, "Food"));
        doGet(s, "/api/reports/monthly/2024/2").andExpect(jsonPath("$.totalExpenses").value(10.0));
        doGet(s, "/api/reports/monthly/2024/3").andExpect(jsonPath("$.totalExpenses").value(20.0));
        doGet(s, "/api/reports/yearly/2024").andExpect(jsonPath("$.totalExpenses").value(30.0));
    }
}
