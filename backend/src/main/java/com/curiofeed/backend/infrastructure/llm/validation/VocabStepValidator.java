package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.model.GenerationResult.VocabularyData;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates Step 2 (vocabulary-only) output.
 * Uses VocabLemmatizer for inflection-aware content matching.
 */
@Component
public class VocabStepValidator {

    private static final int REQUIRED_COUNT = 5;

    private static final Set<String> FORBIDDEN_WORDS = Set.of(
        "say","make","give","take","use","help","start","get","go","come",
        "keep","put","show","move","run","turn","set","ask","need","want",
        "tell","work","call","feel","look","know","think",
        "block","connect","continue","approach","container","project",
        "big","small","many","few","main","major","important","good","bad",
        "thing","place","level","number","area","violation","agreement",
        "warning","attack","change","open","close","allow","stop","hold",
        "send","bring"
    );

    private final VocabLemmatizer lemmatizer;

    public VocabStepValidator(VocabLemmatizer lemmatizer) {
        this.lemmatizer = lemmatizer;
    }

    /**
     * Validates the vocabulary list against the generated content.
     * Returns list of errors; empty = pass.
     * Errors prefixed "[SOFT]" are warnings only.
     */
    public List<String> validate(List<VocabularyData> vocabs, String content) {
        List<String> errors = new ArrayList<>();

        if (vocabs == null) {
            errors.add("vocabularies is null");
            return errors;
        }
        if (vocabs.size() != REQUIRED_COUNT) {
            errors.add("expected " + REQUIRED_COUNT + " vocabularies, got " + vocabs.size());
        }

        Set<String> seenWords = new HashSet<>();

        for (int i = 0; i < vocabs.size(); i++) {
            VocabularyData v = vocabs.get(i);

            if (isBlank(v.word())) {
                errors.add("vocab[" + i + "].word is blank");
                continue;
            }
            if (isBlank(v.definition())) {
                errors.add("vocab[" + i + "].definition is blank");
            }
            if (isBlank(v.exampleSentence())) {
                errors.add("vocab[" + i + "].exampleSentence is blank");
            }

            String word = v.word().toLowerCase(Locale.ROOT).trim();

            // Forbidden word check
            if (FORBIDDEN_WORDS.contains(word)) {
                errors.add("vocab[" + i + "] forbidden word: " + v.word());
            }

            // Duplicate check
            if (!seenWords.add(word)) {
                errors.add("[SOFT] vocab[" + i + "] duplicate word: " + v.word());
            }

            // Content alignment check (via lemmatizer — handles case-folding internally)
            if (!lemmatizer.appearsInContent(v.word(), content)) {
                errors.add("vocab[" + i + "] word not found in content (base or inflected form): " + v.word());
            }

            // Definition quality: must contain "used when"
            if (v.definition() != null && !v.definition().toLowerCase(Locale.ROOT).contains("used when")) {
                errors.add("[SOFT] vocab[" + i + "] definition missing 'used when' clause: " + v.definition());
            }
        }

        return errors;
    }

    public boolean isHardFail(List<String> errors) {
        return errors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
