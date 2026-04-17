package com.curiofeed.backend.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record RegisterArticleRequest(
        String originalTitle,
        String sourceName,
        String sourceUrl,
        String originalContent,
        Instant originalPublishedAt,
        UUID categoryId,
        String thumbnailUrl
) {}
