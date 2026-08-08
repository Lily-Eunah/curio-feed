package com.curiofeed.backend.infrastructure.llm.validation.safety;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CopyrightPhraseDetector {

    private static final int NGRAM_SIZE = 5;

    /**
     * Checks if generated content copies verbatim n-gram phrases (5 or more consecutive words)
     * from the original article text.
     */
    public List<String> check(String generatedContent, String originalArticle) {
        List<String> violations = new ArrayList<>();
        if (generatedContent == null || originalArticle == null || generatedContent.isBlank() || originalArticle.isBlank()) {
            return violations;
        }

        Set<String> originalNGrams = extractNGrams(originalArticle, NGRAM_SIZE);
        List<String> genWords = tokenize(generatedContent);

        if (genWords.size() < NGRAM_SIZE) {
            return violations;
        }

        Set<String> matchedPhrases = new LinkedHashSet<>();
        for (int i = 0; i <= genWords.size() - NGRAM_SIZE; i++) {
            String phrase = String.join(" ", genWords.subList(i, i + NGRAM_SIZE));
            if (originalNGrams.contains(phrase)) {
                matchedPhrases.add(phrase);
            }
        }

        if (!matchedPhrases.isEmpty()) {
            violations.add("Copyright Violation: Verbatim copying detected (" + matchedPhrases.size() +
                    " copied " + NGRAM_SIZE + "-gram phrases found, e.g. '" + matchedPhrases.iterator().next() + "...')");
        }

        return violations;
    }

    private Set<String> extractNGrams(String text, int n) {
        List<String> words = tokenize(text);
        Set<String> nGrams = new HashSet<>();
        for (int i = 0; i <= words.size() - n; i++) {
            nGrams.add(String.join(" ", words.subList(i, i + n)));
        }
        return nGrams;
    }

    private List<String> tokenize(String text) {
        String clean = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = clean.trim().split("\\s+");
        List<String> list = new ArrayList<>();
        for (String t : tokens) {
            if (!t.isBlank()) {
                list.add(t);
            }
        }
        return list;
    }
}
