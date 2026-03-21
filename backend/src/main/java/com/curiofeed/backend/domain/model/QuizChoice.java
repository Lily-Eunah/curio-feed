package com.curiofeed.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizChoice {
    private String key;
    private String text;
    private String explanation;
}
