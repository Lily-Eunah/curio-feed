package com.curiofeed.backend.api.controller;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.service.ArticleDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleDetailController {

    private final ArticleDetailService articleDetailService;

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getArticleDetail(
            @PathVariable UUID id,
            @RequestParam(required = false) DifficultyLevel level
    ) {
        // TODO: Implement according to TDD planning
        return null;
    }
}
