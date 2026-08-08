package com.curiofeed.backend.infrastructure.llm.validation.safety;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class KeywordToxicityFilter implements SafetyFilter {

    // Set of forbidden/toxic English words & expressions
    private static final Set<String> TOXIC_KEYWORDS = Set.of(
            "fuck", "shit", "bullshit", "bitch", "asshole", "bastard", "crap", "dick",
            "pussy", "cunt", "nigger", "faggot", "slut", "whore", "retard",
            "kill yourself", "suicide", "terrorist", "nazi"
    );

    @Override
    public List<String> check(String content) {
        List<String> violations = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return violations;
        }

        String lowerContent = content.toLowerCase(Locale.ROOT);
        // Normalize punctuation to spaces for word boundary checking
        String normalized = lowerContent.replaceAll("[^a-z0-9\\s]", " ");

        Set<String> wordsInContent = new HashSet<>(Arrays.asList(normalized.split("\\s+")));

        for (String toxic : TOXIC_KEYWORDS) {
            if (toxic.contains(" ")) {
                if (lowerContent.contains(toxic)) {
                    violations.add("Toxicity Violation: Toxic phrase detected '" + toxic + "'");
                }
            } else {
                if (wordsInContent.contains(toxic)) {
                    violations.add("Toxicity Violation: Toxic word detected '" + toxic + "'");
                }
            }
        }

        return violations;
    }
}
