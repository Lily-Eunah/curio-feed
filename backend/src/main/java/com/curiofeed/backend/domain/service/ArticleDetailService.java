package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.domain.entity.DifficultyLevel;

import java.util.UUID;

public interface ArticleDetailService {
    ArticleDetailResponse getArticleDetail(UUID id, DifficultyLevel level);
}
