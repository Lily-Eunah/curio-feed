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
     * Above this share of capitalised words, a run is a name rather than borrowed phrasing.
     *
     * <p>"Harvard Medical School and Brigham and Women's Hospital" cannot be paraphrased and is
     * long enough that the sliding window flags it several times over, which on its own exceeded
     * the tolerance. Reporting the same institution three times is not three copied phrases.
     */
    private static final double PROPER_NOUN_RATIO = 0.5;

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

        Set<String> originalNGrams = extractComparableNGrams(originalArticle, NGRAM_SIZE);
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

    /**
     * N-grams of the source that would count as copying if they reappeared.
     *
     * <p>Windows that are mostly capitalised in the source are left out: they are names of people,
     * organisations and places, which have to be repeated verbatim to report the facts at all.
     */
    private Set<String> extractComparableNGrams(String text, int n) {
        List<String> raw = tokenizeKeepingCase(text);
        Set<String> nGrams = new HashSet<>();
        for (int i = 0; i <= raw.size() - n; i++) {
            List<String> window = raw.subList(i, i + n);
            if (capitalisedRatio(window) >= PROPER_NOUN_RATIO) {
                continue;
            }
            nGrams.add(String.join(" ", window).toLowerCase(Locale.ROOT));
        }
        return nGrams;
    }

    private double capitalisedRatio(List<String> words) {
        int capitalised = 0;
        for (String w : words) {
            if (!w.isEmpty() && Character.isUpperCase(w.charAt(0))) {
                capitalised++;
            }
        }
        return capitalised / (double) words.size();
    }

    /** Same splitting as {@link #tokenize}, but preserves case so names can be recognised. */
    private List<String> tokenizeKeepingCase(String text) {
        String clean = text.replaceAll("[^A-Za-z0-9\\s]", " ");
        List<String> list = new ArrayList<>();
        for (String t : clean.trim().split("\\s+")) {
            if (!t.isBlank()) {
                list.add(t);
            }
        }
        return list;
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
