package com.curiofeed.backend.config;

import com.curiofeed.backend.api.controller.ArticleController;
import com.curiofeed.backend.api.dto.CursorPageResponse;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.service.ArticleAudioService;
import com.curiofeed.backend.domain.service.ArticleDetailService;
import com.curiofeed.backend.domain.service.ArticleFeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleController.class)
@Import({CorsConfig.class, com.curiofeed.backend.api.controller.advice.GlobalExceptionHandler.class})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleFeedService feedService;

    @MockBean
    private ArticleDetailService articleDetailService;

    @MockBean
    private ArticleAudioService articleAudioService;

    @Test
    @DisplayName("Cloudflare Pages 미리보기 Origin (https://5301a69b.curio-feed.pages.dev) 요청 시 CORS 헤더가 정상 반환된다")
    void shouldAllowCloudflarePagesPreviewOrigin() throws Exception {
        given(feedService.getFeed(any(), eq(20), eq(DifficultyLevel.MEDIUM)))
                .willReturn(new CursorPageResponse<>(List.of(), null, false));

        mockMvc.perform(options("/api/articles")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "https://5301a69b.curio-feed.pages.dev"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://5301a69b.curio-feed.pages.dev"));
    }

    @Test
    @DisplayName("Cloudflare Pages 프로덕션 Origin (https://curio-feed.pages.dev) 요청 시 CORS 헤더가 정상 반환된다")
    void shouldAllowCloudflarePagesProductionOrigin() throws Exception {
        given(feedService.getFeed(any(), eq(20), eq(DifficultyLevel.MEDIUM)))
                .willReturn(new CursorPageResponse<>(List.of(), null, false));

        mockMvc.perform(options("/api/articles")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "https://curio-feed.pages.dev"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://curio-feed.pages.dev"));
    }
}
