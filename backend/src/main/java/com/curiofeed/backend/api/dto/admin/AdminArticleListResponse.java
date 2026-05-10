package com.curiofeed.backend.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminArticleListResponse(
        UUID id,
        String originalTitle,
        String sourceName,
        String status,
        String categoryName,
        Instant createdAt
) {}
