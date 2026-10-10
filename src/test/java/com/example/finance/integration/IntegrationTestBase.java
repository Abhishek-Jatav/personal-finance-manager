package com.example.finance.integration;

import com.example.finance.support.TestClockConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers. Every test uses its own random user, so tests never depend on each other. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected MockHttpSession registerAndLogin(String email) throws Exception {
        Map<String, Object> register = new HashMap<>();
        register.put("fullName", "Test User");
        register.put("email", email);
        register.put("password", PASSWORD);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(register)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected ResultActions doGet(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(get(url).session(session));
    }

    protected ResultActions doPost(MockHttpSession session, String url, Object body) throws Exception {
        return mockMvc.perform(post(url).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected ResultActions doPut(MockHttpSession session, String url, Object body) throws Exception {
        return mockMvc.perform(put(url).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected ResultActions doDelete(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(delete(url).session(session).with(csrf()));
    }

    protected Long categoryId(MockHttpSession session, String name) throws Exception {
        String body = doGet(session, "/api/categories").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(body)) {
            if (node.get("name").asText().equals(name)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Category not found in test: " + name);
    }

    protected Map<String, Object> txBody(String type, String amount, String date, Long categoryId, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", new BigDecimal(amount));
        body.put("date", date);
        body.put("categoryId", categoryId);
        body.put("description", description);
        body.put("type", type);
        return body;
    }

    protected Long createTransaction(MockHttpSession session, String type, String amount, String date,
                                     Long categoryId) throws Exception {
        String body = doPost(session, "/api/transactions", txBody(type, amount, date, categoryId, "test"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
