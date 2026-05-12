package com.curiofeed.backend.api.controller;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.api.dto.CursorPageResponse;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.service.ArticleDetailService;
import com.curiofeed.backend.domain.service.ArticleFeedService;
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
public class ArticleController {

    private final ArticleFeedService feedService;
    private final ArticleDetailService articleDetailService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<ArticleFeedResponse>> getArticleFeed(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "level", required = false, defaultValue = "MEDIUM") DifficultyLevel level
    ) {
        return ResponseEntity.ok(feedService.getFeed(cursor, size, level));
    }

    @GetMapping("/bulk")
    public ResponseEntity<java.util.List<ArticleFeedResponse>> getArticlesBulk(
            @RequestParam("ids") java.util.List<UUID> ids
    ) {
        return ResponseEntity.ok(feedService.getArticlesByIds(ids));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getArticleDetail(
            @PathVariable("id") UUID id,
            @RequestParam(name = "level", required = false, defaultValue = "EASY") DifficultyLevel level
    ) {
        return ResponseEntity.ok(articleDetailService.getArticleDetail(id, level));
    }
}
