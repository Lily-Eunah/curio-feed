package com.curiofeed.backend.infrastructure.llm.validation.safety;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompositeSafetyFilter {

    private final List<SafetyFilter> safetyFilters;
    private final CopyrightPhraseDetector copyrightPhraseDetector;

    public CompositeSafetyFilter(List<SafetyFilter> safetyFilters, CopyrightPhraseDetector copyrightPhraseDetector) {
        this.safetyFilters = safetyFilters;
        this.copyrightPhraseDetector = copyrightPhraseDetector;
    }

    public List<String> validate(String generatedContent, String originalArticle) {
        List<String> violations = new ArrayList<>();

        if (generatedContent == null || generatedContent.isBlank()) {
            return violations;
        }

        // Run general safety filters (PII, Toxicity, etc.)
        for (SafetyFilter filter : safetyFilters) {
            violations.addAll(filter.check(generatedContent));
        }

        // Run copyright verbatim check if originalArticle is present
        if (originalArticle != null && !originalArticle.isBlank()) {
            violations.addAll(copyrightPhraseDetector.check(generatedContent, originalArticle));
        }

        return violations;
    }
}
