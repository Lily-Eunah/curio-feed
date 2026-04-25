package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.model.GenerationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContentValidator {

    private static final int MIN_CONTENT_LENGTH = 200;
    private static final int MIN_SENTENCE_COUNT = 5;

    public List<String> check(GenerationResult result) {
        List<String> warnings = new ArrayList<>();
        String content = result.content();

        if (content.length() < MIN_CONTENT_LENGTH) {
            warnings.add("[SOFT] content too short: " + content.length() + " chars (min " + MIN_CONTENT_LENGTH + ")");
        }

        int sentenceCount = countSentences(content);
        if (sentenceCount < MIN_SENTENCE_COUNT) {
            warnings.add("[SOFT] too few sentences: " + sentenceCount + " (min " + MIN_SENTENCE_COUNT + ")");
        }

        return warnings;
    }

    int countSentences(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '.' || c == '!' || c == '?') count++;
        }
        return Math.max(count, 1);
    }
}
