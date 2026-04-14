package com.curiofeed.backend.api.dto;

import lombok.Builder;

@Builder
public record QuizAttemptResponse(
    boolean isCorrect,
    Object correctAnswer, // Can be String or List<String>
    String explanation
) {}
