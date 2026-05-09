package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleFeedResponse;
import com.curiofeed.backend.api.dto.CursorPageResponse;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.repository.ArticleFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleFeedService {

    /** WPM used for feed reading time (MEDIUM level as default). */
    private static final int FEED_WPM = 120;
    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\s+");

    private final ArticleFeedRepository feedRepository;

    public CursorPageResponse<ArticleFeedResponse> getFeed(String cursorToken, int size, DifficultyLevel level) {
        // Query one extra to check if there is a next page
        int limit = size + 1;
        List<ArticleFeedResponse> results;
        
        if (cursorToken == null || cursorToken.isBlank()) {
            results = feedRepository.findFeedFirstPage(ArticleStatus.PUBLISHED, level, PageRequest.of(0, limit));
        } else {
            String[] parts = cursorToken.split("_");
            Instant cursorAt = Instant.parse(parts[0]);
            UUID cursorId = UUID.fromString(parts[1]);
            results = feedRepository.findFeedByCursor(ArticleStatus.PUBLISHED, cursorAt, cursorId, level, PageRequest.of(0, limit));
        }

        boolean hasNext = results.size() == limit;
        if (hasNext) {
            results.remove(results.size() - 1); // remove the extra item
        }

        // Patch reading time: the SQL hardcodes 0; calculate from excerpt word count.
        // The excerpt field contains originalContent or generated content at this point.
        results = results.stream().map(r -> withReadingTime(r, level)).toList();

        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            ArticleFeedResponse last = results.get(results.size() - 1);
            nextCursor = last.getPublishedAt().toString() + "_" + last.getId();
        }

        return new CursorPageResponse<>(results, nextCursor, hasNext);
    }

    public List<ArticleFeedResponse> getArticlesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return feedRepository.findAllById(ids).stream()
                .filter(a -> a.getStatus() == ArticleStatus.PUBLISHED)
                .map(a -> new ArticleFeedResponse(
                        a.getId().toString(),
                        a.getTitle(),
                        a.getOriginalContent(),
                        a.getCategory().getDisplayName(),
                        a.getSourceName(),
                        a.getPublishedAt(),
                        readingMinutes(a.getOriginalContent(), DifficultyLevel.MEDIUM) // Fallback for bulk get (not level specific)
                ))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ArticleFeedResponse withReadingTime(ArticleFeedResponse r, DifficultyLevel level) {
        if (r.getEstimatedReadingTime() != 0) return r; // already set
        return ArticleFeedResponse.builder()
                .id(r.getId())
                .title(r.getTitle())
                .excerpt(r.getExcerpt())
                .categoryName(r.getCategoryName())
                .sourceName(r.getSourceName())
                .publishedAt(r.getPublishedAt())
                .estimatedReadingTime(readingMinutes(r.getExcerpt(), level))
                .build();
    }

    private int readingMinutes(String text, DifficultyLevel level) {
        if (text == null || text.isBlank()) return 1;
        int wordCount = WORD_BOUNDARY.split(text.trim()).length;
        int wpm = switch (level) {
            case EASY -> 90;
            case MEDIUM -> 120;
            case HARD -> 140;
        };
        return Math.max(1, (int) Math.ceil((double) wordCount / wpm));
    }
}
