package com.curiofeed.backend.infrastructure.llm.validation;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects when a generated title is too similar to the original article title
 * using bigram Jaccard similarity. Similarity >= THRESHOLD triggers a retry
 * in the SOURCE_DIGEST step so the LLM can produce a more independent title.
 */
@Component
public class TitleSimilarityValidator {

    static final double THRESHOLD = 0.30;

    private static final Pattern STRIP_PUNCT = Pattern.compile("[^a-z0-9\\s]");

    public boolean isTooSimilar(String generatedTitle, String originalTitle) {
        return bigramJaccard(generatedTitle, originalTitle) >= THRESHOLD;
    }

    double bigramJaccard(String a, String b) {
        Set<String> bigramsA = bigrams(normalize(a));
        Set<String> bigramsB = bigrams(normalize(b));
        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(bigramsA);
        intersection.retainAll(bigramsB);

        Set<String> union = new HashSet<>(bigramsA);
        union.addAll(bigramsB);

        return (double) intersection.size() / union.size();
    }

    private static String normalize(String title) {
        if (title == null) return "";
        return STRIP_PUNCT.matcher(title.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Set<String> bigrams(String normalized) {
        if (normalized.isBlank()) return Set.of();
        String[] tokens = normalized.split(" ");
        if (tokens.length < 2) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < tokens.length - 1; i++) {
            result.add(tokens[i] + " " + tokens[i + 1]);
        }
        return result;
    }
}
