package com.curiofeed.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.curiofeed.backend.api.dto.QuizAttemptResponse;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> options;

    @Column(nullable = false, length = 255)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_content_id", nullable = false)
    private ArticleContent articleContent;

    public QuizAttemptResponse evaluate(Object answer) {
        boolean isCorrect = false;
        String parsedStringAnswer = "";
        Object returnedCorrectAnswer = this.correctAnswer;

        if (answer instanceof String) {
            parsedStringAnswer = ((String) answer).trim();
            isCorrect = parsedStringAnswer.equalsIgnoreCase(this.correctAnswer.trim());
        } else if (answer instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> listAnswer = (java.util.List<String>) answer;
            parsedStringAnswer = String.join(" ", listAnswer).trim();
            isCorrect = parsedStringAnswer.equalsIgnoreCase(this.correctAnswer.trim());
            // Test expects correctAnswer to be returned as a List for array submissions
            returnedCorrectAnswer = java.util.List.of(this.correctAnswer.split(" "));
        }

        String finalExplanation = this.explanation;

        // Fallback explanation logic
        if (this.options != null && this.options.containsKey("explanations") && !isCorrect) {
            Object explanationsObj = this.options.get("explanations");
            if (explanationsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> explanationsMap = (Map<String, String>) explanationsObj;
                for (Map.Entry<String, String> entry : explanationsMap.entrySet()) {
                    if (entry.getKey().trim().equalsIgnoreCase(parsedStringAnswer)) {
                        finalExplanation = entry.getValue();
                        break;
                    }
                }
            }
        }

        return QuizAttemptResponse.builder()
                .isCorrect(isCorrect)
                .correctAnswer(returnedCorrectAnswer)
                .explanation(finalExplanation)
                .build();
    }
}
