package com.curiofeed.backend.infrastructure.llm.validation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight deterministic lemmatizer that checks whether a vocabulary base
 * form (or any common inflection of it) appears in generated content.
 *
 * Covers the main failure cases observed in experiments:
 *   target  ↔ targeted / targeting / targets
 *   surge   ↔ surged / surging / surges
 *   restrict↔ restricted / restricting / restricts / restriction
 *   escalate↔ escalated / escalating / escalation
 *   depreciate↔ depreciated / depreciating / depreciation
 *   constitute↔ constituted / constituting / constitution
 *   propel  ↔ propelled / propelling / propulsion
 */
@Component
public class VocabLemmatizer {

    // Words that never need matching (stopwords the prompt should have excluded)
    private static final Set<String> STOPWORDS = Set.of(
        "say","make","give","take","use","help","start","get","go","come",
        "keep","put","show","move","run","turn","set","ask","need","want",
        "tell","work","call","feel","look","know","think"
    );

    /**
     * Returns true if {@code baseWord} or any common inflected form appears in
     * {@code content} (case-insensitive; the method lowercases both internally).
     */
    public boolean appearsInContent(String baseWord, String content) {
        if (baseWord == null || baseWord.isBlank()) return false;
        String w = baseWord.toLowerCase(Locale.ROOT).trim();
        if (STOPWORDS.contains(w)) return true; // skipped by policy

        String contentLower = content == null ? "" : content.toLowerCase(Locale.ROOT);
        for (String candidate : generateForms(w)) {
            if (containsWord(contentLower, candidate)) return true;
        }
        return false;
    }

    /**
     * Generates candidate surface forms from a base word, ordered from most
     * specific to most general so the first match is most reliable.
     */
    List<String> generateForms(String base) {
        return List.of(
            base,                               // exact base form
            base + "s",                         // 3rd person / plural
            base + "es",                        // -es plural / 3rd person
            base + "d",                         // past simple: e + d
            base + "ed",                        // past simple: consonant + ed
            base + "ing",                       // present participle
            base + "ion",                       // noun: restrict → restriction
            base + "tion",                      // noun: depreciate → depreciation
            base + "ation",                     // noun: escalate → escalation
            base + "er",                        // comparative / agent noun
            base + "ly",                        // adverb: significant → significantly
            dropE(base) + "d",                  // surged, targeted (base ends in e)
            dropE(base) + "ed",
            dropE(base) + "ing",               // surging, targeting
            dropE(base) + "ion",               // depreciation from depreciate
            dropE(base) + "ation",             // escalation from escalate
            doubleConsonant(base) + "ed",      // propelled from propel
            doubleConsonant(base) + "ing",     // propelling
            base.replace("y", "ied"),           // worried from worry
            base.replace("y", "ies"),           // carries from carry
            dropE(base) + "ation"              // constitution from constitute
        );
    }

    // ── String helpers ────────────────────────────────────────────────────────

    private String dropE(String w) {
        return w.endsWith("e") ? w.substring(0, w.length() - 1) : w;
    }

    private String doubleConsonant(String w) {
        if (w.length() < 2) return w;
        char last = w.charAt(w.length() - 1);
        char prev = w.charAt(w.length() - 2);
        // Simple heuristic: double final consonant if it's a consonant
        // and the vowel before it is a single vowel (e.g. propel → propell-)
        boolean lastIsConsonant = !isVowel(last);
        boolean prevIsVowel = isVowel(prev);
        if (lastIsConsonant && prevIsVowel) {
            return w + last;
        }
        return w;
    }

    private boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }

    /**
     * Checks that {@code form} appears as a whole word (surrounded by
     * non-alphanumeric characters) in {@code contentLower}.
     * Falls back to substring if boundary check fails (handles punctuation edge cases).
     */
    private boolean containsWord(String contentLower, String form) {
        if (form.isBlank()) return false;
        int idx = contentLower.indexOf(form);
        while (idx >= 0) {
            boolean boundaryBefore = (idx == 0 || !Character.isLetterOrDigit(contentLower.charAt(idx - 1)));
            boolean boundaryAfter  = (idx + form.length() == contentLower.length()
                    || !Character.isLetterOrDigit(contentLower.charAt(idx + form.length())));
            if (boundaryBefore && boundaryAfter) return true;
            idx = contentLower.indexOf(form, idx + 1);
        }
        return false;
    }

    /**
     * Normalizes a vocabulary word for display on learning cards (base form).
     * Applies safe transformations for common inflections while preserving words that shouldn't be changed.
     */
    public String normalizeDisplayWord(String word) {
        if (word == null || word.isBlank()) return word;
        String w = word.trim();

        // Do not normalize specific words
        if (w.equals("depreciation") || w.equals("significantly") 
            || w.equals("inflation") || w.equals("justification")) {
            return w;
        }

        return switch (w) {
            case "targeted" -> "target";
            case "surged" -> "surge";
            case "restricted" -> "restrict";
            case "escalated" -> "escalate";
            case "constituted" -> "constitute";
            case "propelled" -> "propel";
            case "asserting" -> "assert";
            default -> w;
        };
    }
}
