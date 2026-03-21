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
    private Object answer; // Can be String or List<String> depending on quiz type
}
