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
import com.curiofeed.backend.domain.model.QuizChoice;
import com.curiofeed.backend.domain.model.QuizEvaluationResult;
import com.curiofeed.backend.domain.model.QuizOptions;
import com.curiofeed.backend.domain.model.QuizSubmission;

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
    @Column(columnDefinition = "jsonb")
    private QuizOptions options;

    @Column(nullable = false, length = 255)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_content_id", nullable = false)
    private ArticleContent articleContent;

    public QuizEvaluationResult evaluate(QuizSubmission submission) {
        boolean isCorrect = false;
        String finalExplanation = this.explanation;
        Object returnedCorrectAnswer = this.correctAnswer;
        String submittedAnswerStr = "";

        if (this.type == QuizType.MULTIPLE_CHOICE) {
            String choiceId = normalize(submission.choiceId());
            String correctId = normalize(this.correctAnswer);
            isCorrect = choiceId.equals(correctId);
            
            if (!isCorrect && this.options != null) {
                if (this.options.getChoices() != null) {
                    for (QuizChoice choice : this.options.getChoices()) {
                        if (normalize(choice.getKey()).equals(choiceId) && choice.getExplanation() != null) {
                            finalExplanation = choice.getExplanation();
                            break;
                        }
                    }
                }
                if (this.options.getExplanations() != null && this.options.getExplanations().containsKey(submission.choiceId())) {
                    finalExplanation = this.options.getExplanations().get(submission.choiceId());
                }
            }
        } else if (this.type == QuizType.SHORT_ANSWER) {
            submittedAnswerStr = normalize(submission.answerText());
            isCorrect = submittedAnswerStr.equals(normalize(this.correctAnswer));
        } else if (this.type == QuizType.SCRAMBLE) {
             if (submission.answerList() != null && !submission.answerList().isEmpty()) {
                 submittedAnswerStr = normalize(String.join(" ", submission.answerList()));
                 isCorrect = submittedAnswerStr.equals(normalize(this.correctAnswer));
                 returnedCorrectAnswer = java.util.List.of(this.correctAnswer.split(" "));
             } else {
                 submittedAnswerStr = normalize(submission.answerText());
                 isCorrect = submittedAnswerStr.equals(normalize(this.correctAnswer));
             }
        }

        return QuizEvaluationResult.builder()
                .isCorrect(isCorrect)
                .correctAnswer(returnedCorrectAnswer)
                .explanation(finalExplanation)
                .build();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase()
                .replaceAll("[.,!?]+$", "") // Strip trailing punctuation
                .trim() // Trim again in case punctuation leaving space
                .replaceAll("\\s+", " ");
    }
}