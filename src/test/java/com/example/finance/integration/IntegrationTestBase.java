package com.example.finance.integration;

import com.example.finance.support.TestClockConfig;
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

    protected Map<String, Object> registerBody(String username) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", PASSWORD);
        body.put("fullName", "Test User");
        body.put("phoneNumber", "+1234567890");
        return body;
    }

    protected MockHttpSession registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(username))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected ResultActions doGet(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(get(url).session(session));
    }

    protected ResultActions doPost(MockHttpSession session, String url, Object body) throws Exception {
        return mockMvc.perform(post(url).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected ResultActions doPut(MockHttpSession session, String url, Object body) throws Exception {
        return mockMvc.perform(put(url).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    protected ResultActions doDelete(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(delete(url).session(session));
    }

    protected Map<String, Object> txBody(String category, String amount, String date, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", new BigDecimal(amount));
        body.put("date", date);
        body.put("category", category);
        body.put("description", description);
        return body;
    }

    protected Long createTransaction(MockHttpSession session, String category, String amount, String date)
            throws Exception {
        String body = doPost(session, "/api/transactions", txBody(category, amount, date, "test"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
