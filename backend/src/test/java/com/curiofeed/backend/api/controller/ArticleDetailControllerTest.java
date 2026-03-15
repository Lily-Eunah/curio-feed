package com.curiofeed.backend.api.controller;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.service.ArticleDetailService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleDetailController.class)
@Import(com.curiofeed.backend.api.controller.advice.GlobalExceptionHandler.class)
class ArticleDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleDetailService articleDetailService;

    @Test
    @DisplayName("유효한 ID와 Level로 호출 시 성공 응답을 반환한다")
    void shouldReturn200_andDetailResponse_whenValidRequest() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();
        DifficultyLevel level = DifficultyLevel.MEDIUM;

        ArticleDetailResponse mockResponse = ArticleDetailResponse.builder()
                .id(articleId)
                .title("Test Article")
                .availableLevels(List.of(DifficultyLevel.EASY, DifficultyLevel.MEDIUM))
                .content(ArticleDetailResponse.ArticleContentDto.builder()
                        .level(DifficultyLevel.MEDIUM)
                        .content("This is medium content.")
                        .build())
                .build();

        given(articleDetailService.getArticleDetail(eq(articleId), eq(level))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/articles/{id}", articleId)
                        .param("level", level.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleId.toString()))
                .andExpect(jsonPath("$.title").value("Test Article"))
                .andExpect(jsonPath("$.availableLevels[0]").value("EASY"))
                .andExpect(jsonPath("$.availableLevels[1]").value("MEDIUM"))
                .andExpect(jsonPath("$.content.level").value("MEDIUM"))
                .andExpect(jsonPath("$.content.content").value("This is medium content."));
    }

    @Test
    @DisplayName("Level 파라미터가 누락된 경우 기본값(EASY)으로 Service를 호출한다")
    void shouldUseDefaultLevelEasy_whenLevelParamIsOmitted() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();

        ArticleDetailResponse mockResponse = ArticleDetailResponse.builder()
                .id(articleId)
                .content(ArticleDetailResponse.ArticleContentDto.builder()
                        .level(DifficultyLevel.EASY)
                        .content("Default easy content")
                        .build())
                .build();

        // Expect service to be called with EASY
        given(articleDetailService.getArticleDetail(eq(articleId), eq(DifficultyLevel.EASY))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/articles/{id}", articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.level").value("EASY"));
    }

    @Test
    @DisplayName("Service에서 EntityNotFoundException 발생 시 404를 반환한다")
    void shouldReturn404_whenServiceThrowsNotFoundException() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();
        given(articleDetailService.getArticleDetail(any(), any()))
                .willThrow(new EntityNotFoundException("Article not found"));

        // when & then
        mockMvc.perform(get("/api/articles/{id}", articleId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("잘못된 형식의 UUID 요청 시 400을 반환한다")
    void shouldReturn400_whenIdFormatIsInvalid() throws Exception {
        // when & then
        mockMvc.perform(get("/api/articles/{id}", "invalid-uuid-format"))
                .andExpect(status().isBadRequest());
    }
}
