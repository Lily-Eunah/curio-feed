package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.domain.service.AbComparisonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAbCompareController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminMockMvcTokenConfig.class)
@TestPropertySource(properties = "admin.api.token=test-admin-token")
class AdminAbCompareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AbComparisonService abComparisonService;

    @Test
    @DisplayName("GET /api/admin/ab-compare returns comparison metrics")
    void testCompareEndpoint() throws Exception {
        when(abComparisonService.compareVersions(anyString(), anyString()))
                .thenReturn(Map.of("promptVersionA", "v3.0", "promptVersionB", "v2.0", "scoreA", 0.92, "scoreB", 0.85));

        mockMvc.perform(get("/api/admin/ab-compare")
                        .header("X-Admin-Token", "test-admin-token")
                        .param("promptVersionA", "v3.0")
                        .param("promptVersionB", "v2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptVersionA").value("v3.0"))
                .andExpect(jsonPath("$.promptVersionB").value("v2.0"))
                .andExpect(jsonPath("$.scoreA").value(0.92))
                .andExpect(jsonPath("$.scoreB").value(0.85));
    }
}
