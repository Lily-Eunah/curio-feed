package com.curiofeed.backend.domain.event;

import java.util.UUID;

public record ArticleIngestedEvent(UUID jobId, UUID articleId) {}
