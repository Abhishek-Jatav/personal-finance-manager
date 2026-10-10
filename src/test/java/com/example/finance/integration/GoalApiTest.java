package com.example.finance.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalApiTest extends IntegrationTestBase {

    private Map<String, Object> goalBody(String name, String target, String targetDate, String startDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("goalName", name);
        body.put("targetAmount", new BigDecimal(target));
        body.put("targetDate", targetDate);
        if (startDate != null) {
            body.put("startDate", startDate);
        }
        return body;
    }

    /** Income 3000 + 500, expenses 250 + 750, all in October 2026 (today in the tests is 2026-10-10). */
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
    void create_progressMatchesFixture() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doPost(s, "/api/goals", goalBody("Emergency Fund", "10000.00", "2027-06-30", "2026-10-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.targetAmount").value(10000.0))
                .andExpect(jsonPath("$.targetDate").value("2027-06-30"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.currentProgress").value(2500.0))
                .andExpect(jsonPath("$.progressPercentage").value(25.0))
                .andExpect(jsonPath("$.remainingAmount").value(7500.0));
    }

    @Test
    void create_startDateDefaultsToToday() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doPost(s, "/api/goals", goalBody("Defaults", "1000.00", "2027-06-30", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-10-10"))
                .andExpect(jsonPath("$.currentProgress").value(0.0));
    }

    @Test
    void progress_ignoresTransactionsBeforeStartDate_andCanBeNegative() throws Exception {
        MockHttpSession s = userWithFixtureData();
        doPost(s, "/api/goals", goalBody("Later", "10000.00", "2027-06-30", "2026-10-05"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentProgress").value(-750.0))
                .andExpect(jsonPath("$.progressPercentage").value(-7.5))
                .andExpect(jsonPath("$.remainingAmount").value(10750.0));
    }

    @Test
    void create_validationRules() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/goals", goalBody("Bad", "0", "2027-01-01", "2026-10-01")).andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody("Bad", "100.00", "2026-10-10", "2026-10-01"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody("Bad", "100.00", "2026-01-01", "2025-01-01"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody("Bad", "100.00", "2027-01-01", "2026-10-11"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody(" ", "100.00", "2027-01-01", "2026-10-01"))
                .andExpect(status().isBadRequest());
        doPost(s, "/api/goals", goalBody("Bad", "100.00", "2026-10-20", "2026-10-25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crud_listGetUpdateDelete() throws Exception {
        MockHttpSession s = userWithFixtureData();
        String created = doPost(s, "/api/goals", goalBody("Trip", "1000.00", "2027-01-01", "2026-10-01"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        doGet(s, "/api/goals").andExpect(status().isOk()).andExpect(jsonPath("$.goals", hasSize(1)));

        // 2500 saved for a 1000 target: above 100 percent is valid and is not clamped
        doGet(s, "/api/goals/" + id).andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(250.0))
                .andExpect(jsonPath("$.remainingAmount").value(-1500.0));

        doPut(s, "/api/goals/" + id, Map.of("targetAmount", 5000, "targetDate", "2027-02-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(5000.0))
                .andExpect(jsonPath("$.targetDate").value("2027-02-01"))
                .andExpect(jsonPath("$.goalName").value("Trip"))
                .andExpect(jsonPath("$.progressPercentage").value(50.0));

        doPut(s, "/api/goals/" + id, Map.of("targetAmount", 4000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value("2027-02-01"));

        doPut(s, "/api/goals/" + id, Map.of("targetAmount", -1)).andExpect(status().isBadRequest());
        doPut(s, "/api/goals/" + id, Map.of("targetDate", "2026-10-10")).andExpect(status().isBadRequest());

        doDelete(s, "/api/goals/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
        doGet(s, "/api/goals/" + id).andExpect(status().isNotFound());
        doGet(s, "/api/goals/abc").andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersGoal_isForbidden_andUnknownGoalIsNotFound() throws Exception {
        MockHttpSession owner = registerAndLogin(uniqueEmail());
        MockHttpSession other = registerAndLogin(uniqueEmail());
        String created = doPost(owner, "/api/goals", goalBody("Mine", "1000.00", "2027-01-01", "2026-10-01"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        doGet(other, "/api/goals/" + id).andExpect(status().isForbidden());
        doPut(other, "/api/goals/" + id, Map.of("targetAmount", 1)).andExpect(status().isForbidden());
        doDelete(other, "/api/goals/" + id).andExpect(status().isForbidden());
        doGet(other, "/api/goals").andExpect(jsonPath("$.goals", hasSize(0)));
        doGet(owner, "/api/goals/999999").andExpect(status().isNotFound());
    }

    @Test
    void progress_changesWhenTransactionIsDeleted() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        Long id = createTransaction(s, "Salary", "1000.00", "2026-10-02");
        doPost(s, "/api/goals", goalBody("Goal", "2000.00", "2027-01-01", "2026-10-01"))
                .andExpect(jsonPath("$.currentProgress").value(1000.0));
        doDelete(s, "/api/transactions/" + id).andExpect(status().isOk());
        doGet(s, "/api/goals").andExpect(jsonPath("$.goals[0].currentProgress").value(0.0));
    }
}
