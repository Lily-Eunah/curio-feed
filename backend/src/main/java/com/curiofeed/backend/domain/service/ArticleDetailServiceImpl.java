package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.repository.ArticleDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleDetailServiceImpl implements ArticleDetailService {

    private final ArticleDetailRepository articleDetailRepository;

    @Override
    public ArticleDetailResponse getArticleDetail(UUID id, DifficultyLevel level) {
        // TODO: Implemented failing logic intentionally (TDD Red)
        return null; // Will cause NullPointerException in assertions or return 404 in integrations
    }
}
