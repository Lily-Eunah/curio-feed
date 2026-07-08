package com.curiofeed.backend.api.controller.admin;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Adds a default {@code X-Admin-Token} header to every MockMvc request so the existing
 * functional admin tests authenticate against {@link com.curiofeed.backend.config.AdminAuthInterceptor}.
 * The token value matches {@code admin.api.token} in {@code application-test.yml}.
 *
 * <p>Runs last so the default request (and its header) is not overwritten by Spring Boot's
 * own MockMvc customizer.
 */
@TestConfiguration
public class AdminMockMvcTokenConfig {

    static final String TEST_ADMIN_TOKEN = "test-admin-token";

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    MockMvcBuilderCustomizer adminTokenDefaultHeader() {
        return builder -> builder.defaultRequest(
                get("/").header("X-Admin-Token", TEST_ADMIN_TOKEN));
    }
}
