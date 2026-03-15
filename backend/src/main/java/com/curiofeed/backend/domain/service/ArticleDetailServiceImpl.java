package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.repository.ArticleDetailRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleDetailServiceImpl implements ArticleDetailService {

    private final ArticleDetailRepository articleDetailRepository;

    @Override
    @Transactional
    public ArticleDetailResponse getArticleDetail(UUID id, DifficultyLevel level) {
        Article article = articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + id));

        article.incrementViewCount();

        DifficultyLevel targetLevel = (level != null) ? level : DifficultyLevel.EASY;

        ArticleContent targetContent = article.getContents().stream()
                .filter(c -> c.getLevel() == targetLevel)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Content not found for level: " + targetLevel));

        List<DifficultyLevel> availableLevels = article.getContents().stream()
                .map(ArticleContent::getLevel)
                .toList();

        return ArticleDetailResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .originalTitle(article.getOriginalTitle())
                .sourceName(article.getSourceName())
                .sourceUrl(article.getSourceUrl())
                .thumbnailUrl(article.getThumbnailUrl())
                .publishedAt(article.getPublishedAt())
                .categoryName(article.getCategory().getDisplayName())
                .availableLevels(availableLevels)
                .content(mapContent(targetContent))
                .build();
    }

    private ArticleDetailResponse.ArticleContentDto mapContent(ArticleContent content) {
        List<ArticleDetailResponse.VocabularyDto> vocabs = content.getVocabularies().stream()
                .map(v -> ArticleDetailResponse.VocabularyDto.builder()
                        .word(v.getWord())
                        .definition(v.getDefinition())
                        .exampleSentence(v.getExampleSentence())
                        .build())
                .toList();

        List<ArticleDetailResponse.QuizDto> quizzes = content.getQuizzes().stream()
                .map(q -> ArticleDetailResponse.QuizDto.builder()
                        .type(q.getType())
                        .question(q.getQuestion())
                        .options(q.getOptions())
                        .correctAnswer(q.getCorrectAnswer())
                        .explanation(q.getExplanation())
                        .build())
                .toList();

        return ArticleDetailResponse.ArticleContentDto.builder()
                .level(content.getLevel())
                .content(content.getContent())
                .audioUrl(content.getAudioUrl())
                .vocabularies(vocabs)
                .quizzes(quizzes)
                .build();
    }
}
