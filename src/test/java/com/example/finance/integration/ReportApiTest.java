package com.example.finance.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiTest extends IntegrationTestBase {

    private MockHttpSession userWithFixtureData() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Freelance", "type", "INCOME")).andExpect(status().isCreated());
        createTransaction(s, "Salary", "3000.00", "2026-10-02");
        createTransaction(s, "Freelance", "500.00", "2026-10-03");
        createTransaction(s, "Food", "250.00", "2026-10-04");
        createTransaction(s, "Rent", "750.00", "2026-10-05");
        return s;
    }

    @Test
    void monthlyReport_matchesFixture() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doGet(s, "/api/reports/monthly/2026/10").andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.0))
                .andExpect(jsonPath("$.totalIncome.Freelance").value(500.0))
                .andExpect(jsonPath("$.totalExpenses.Food").value(250.0))
                .andExpect(jsonPath("$.totalExpenses.Rent").value(750.0))
                .andExpect(jsonPath("$.netSavings").value(2500.0));
    }

    @Test
    void yearlyReport_matchesFixture() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doGet(s, "/api/reports/yearly/2026").andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").doesNotExist())
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.0))
                .andExpect(jsonPath("$.totalExpenses.Rent").value(750.0))
                .andExpect(jsonPath("$.netSavings").value(2500.0));
    }

    @Test
    void report_emptyPeriod_returnsZeroAndEmptyMaps() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doGet(s, "/api/reports/monthly/2026/09").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").isEmpty())
                .andExpect(jsonPath("$.totalExpenses").isEmpty())
                .andExpect(jsonPath("$.netSavings").value(0.0));
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
        Long bigId = createTransaction(b, "Salary", "99999.00", "2026-10-02");

        doGet(a, "/api/reports/monthly/2026/10").andExpect(jsonPath("$.totalIncome.Salary").value(3000.0));
        doGet(b, "/api/reports/monthly/2026/10").andExpect(jsonPath("$.totalIncome.Salary").value(99999.0));

        doDelete(b, "/api/transactions/" + bigId).andExpect(status().isOk());
        doGet(b, "/api/reports/monthly/2026/10").andExpect(jsonPath("$.netSavings").value(0.0));
    }

    @Test
    void report_leapDayBelongsToFebruary() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        createTransaction(s, "Food", "10.00", "2024-02-29");
        createTransaction(s, "Food", "20.00", "2024-03-01");
        doGet(s, "/api/reports/monthly/2024/2").andExpect(jsonPath("$.totalExpenses.Food").value(10.0));
        doGet(s, "/api/reports/monthly/2024/3").andExpect(jsonPath("$.totalExpenses.Food").value(20.0));
        doGet(s, "/api/reports/yearly/2024").andExpect(jsonPath("$.totalExpenses.Food").value(30.0));
    }
}
