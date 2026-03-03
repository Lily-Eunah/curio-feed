package com.curiofeed.backend.api.controller;

import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.api.dto.CursorPageResponse;
import com.curiofeed.backend.domain.service.ArticleFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class ArticleFeedController {

    private final ArticleFeedService feedService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<ArticleFeedResponse>> getArticleFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(feedService.getFeed(cursor, size));
    }
}
