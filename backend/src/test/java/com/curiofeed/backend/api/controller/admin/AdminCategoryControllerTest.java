package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.domain.entity.Category;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(AdminMockMvcTokenConfig.class)
class AdminCategoryControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        // Clean seeded data so tests see only what they insert (all within @Transactional — rolled back after each test)
        em.createQuery("DELETE FROM Quiz").executeUpdate();
        em.createQuery("DELETE FROM Vocabulary").executeUpdate();
        em.createQuery("DELETE FROM ArticleContent").executeUpdate();
        em.createQuery("DELETE FROM ArticleGenerationSubJob").executeUpdate();
        em.createQuery("DELETE FROM ArticleGenerationJob").executeUpdate();
        em.createQuery("DELETE FROM Article").executeUpdate();
        em.createQuery("DELETE FROM Category").executeUpdate();
        persistCategory("tech", "Tech", 1, true);
        persistCategory("science", "Science", 2, true);
        persistCategory("business", "Business", 3, true);
        persistCategory("culture", "Culture", 4, false);
        em.flush();
    }

    @Test
    @DisplayName("GET /api/admin/categories - 200 with active categories ordered by sortOrder")
    void listCategories_returnsActiveCategoriesOrdered() throws Exception {
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Tech"))
                .andExpect(jsonPath("$[0].slug").value("tech"))
                .andExpect(jsonPath("$[0].sortOrder").value(1))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[1].name").value("Science"))
                .andExpect(jsonPath("$[1].sortOrder").value(2))
                .andExpect(jsonPath("$[2].name").value("Business"))
                .andExpect(jsonPath("$[2].sortOrder").value(3));
    }

    @Test
    @DisplayName("GET /api/admin/categories - excludes inactive categories")
    void listCategories_excludesInactive() throws Exception {
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'culture')]").isEmpty());
    }

    @Test
    @DisplayName("GET /api/admin/categories - returns empty list when no active categories exist")
    void listCategories_emptyWhenNoActive() throws Exception {
        // Deactivate all categories
        em.createQuery("UPDATE Category c SET c.active = false").executeUpdate();
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/categories - response contains all required fields")
    void listCategories_responseHasRequiredFields() throws Exception {
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].slug").exists())
                .andExpect(jsonPath("$[0].sortOrder").exists())
                .andExpect(jsonPath("$[0].active").exists());
    }

    @Test
    @DisplayName("GET /api/admin/categories?all=true - includes inactive categories")
    void listCategories_allIncludesInactive() throws Exception {
        mockMvc.perform(get("/api/admin/categories?all=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.slug == 'culture')]").exists());
    }

    @Test
    @DisplayName("POST /api/admin/categories - creates new category")
    void createCategory() throws Exception {
        String payload = """
            {
                "name": "new-cat",
                "displayName": "New Category",
                "sortOrder": 5,
                "active": true
            }
        """;
        mockMvc.perform(post("/api/admin/categories")
                .contentType("application/json")
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Category"))
                .andExpect(jsonPath("$.slug").value("new-cat"));
    }

    @Test
    @DisplayName("PATCH /api/admin/categories/{id} - updates category")
    void updateCategory() throws Exception {
        String listResponse = mockMvc.perform(get("/api/admin/categories"))
                .andReturn().getResponse().getContentAsString();
        String id = listResponse.split("\"id\":\"")[1].split("\"")[0];

        String payload = """
            {
                "name": "updated-cat",
                "displayName": "Updated Category",
                "sortOrder": 10,
                "active": false
            }
        """;
        mockMvc.perform(patch("/api/admin/categories/" + id)
                .contentType("application/json")
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Category"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("DELETE /api/admin/categories/{id} - deletes category if no articles")
    void deleteCategory() throws Exception {
        String listResponse = mockMvc.perform(get("/api/admin/categories"))
                .andReturn().getResponse().getContentAsString();
        String id = listResponse.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(delete("/api/admin/categories/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/categories?all=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)); // 4 originally, 1 deleted
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void persistCategory(String name, String displayName, int sortOrder, boolean active) {
        Category category = newInstance(Category.class);
        setField(category, "name", name);
        setField(category, "displayName", displayName);
        setField(category, "sortOrder", sortOrder);
        setField(category, "active", active);
        em.persist(category);
    }

    @SuppressWarnings("unchecked")
    private <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("Field not found: " + fieldName);
    }
}
