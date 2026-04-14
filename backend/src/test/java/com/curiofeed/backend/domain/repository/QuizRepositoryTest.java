package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Article;
import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.ArticleStatus;
import com.curiofeed.backend.domain.entity.Category;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.Quiz;
import com.curiofeed.backend.domain.entity.QuizType;
import com.curiofeed.backend.domain.model.QuizChoice;
import com.curiofeed.backend.domain.model.QuizOptions;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(com.curiofeed.backend.config.JpaConfig.class)
class QuizRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private EntityManager em;

    private UUID quizId;

    @BeforeEach
    void setUp() {
        // Category
        Category category = newInstance(Category.class);
        setField(category, "name", "tech");
        setField(category, "displayName", "Technology");
        setField(category, "active", true);
        setField(category, "sortOrder", 1);
        em.persist(category);

        // Article
        Article article = newInstance(Article.class);
        setField(article, "originalTitle", "Original");
        setField(article, "sourceName", "Source");
        setField(article, "sourceUrl", "http://example.com");
        setField(article, "originalPublishedAt", Instant.now());
        setField(article, "title", "Title");
        setField(article, "slug", "slug-test-quiz");
        setField(article, "category", category);
        setField(article, "publishedAt", Instant.now());
        setField(article, "status", ArticleStatus.PUBLISHED);
        setField(article, "thumbnailUrl", "http://example.com/thumb");
        setField(article, "viewCount", 0L);
        em.persist(article);

        // Article Content
        ArticleContent content = newInstance(ArticleContent.class);
        setField(content, "level", DifficultyLevel.EASY);
        setField(content, "content", "Content text");
        setField(content, "article", article);
        em.persist(content);

        // Quiz
        Quiz quiz = newInstance(Quiz.class);
        setField(quiz, "question", "What is JSONB?");
        setField(quiz, "type", QuizType.MULTIPLE_CHOICE);
        
        QuizOptions options = new QuizOptions(
                List.of(
                        new QuizChoice("A", "Format", "JSON representation in DB"),
                        new QuizChoice("B", "Network", null)
                ),
                Map.of("C", "Just arbitrary string")
        );
        
        setField(quiz, "options", options);
        setField(quiz, "correctAnswer", "A");
        setField(quiz, "explanation", "Default explanation.");
        setField(quiz, "articleContent", content);
        
        em.persist(quiz);
        quizId = quiz.getId();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("JSONB 컬럼을 포함한 퀴즈를 성공적으로 조회하고 객체로 매핑한다")
    void shouldRetrieveQuizWithJsonbOptions() {
        // when
        Optional<Quiz> result = quizRepository.findById(quizId);

        // then
        assertThat(result).isPresent();
        Quiz foundQuiz = result.get();
        assertThat(foundQuiz.getQuestion()).isEqualTo("What is JSONB?");
        assertThat(foundQuiz.getType()).isEqualTo(QuizType.MULTIPLE_CHOICE);
        assertThat(foundQuiz.getCorrectAnswer()).isEqualTo("A");

        // Verify JSONB mapping
        QuizOptions options = foundQuiz.getOptions();
        assertThat(options).isNotNull();
        assertThat(options.getChoices()).hasSize(2);
        
        QuizChoice choiceA = options.getChoices().get(0);
        assertThat(choiceA.getKey()).isEqualTo("A");
        assertThat(choiceA.getText()).isEqualTo("Format");
        assertThat(choiceA.getExplanation()).isEqualTo("JSON representation in DB");

        assertThat(options.getExplanations()).containsEntry("C", "Just arbitrary string");
    }

    @Test
    @DisplayName("특정 기사 본문(ArticleContent)에 연관된 퀴즈 목록을 생성일 순으로 조회한다")
    void shouldFindQuizzesByArticleContentId() {
        // given (A second quiz for the same content)
        Quiz quiz2 = newInstance(Quiz.class);
        setField(quiz2, "question", "Second Quiz");
        setField(quiz2, "type", QuizType.SHORT_ANSWER);
        setField(quiz2, "correctAnswer", "Ans");
        QuizOptions emptyOptions = new QuizOptions(List.of(), Map.of());
        setField(quiz2, "options", emptyOptions);

        Optional<ArticleContent> contentOpt = quizRepository.findById(quizId).map(Quiz::getArticleContent);
        assertThat(contentOpt).isPresent();
        setField(quiz2, "articleContent", contentOpt.get());
        em.persist(quiz2);
        em.flush();
        em.clear();

        // when
        List<Quiz> quizzes = quizRepository.findByArticleContentIdOrderByCreatedAtAsc(contentOpt.get().getId());

        // then
        assertThat(quizzes).hasSize(2);
        assertThat(quizzes.get(0).getQuestion()).isEqualTo("What is JSONB?");
        assertThat(quizzes.get(1).getQuestion()).isEqualTo("Second Quiz");
    }

    @Test
    @DisplayName("nullable 필드(해설, options 등)가 null인 퀴즈도 정상적으로 저장 및 조회된다")
    void shouldSaveAndRetrieveQuizWithNullableFields() {
        // given
        Optional<ArticleContent> contentOpt = quizRepository.findById(quizId).map(Quiz::getArticleContent);
        assertThat(contentOpt).isPresent();

        Quiz quizNullable = newInstance(Quiz.class);
        setField(quizNullable, "question", "Short answer without explanation");
        setField(quizNullable, "type", QuizType.SHORT_ANSWER);
        setField(quizNullable, "correctAnswer", "DB");
        QuizOptions emptyOptions = new QuizOptions(List.of(), Map.of());
        setField(quizNullable, "options", emptyOptions); // MUST NOT BE NULL IN DB
        setField(quizNullable, "explanation", null); // DB TEXT nullable
        setField(quizNullable, "articleContent", contentOpt.get());
        
        em.persist(quizNullable);
        UUID nullQuizId = quizNullable.getId();
        em.flush();
        em.clear();

        // when
        Optional<Quiz> result = quizRepository.findById(nullQuizId);

        // then
        assertThat(result).isPresent();
        Quiz foundQuiz = result.get();
        assertThat(foundQuiz.getOptions()).isNotNull();
        assertThat(foundQuiz.getExplanation()).isNull();
        assertThat(foundQuiz.getCorrectAnswer()).isEqualTo("DB");
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
