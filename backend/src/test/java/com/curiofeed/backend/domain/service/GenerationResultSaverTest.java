package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.domain.model.QuizOptions;
import com.curiofeed.backend.domain.repository.ArticleContentRepository;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import com.curiofeed.backend.domain.repository.QuizRepository;
import com.curiofeed.backend.domain.repository.VocabularyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class GenerationResultSaverTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GenerationResultSaver saver;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleContentRepository contentRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private EntityManager em;

    private Article article;

    @BeforeEach
    void setUp() {
        Category category = newInstance(Category.class);
        setField(category, "name", "tech-" + UUID.randomUUID());
        setField(category, "displayName", "Technology");
        setField(category, "active", true);
        setField(category, "sortOrder", 1);
        em.persist(category);

        article = newInstance(Article.class);
        setField(article, "originalTitle", "Test Article");
        setField(article, "sourceName", "Test Source");
        setField(article, "sourceUrl", "https://example.com/" + UUID.randomUUID());
        setField(article, "originalPublishedAt", Instant.now());
        setField(article, "title", "Test Article");
        setField(article, "slug", "test-article-" + UUID.randomUUID());
        setField(article, "category", category);
        setField(article, "publishedAt", Instant.now());
        setField(article, "status", ArticleStatus.DRAFT);
        setField(article, "thumbnailUrl", "https://example.com/thumb.jpg");
        setField(article, "originalContent", "Original content.");
        em.persist(article);
        em.flush();
    }

    // ── Factory helpers ──────────────────────────────────────────────────────

    private GenerationResult fullResult() {
        var vocab = new GenerationResult.VocabularyData("apple", "a fruit", "I ate an apple.");
        var quiz = new GenerationResult.QuizData(
                QuizType.MULTIPLE_CHOICE, "What is an apple?",
                new QuizOptions(null, null), "A", "Apple is a fruit.");
        return new GenerationResult("Rewritten content.", List.of(vocab), List.of(quiz));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("전체 성공: FULL_SUCCESS 반환, DB에 content+vocab+quiz 모두 존재")
    void save_fullResult_fullSuccess() {
        SaveStatus status = saver.save(article.getId(), DifficultyLevel.EASY, fullResult());

        assertThat(status).isEqualTo(SaveStatus.FULL_SUCCESS);
        em.flush(); em.clear();

        var content = contentRepository.findByArticleIdAndLevel(article.getId(), DifficultyLevel.EASY);
        assertThat(content).isPresent();
        assertThat(content.get().getContent()).isEqualTo("Rewritten content.");
        assertThat(vocabularyRepository.findAll()).isNotEmpty();
        assertThat(quizRepository.findAll()).isNotEmpty();
    }

    @Test
    @DisplayName("content 없음: NO_CONTENT 반환, DB에 아무것도 없음")
    void save_noContent_returnsNoContent() {
        var emptyResult = new GenerationResult(null, List.of(), List.of());
        SaveStatus status = saver.save(article.getId(), DifficultyLevel.EASY, emptyResult);

        assertThat(status).isEqualTo(SaveStatus.NO_CONTENT);
        assertThat(contentRepository.findByArticleIdAndLevel(article.getId(), DifficultyLevel.EASY)).isEmpty();
    }

    @Test
    @DisplayName("vocab 없음: CONTENT_ONLY 반환, content만 DB에 존재")
    void save_noVocab_returnsContentOnly() {
        var result = new GenerationResult("Some content.", null, List.of());
        SaveStatus status = saver.save(article.getId(), DifficultyLevel.MEDIUM, result);

        assertThat(status).isEqualTo(SaveStatus.CONTENT_ONLY);
        assertThat(contentRepository.findByArticleIdAndLevel(article.getId(), DifficultyLevel.MEDIUM)).isPresent();
        assertThat(vocabularyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("quiz 없음: CONTENT_WITH_VOCAB 반환, content+vocab만 DB에 존재")
    void save_noQuiz_returnsContentWithVocab() {
        var vocab = new GenerationResult.VocabularyData("word", "definition", "example");
        var result = new GenerationResult("Some content.", List.of(vocab), null);
        SaveStatus status = saver.save(article.getId(), DifficultyLevel.HARD, result);

        assertThat(status).isEqualTo(SaveStatus.CONTENT_WITH_VOCAB);
        assertThat(contentRepository.findByArticleIdAndLevel(article.getId(), DifficultyLevel.HARD)).isPresent();
        assertThat(vocabularyRepository.findAll()).isNotEmpty();
        assertThat(quizRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("동일 (article_id, level)에 재저장 시 덮어쓰기 성공")
    void save_existingContent_overwriteSuccess() {
        saver.save(article.getId(), DifficultyLevel.EASY, fullResult());
        em.flush(); em.clear();

        var updatedResult = new GenerationResult("Updated content.", List.of(), List.of());
        // NOTE: save with no vocab/quiz but we just verify overwrite
        var updatedWithVocab = new GenerationResult.VocabularyData("banana", "yellow fruit", "I like bananas.");
        var rewriteResult = new GenerationResult("Updated content.", List.of(updatedWithVocab), List.of());
        SaveStatus status = saver.save(article.getId(), DifficultyLevel.EASY, rewriteResult);

        assertThat(status).isEqualTo(SaveStatus.CONTENT_WITH_VOCAB);
        em.flush(); em.clear();

        var content = contentRepository.findByArticleIdAndLevel(article.getId(), DifficultyLevel.EASY).orElseThrow();
        assertThat(content.getContent()).isEqualTo("Updated content.");
    }

    // ── Reflection helpers ───────────────────────────────────────────────────

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
