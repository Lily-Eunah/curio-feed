package com.curiofeed.backend.domain.model;

import lombok.Builder;

@Builder
public record QuizEvaluationResult(
        boolean isCorrect,
        Object correctAnswer,
        String explanation
) {
}
