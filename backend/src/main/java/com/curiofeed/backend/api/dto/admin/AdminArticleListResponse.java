package com.curiofeed.backend.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminArticleListResponse(
        UUID id,
        String originalTitle,
        String sourceName,
        String status,
        String categoryName,
        Instant createdAt,
        /**
         * Difficulty levels that actually hold generated text, out of three.
         *
         * <p>Status alone cannot show this: an article whose generation failed outright and one
         * that is ready to publish are both DRAFT. Publishing a partial article makes the feed
         * fall back to the source text for the missing level, so this needs to be visible.
         */
        long populatedLevels
) {}
