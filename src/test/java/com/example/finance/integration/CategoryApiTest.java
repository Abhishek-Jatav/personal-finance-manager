package com.example.finance.integration;

import com.example.finance.config.DataInitializer;
import com.example.finance.entity.Category;
import com.example.finance.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiTest extends IntegrationTestBase {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void list_containsAllSevenDefaults_marked_notCustom() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doGet(s, "/api/categories").andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].name", hasItem("Salary")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Food")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Rent")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Transportation")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Entertainment")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Healthcare")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Utilities")))
                .andExpect(jsonPath("$.categories[?(@.name == 'Salary')].type", hasItem("INCOME")))
                .andExpect(jsonPath("$.categories[?(@.name == 'Salary')].isCustom", hasItem(false)))
                .andExpect(jsonPath("$.categories[?(@.name == 'Salary')].custom", hasItem(false)));
    }

    @Test
    void seeding_isIdempotent() {
        dataInitializer.run(null);
        dataInitializer.run(null);
        long defaults = categoryRepository.findAll().stream().filter(Category::isDefaultCategory).count();
        assertThat(defaults).isEqualTo(7);
    }

    @Test
    void create_customCategory_isPrivateToOwner() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        doPost(a, "/api/categories", Map.of("name", "  SideBusinessIncome  ", "type", "INCOME"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SideBusinessIncome"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.isCustom").value(true))
                .andExpect(jsonPath("$.custom").value(true));
        doGet(a, "/api/categories").andExpect(jsonPath("$.categories[*].name", hasItem("SideBusinessIncome")));
        doGet(b, "/api/categories")
                .andExpect(jsonPath("$.categories[?(@.name == 'SideBusinessIncome')]").isEmpty());
    }

    @Test
    void create_sameNameForDifferentUsers_isAllowed() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        doPost(a, "/api/categories", Map.of("name", "Shared", "type", "EXPENSE")).andExpect(status().isCreated());
        doPost(b, "/api/categories", Map.of("name", "Shared", "type", "EXPENSE")).andExpect(status().isCreated());
    }

    @Test
    void create_duplicateNameIgnoringCase_returnsConflict() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "EXPENSE")).andExpect(status().isCreated());
        doPost(s, "/api/categories", Map.of("name", "pETS", "type", "EXPENSE")).andExpect(status().isConflict());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "INCOME")).andExpect(status().isConflict());
    }

    @Test
    void create_duplicateOfDefault_returnsConflict() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "food", "type", "EXPENSE")).andExpect(status().isConflict());
        doPost(s, "/api/categories", Map.of("name", "Salary", "type", "EXPENSE")).andExpect(status().isConflict());
    }

    @Test
    void create_invalidInput_returnsBadRequest() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", " ", "type", "EXPENSE")).andExpect(status().isBadRequest());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "OTHER")).andExpect(status().isBadRequest());
        doPost(s, "/api/categories", Map.of("name", "Pets")).andExpect(status().isBadRequest());
    }

    @Test
    void delete_defaultCategory_isForbidden() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doDelete(s, "/api/categories/Food").andExpect(status().isForbidden());
    }

    @Test
    void delete_customCategory_returnsMessage_thenNotFound() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Pet Care", "type", "EXPENSE")).andExpect(status().isCreated());
        doDelete(s, "/api/categories/Pet Care")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
        doDelete(s, "/api/categories/Pet Care").andExpect(status().isNotFound());
    }

    @Test
    void delete_categoryUsedByTransaction_returnsBadRequest() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "EXPENSE")).andExpect(status().isCreated());
        Long txId = createTransaction(s, "Pets", "10.00", "2026-10-05");
        doDelete(s, "/api/categories/Pets").andExpect(status().isBadRequest());

        doDelete(s, "/api/transactions/" + txId).andExpect(status().isOk());
        doDelete(s, "/api/categories/Pets").andExpect(status().isOk());
    }

    @Test
    void delete_otherUsersCategory_returnsNotFound() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        doPost(a, "/api/categories", Map.of("name", "Secret", "type", "EXPENSE")).andExpect(status().isCreated());
        doDelete(b, "/api/categories/Secret").andExpect(status().isNotFound());
    }
}
