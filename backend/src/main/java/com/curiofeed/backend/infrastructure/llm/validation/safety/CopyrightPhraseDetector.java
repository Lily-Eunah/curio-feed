package com.curiofeed.backend.infrastructure.llm.validation.safety;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CopyrightPhraseDetector {

    /**
     * Length of the word run that counts as copying.
     *
     * <p>Five was too short to mean anything: writing a factual article about the same events
     * produces five-word overlaps constantly ("is already in use in"), none of which is
     * protectable expression. Eight consecutive identical words is a real reproduction signal.
     */
    private static final int NGRAM_SIZE = 8;

    /** An n-gram made almost entirely of function words carries no expressive content. */
    private static final int MIN_CONTENT_WORDS = 2;

    /**
     * How many distinct offending phrases are tolerated before the content is rejected.
     *
     * <p>One or two long runs are usually unavoidable proper-noun sequences such as
     * "the national institute for health and care excellence". Three or more distinct
     * eight-word reproductions is copying.
     */
    private static final int MAX_TOLERATED_MATCHES = 2;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "about", "after", "all", "also", "an", "and", "any", "are", "as", "at",
            "be", "because", "been", "before", "being", "but", "by",
            "can", "could", "did", "do", "does", "for", "from",
            "had", "has", "have", "he", "her", "here", "him", "his", "how",
            "i", "if", "in", "into", "is", "it", "its",
            "just", "like", "may", "me", "might", "more", "most", "much", "must", "my",
            "no", "not", "now", "of", "on", "one", "only", "or", "other", "our", "out", "over",
            "said", "says", "she", "should", "so", "some", "such",
            "than", "that", "the", "their", "them", "then", "there", "these", "they", "this",
            "those", "to", "too", "up", "us", "very",
            "was", "we", "were", "what", "when", "where", "which", "while", "who", "will",
            "with", "would", "you", "your"
    );

    /**
     * Checks whether the generated content reproduces runs of words from the original article.
     *
     * @param generatedContent the article we produced
     * @param originalArticle  the source text it must not reproduce — never the digest
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
            List<String> window = genWords.subList(i, i + NGRAM_SIZE);
            if (countContentWords(window) < MIN_CONTENT_WORDS) {
                continue;
            }
            String phrase = String.join(" ", window);
            if (originalNGrams.contains(phrase)) {
                matchedPhrases.add(phrase);
            }
        }

        if (matchedPhrases.size() > MAX_TOLERATED_MATCHES) {
            violations.add("Copyright Violation: Verbatim copying detected (" + matchedPhrases.size() +
                    " copied " + NGRAM_SIZE + "-gram phrases found, e.g. '" + matchedPhrases.iterator().next() + "...')");
        }

        return violations;
    }

    private int countContentWords(List<String> words) {
        int count = 0;
        for (String w : words) {
            if (!STOPWORDS.contains(w)) {
                count++;
            }
        }
        return count;
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
