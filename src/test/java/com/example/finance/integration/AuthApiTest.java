package com.example.finance.integration;

import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Test
    void register_returnsCreatedWithMessageAndUserId() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(uniqueEmail()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_storesHashedPasswordAndNormalizedUsername() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(email.toUpperCase()))))
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void register_duplicateUsername_returnsConflict() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(email)))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(email.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void register_invalidFields_returnsValidationError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "not-an-email");
        body.put("password", "short");
        body.put("fullName", "");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void register_phoneNumberIsMandatoryAndValidated() throws Exception {
        Map<String, Object> missing = registerBody(uniqueEmail());
        missing.remove("phoneNumber");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(missing)))
                .andExpect(status().isBadRequest());

        Map<String, Object> bad = registerBody(uniqueEmail());
        bad.put("phoneNumber", "abc");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void login_success_returnsMessage() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registerBody(email)))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registerBody(email)))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void login_unknownUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_thenAccessProtectedRoute_works() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerAndLogin(email);
        doGet(session, "/api/transactions").andExpect(status().isOk());
        doGet(session, "/api/categories").andExpect(status().isOk());
    }

    @Test
    void protectedRoutes_withoutLogin_returnUnauthorized() throws Exception {
        String[] urls = {"/api/transactions", "/api/categories", "/api/goals", "/api/reports/yearly/2026",
                "/api/reports/monthly/2026/10"};
        for (String url : urls) {
            mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void logout_withoutLogin_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returnsMessage_andInvalidatesSession() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        doPost(session, "/api/auth/logout", Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRoute_whenLoggedIn_returnsJsonNotFound() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        doGet(session, "/api/does-not-exist").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
