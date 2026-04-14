package com.curiofeed.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptions {
    private List<QuizChoice> choices;
    // Map used as fallback for simpler string-based explanations if choices aren't sufficient
    private Map<String, String> explanations;
}
