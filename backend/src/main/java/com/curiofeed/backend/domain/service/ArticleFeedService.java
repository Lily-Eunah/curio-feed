package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.api.dto.CursorPageResponse;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.repository.ArticleFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleFeedService {

    private final ArticleFeedRepository feedRepository;

    public CursorPageResponse<ArticleFeedResponse> getFeed(String cursorToken, int size) {
        // Query one extra to check if there is a next page
        int limit = size + 1;
        List<ArticleFeedResponse> results;
        
        if (cursorToken == null || cursorToken.isBlank()) {
            results = feedRepository.findFeedFirstPage(ArticleStatus.PUBLISHED, PageRequest.of(0, limit));
        } else {
            String[] parts = cursorToken.split("_");
            Instant cursorAt = Instant.parse(parts[0]);
            UUID cursorId = UUID.fromString(parts[1]);
            results = feedRepository.findFeedByCursor(ArticleStatus.PUBLISHED, cursorAt, cursorId, PageRequest.of(0, limit));
        }

        boolean hasNext = results.size() == limit;
        if (hasNext) {
            results.remove(results.size() - 1); // remove the extra item
        }

        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            ArticleFeedResponse last = results.get(results.size() - 1);
            nextCursor = last.getPublishedAt().toString() + "_" + last.getId();
        }

        return new CursorPageResponse<>(results, nextCursor, hasNext);
    }
}
