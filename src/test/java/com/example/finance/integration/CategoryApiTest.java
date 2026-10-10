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
    void list_containsAllSevenDefaults() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doGet(s, "/api/categories").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Salary")))
                .andExpect(jsonPath("$[*].name", hasItem("Food")))
                .andExpect(jsonPath("$[*].name", hasItem("Rent")))
                .andExpect(jsonPath("$[*].name", hasItem("Transportation")))
                .andExpect(jsonPath("$[*].name", hasItem("Entertainment")))
                .andExpect(jsonPath("$[*].name", hasItem("Healthcare")))
                .andExpect(jsonPath("$[*].name", hasItem("Utilities")));
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
        doPost(a, "/api/categories", Map.of("name", "  Pets  ", "type", "EXPENSE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pets"))
                .andExpect(jsonPath("$.defaultCategory").value(false));
        doGet(a, "/api/categories").andExpect(jsonPath("$[*].name", hasItem("Pets")));
        doGet(b, "/api/categories").andExpect(jsonPath("$[?(@.name == 'Pets')]").isEmpty());
    }

    @Test
    void create_duplicateNameIgnoringCase_returnsConflict() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "EXPENSE")).andExpect(status().isCreated());
        doPost(s, "/api/categories", Map.of("name", "pETS", "type", "EXPENSE")).andExpect(status().isConflict());
    }

    @Test
    void create_duplicateOfDefault_returnsConflict() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "food", "type", "EXPENSE")).andExpect(status().isConflict());
    }

    @Test
    void create_invalidInput_returnsBadRequest() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", " ", "type", "EXPENSE")).andExpect(status().isBadRequest());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "OTHER")).andExpect(status().isBadRequest());
    }

    @Test
    void delete_defaultCategory_isForbidden() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doDelete(s, "/api/categories/Food").andExpect(status().isForbidden());
    }

    @Test
    void delete_customCategory_works_andUnknownIsNotFound() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Pet Care", "type", "EXPENSE")).andExpect(status().isCreated());
        doDelete(s, "/api/categories/Pet Care").andExpect(status().isNoContent());
        doDelete(s, "/api/categories/Pet Care").andExpect(status().isNotFound());
    }

    @Test
    void delete_categoryUsedByTransaction_returnsConflict() throws Exception {
        MockHttpSession s = registerAndLogin(uniqueEmail());
        doPost(s, "/api/categories", Map.of("name", "Pets", "type", "EXPENSE")).andExpect(status().isCreated());
        createTransaction(s, "EXPENSE", "10.00", "2026-10-05", categoryId(s, "Pets"));
        doDelete(s, "/api/categories/Pets").andExpect(status().isConflict());
    }

    @Test
    void otherUsersCategory_cannotBeUsedOrDeleted() throws Exception {
        MockHttpSession a = registerAndLogin(uniqueEmail());
        MockHttpSession b = registerAndLogin(uniqueEmail());
        doPost(a, "/api/categories", Map.of("name", "Secret", "type", "EXPENSE")).andExpect(status().isCreated());
        Long secretId = categoryId(a, "Secret");

        doPost(b, "/api/transactions", txBody("EXPENSE", "5.00", "2026-10-05", secretId, "steal"))
                .andExpect(status().isNotFound());
        doDelete(b, "/api/categories/Secret").andExpect(status().isNotFound());
    }
}
