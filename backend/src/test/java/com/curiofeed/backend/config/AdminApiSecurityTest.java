package com.curiofeed.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies {@link AdminAuthInterceptor} guards the admin surface while leaving
 * consumer endpoints and CORS pre-flight open. The expected token is
 * {@code test-admin-token} (from application-test.yml). This class intentionally
 * does NOT import the default-header test config, so requests are unauthenticated
 * unless the header is set explicitly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AdminApiSecurityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("admin GET without token is rejected with 401")
    void adminWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin POST without token is rejected with 401 (no pipeline trigger)")
    void adminPostWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin GET with wrong token is rejected with 401")
    void adminWithWrongTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/articles").header("X-Admin-Token", "nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin GET with correct token is allowed")
    void adminWithCorrectTokenIsAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/articles").header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("consumer feed endpoint stays public")
    void consumerEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/articles").param("level", "MEDIUM"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CORS pre-flight for admin passes without a token")
    void adminPreflightIsAllowed() throws Exception {
        mockMvc.perform(options("/api/admin/articles")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "https://curio-feed.pages.dev"))
                .andExpect(status().isOk());
    }
}
