package com.curiofeed.backend.domain.model;

import lombok.Builder;
import java.util.List;

@Builder
public record QuizSubmission(
        String choiceId,
        String answerText,
        List<String> answerList
) {
}
