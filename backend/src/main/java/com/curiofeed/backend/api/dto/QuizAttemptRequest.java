package com.curiofeed.backend.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptRequest {
    private String choiceId;
    private String answerText;
    private java.util.List<String> answerList;
}
