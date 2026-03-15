package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.api.dto.ArticleDetailResponse;
import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.Category;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.repository.ArticleDetailRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ArticleDetailServiceTest {

    @Mock
    private ArticleDetailRepository articleDetailRepository;

    @InjectMocks
    private ArticleDetailServiceImpl articleDetailService;

    private Article mockArticle;
    private final UUID articleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Category category = newInstance(Category.class);
        setField(category, "name", "tech");
        setField(category, "displayName", "Technology");

        mockArticle = newInstance(Article.class);
        setField(mockArticle, "id", articleId);
        setField(mockArticle, "title", "Test Article");
        setField(mockArticle, "originalTitle", "Original Title");
        setField(mockArticle, "sourceName", "Test Source");
        setField(mockArticle, "sourceUrl", "http://test.com");
        setField(mockArticle, "thumbnailUrl", "http://test.com/thumb.jpg");
        setField(mockArticle, "publishedAt", Instant.now());
        setField(mockArticle, "status", ArticleStatus.PUBLISHED);
        setField(mockArticle, "category", category);

        // Add EASY content
        ArticleContent easyContent = newInstance(ArticleContent.class);
        setField(easyContent, "level", DifficultyLevel.EASY);
        setField(easyContent, "content", "Easy content text");
        setField(easyContent, "article", mockArticle);
        mockArticle.getContents().add(easyContent);

        // Add MEDIUM content
        ArticleContent mediumContent = newInstance(ArticleContent.class);
        setField(mediumContent, "level", DifficultyLevel.MEDIUM);
        setField(mediumContent, "content", "Medium content text");
        setField(mediumContent, "article", mockArticle);
        mockArticle.getContents().add(mediumContent);
    }

    @Test
    @DisplayName("유효한 ID와 Level로 요청하면 해당하는 본문이 포함된 DTO를 반환한다")
    void shouldReturnArticleDetail_whenValidIdAndLevel() {
        // given
        given(articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(articleId))
                .willReturn(Optional.of(mockArticle));

        // when
        ArticleDetailResponse response = articleDetailService.getArticleDetail(articleId, DifficultyLevel.MEDIUM);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(articleId);
        assertThat(response.title()).isEqualTo("Test Article");
        assertThat(response.availableLevels()).containsExactlyInAnyOrder(DifficultyLevel.EASY, DifficultyLevel.MEDIUM);
        assertThat(response.content().level()).isEqualTo(DifficultyLevel.MEDIUM);
        assertThat(response.content().content()).isEqualTo("Medium content text");
    }

    @Test
    @DisplayName("Level 파라미터가 null이면 자동으로 EASY 난이도를 선택하여 반환한다")
    void shouldFallbackToEasy_whenLevelIsNull() {
        // given
        given(articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(articleId))
                .willReturn(Optional.of(mockArticle));

        // when
        ArticleDetailResponse response = articleDetailService.getArticleDetail(articleId, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content().level()).isEqualTo(DifficultyLevel.EASY);
        assertThat(response.content().content()).isEqualTo("Easy content text");
    }

    @Test
    @DisplayName("요청한 기사가 DB에 없으면 EntityNotFoundException을 발생시킨다")
    void shouldThrowEntityNotFound_whenRepositoryReturnsEmpty() {
        // given
        given(articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(articleId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> articleDetailService.getArticleDetail(articleId, DifficultyLevel.EASY))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Article not found with id: " + articleId);
    }

    @Test
    @DisplayName("기사는 존재하지만 요청한 레벨의 본문(Content)이 없으면 예외를 발생시킨다")
    void shouldThrowNotFound_whenRequestedLevelContentIsMissing() {
        // given
        given(articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(articleId))
                .willReturn(Optional.of(mockArticle));

        // when & then (Requesting HARD which is not in mockArticle)
        assertThatThrownBy(() -> articleDetailService.getArticleDetail(articleId, DifficultyLevel.HARD))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Content not found for level: " + DifficultyLevel.HARD);
    }

    @Test
    @DisplayName("상세 조회 시 조회수(viewCount)가 1 증가한다")
    void shouldIncrementViewCount_whenArticleDetailIsRetrieved() {
        // given
        setField(mockArticle, "viewCount", 5L);
        given(articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(articleId))
                .willReturn(Optional.of(mockArticle));

        // when
        articleDetailService.getArticleDetail(articleId, DifficultyLevel.EASY);

        // then
        assertThat(mockArticle.getViewCount()).isEqualTo(6L);
    }

    // --- Reflection Helpers for creating Mock Entities with Protected Constructors ---
    @SuppressWarnings("unchecked")
    private <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("Field not found: " + fieldName);
    }
}
