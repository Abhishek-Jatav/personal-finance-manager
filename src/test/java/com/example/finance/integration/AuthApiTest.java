package com.example.finance.integration;

import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    private String registerBody(String email) throws Exception {
        return json(Map.of("fullName", "Alex Example", "email", email, "password", PASSWORD,
                "phoneNumber", "+91 98765 43210"));
    }

    @Test
    void register_returnsCreatedWithoutPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_storesHashedPasswordAndNormalizedEmail() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase())))
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void register_invalidFields_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void register_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void login_unknownUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_thenAccessProtectedRoute_works() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerAndLogin(email);
        doGet(session, "/api/auth/me").andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        doGet(session, "/api/transactions").andExpect(status().isOk());
    }

    @Test
    void protectedRoutes_withoutLogin_returnUnauthorized() throws Exception {
        String[] urls = {"/api/transactions", "/api/categories", "/api/goals", "/api/reports/yearly/2026",
                "/api/reports/monthly/2026/10", "/api/auth/me"};
        for (String url : urls) {
            mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        doPost(session, "/api/auth/logout", Map.of()).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequest_withoutCsrfToken_isForbidden() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Pets", "type", "EXPENSE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unknownRoute_whenLoggedIn_returnsJsonNotFound() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());
        doGet(session, "/api/does-not-exist").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
