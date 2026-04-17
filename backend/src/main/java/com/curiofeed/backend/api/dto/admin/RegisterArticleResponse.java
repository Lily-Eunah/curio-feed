package com.curiofeed.backend.api.dto.admin;

import java.util.UUID;

public record RegisterArticleResponse(UUID articleId, UUID jobId, String status) {}
