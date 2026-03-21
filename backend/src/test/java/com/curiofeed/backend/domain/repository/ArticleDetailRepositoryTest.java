package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.Category;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.Quiz;
import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.entity.Vocabulary;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(com.curiofeed.backend.config.JpaConfig.class)
class ArticleDetailRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ArticleDetailRepository articleDetailRepository;

    @Autowired
    private EntityManager em;

    private UUID publishedArticleId;
    private UUID draftArticleId;

    @BeforeEach
    void setUp() {
        // Category
        Category category = newInstance(Category.class);
        setField(category, "name", "tech");
        setField(category, "displayName", "Technology");
        setField(category, "active", true);
        setField(category, "sortOrder", 1);
        em.persist(category);

        // PUBLISHED Article
        Article published = newInstance(Article.class);
        setField(published, "originalTitle", "Original Title");
        setField(published, "sourceName", "Test Source");
        setField(published, "sourceUrl", "https://example.com/article");
        setField(published, "originalPublishedAt", Instant.now());
        setField(published, "title", "Published Article");
        setField(published, "slug", "published-article");
        setField(published, "category", category);
        setField(published, "publishedAt", Instant.now());
        setField(published, "status", ArticleStatus.PUBLISHED);
        setField(published, "thumbnailUrl", "https://example.com/thumb.jpg");
        setField(published, "viewCount", 0L);
        em.persist(published);
        publishedArticleId = published.getId();

        // EASY Content with Vocabulary and Quiz
        ArticleContent easyContent = newInstance(ArticleContent.class);
        setField(easyContent, "level", DifficultyLevel.EASY);
        setField(easyContent, "content", "Easy content text");
        setField(easyContent, "audioUrl", "https://example.com/audio-easy.mp3");
        setField(easyContent, "article", published);
        em.persist(easyContent);

        Vocabulary vocab = newInstance(Vocabulary.class);
        setField(vocab, "word", "technology");
        setField(vocab, "definition", "The application of scientific knowledge");
        setField(vocab, "exampleSentence", "Technology is evolving rapidly.");
        setField(vocab, "articleContent", easyContent);
        em.persist(vocab);

        Quiz quiz = newInstance(Quiz.class);
        setField(quiz, "question", "What is the main topic?");
        setField(quiz, "type", QuizType.MULTIPLE_CHOICE);
        com.curiofeed.backend.domain.model.QuizOptions quizOptions = new com.curiofeed.backend.domain.model.QuizOptions(
            java.util.List.of(
                new com.curiofeed.backend.domain.model.QuizChoice("A", "Technology", null),
                new com.curiofeed.backend.domain.model.QuizChoice("B", "Science", null),
                new com.curiofeed.backend.domain.model.QuizChoice("C", "History", null)
            ),
            null
        );
        setField(quiz, "options", quizOptions);
        setField(quiz, "correctAnswer", "A");
        setField(quiz, "explanation", "The article discusses technology.");
        setField(quiz, "articleContent", easyContent);
        em.persist(quiz);

        // MEDIUM Content (no vocab/quiz)
        ArticleContent mediumContent = newInstance(ArticleContent.class);
        setField(mediumContent, "level", DifficultyLevel.MEDIUM);
        setField(mediumContent, "content", "Medium content text");
        setField(mediumContent, "article", published);
        em.persist(mediumContent);

        // DRAFT Article
        Article draft = newInstance(Article.class);
        setField(draft, "originalTitle", "Draft Original");
        setField(draft, "sourceName", "Test Source");
        setField(draft, "sourceUrl", "https://example.com/draft");
        setField(draft, "originalPublishedAt", Instant.now());
        setField(draft, "title", "Draft Article");
        setField(draft, "slug", "draft-article");
        setField(draft, "category", category);
        setField(draft, "publishedAt", Instant.now());
        setField(draft, "status", ArticleStatus.DRAFT);
        setField(draft, "thumbnailUrl", "https://example.com/draft-thumb.jpg");
        setField(draft, "viewCount", 0L);
        em.persist(draft);
        draftArticleId = draft.getId();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("PUBLISHED 기사를 ID로 조회하면 Contents, Vocabularies, Quizzes를 함께 반환한다")
    void shouldFindByIdWithContentsVocabsAndQuizzes() {
        // when
        Optional<Article> result = articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(publishedArticleId);

        // then
        assertThat(result).isPresent();
        Article article = result.get();
        assertThat(article.getTitle()).isEqualTo("Published Article");
        assertThat(article.getCategory().getDisplayName()).isEqualTo("Technology");
        assertThat(article.getContents()).hasSize(2);

        // Verify EASY content has vocabulary and quiz loaded
        ArticleContent easyContent = article.getContents().stream()
                .filter(c -> c.getLevel() == DifficultyLevel.EASY)
                .findFirst()
                .orElseThrow();

        assertThat(easyContent.getVocabularies()).hasSize(1);
        assertThat(easyContent.getVocabularies().get(0).getWord()).isEqualTo("technology");
        assertThat(easyContent.getQuizzes()).hasSize(1);
        assertThat(easyContent.getQuizzes().get(0).getQuestion()).isEqualTo("What is the main topic?");
    }

    @Test
    @DisplayName("PUBLISHED가 아닌 기사는 조회되지 않는다")
    void shouldReturnEmpty_whenStatusIsNotPublished() {
        // when
        Optional<Article> result = articleDetailRepository.findPublishedByIdWithContentsVocabsAndQuizzes(draftArticleId);

        // then
        assertThat(result).isEmpty();
    }

    // --- Reflection Helpers ---
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
