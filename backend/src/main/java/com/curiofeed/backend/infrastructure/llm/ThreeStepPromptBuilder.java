package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds prompts for the 3-step generation pipeline.
 * Each step produces a focused schema rather than the full combined schema.
 *
 * Step 1: content only  →  {"content": "..."}
 * Step 2: vocab only    →  {"vocabularies": [...]}
 * Step 3: quiz only     →  {"quizzes": [...]}
 */
@Component
public class ThreeStepPromptBuilder {

    // ── Step 1: Content ───────────────────────────────────────────────────────

    public String buildContentPrompt(String originalArticle, DifficultyLevel level) {
        String spec = switch (level) {
            case EASY -> """
                    EASY (A2-B1 level):
                    • Keep sentences short — aim for 12-15 words per sentence.
                    • Use only common vocabulary a 10-year-old native speaker would know.
                    • Replace difficult words with simpler ones — NEVER remove facts.
                    • Cover ALL key events, causes, and consequences from the original.
                    • Write AT LEAST 180 words. Write AT MOST 260 words.
                    • If your draft is shorter than 180 words, add more facts from the original.""";
            case MEDIUM -> """
                    MEDIUM (B1-B2 level):
                    • Natural news-writing style with moderate sentence variety.
                    • Topic-specific vocabulary is acceptable.
                    • Retain all cause-effect relationships, actor motivations, and key figures.
                    • Write AT LEAST 220 words. Write AT MOST 320 words.
                    • If your draft is shorter than 220 words, expand each key event with more detail.""";
            case HARD -> """
                    HARD (C1 level):
                    • Advanced vocabulary and complex, varied sentence structures.
                    • Formal, precise register. Dense information delivery.
                    • Preserve nuance, expert viewpoints, and causal chains.
                    • Write AT LEAST 280 words. Write AT MOST 420 words.
                    • If your draft is shorter than 280 words, include additional context and background.""";
        };

        return """
                You are an expert English content rewriter for language learners.

                Rewrite the news article below at the reading level specified.

                LEVEL AND WORD COUNT REQUIREMENTS:
                %s

                RULES:
                1. REWRITE, not summarise. Every key fact, event, and causal link must appear.
                2. Do NOT add information not in the original.
                3. Natural flowing prose — no bullet points, headers, or lists.
                4. Check your word count. If below the minimum, add more detail from the original.

                Return ONLY this JSON — no other text, no markdown:
                {"content": "your rewritten article here"}

                [ORIGINAL ARTICLE]
                %s
                """.formatted(spec, originalArticle);
    }

    // ── Step 2: Vocabulary ────────────────────────────────────────────────────

    public String buildVocabularyPrompt(String generatedContent, DifficultyLevel level) {
        String spec = switch (level) {
            case EASY -> "EASY (A2-B1): Words a learner would encounter in B1 reading but may not fully understand. Not trivial, not too complex.";
            case MEDIUM -> "MEDIUM (B1-B2): Academic or journalism-register vocabulary that appears naturally in news writing.";
            case HARD -> "HARD (C1): Advanced, domain-specific vocabulary that signals sophisticated writing.";
        };

        return """
                You are an English vocabulary educator.

                Extract exactly 5 vocabulary words from the %s-level news article below.

                DIFFICULTY TARGET: %s

                ════ BASE FORM RULE — CRITICAL ════════════════════════════════════════
                Always list the dictionary base form, never an inflected form.

                  content: "targeted"    → vocab word: "target"     ✓
                  content: "targeted"    → vocab word: "targeted"   ✗ WRONG
                  content: "surged"      → vocab word: "surge"      ✓
                  content: "restricting" → vocab word: "restrict"   ✓
                  content: "announced"   → vocab word: "announce"   ✓
                  content: "depreciation"→ vocab word: "depreciate" ✓
                ════════════════════════════════════════════════════════════════════════

                EXTRACTION STEPS (follow in this exact order):
                  A. Read every word in the content.
                  B. Identify 5 candidates that are educationally valuable and level-appropriate.
                  C. For each candidate, verify: does the BASE FORM or any inflected form appear in the content?
                     If NO match found → reject this candidate, pick another.
                  D. Write the BASE FORM as the vocabulary entry.
                  E. Cross-check: is any of your 5 words on the FORBIDDEN LIST below?
                     If YES → remove it immediately, pick a different word.

                ════ FORBIDDEN LIST — NEVER include these words ════════════════════
                  say, make, give, take, use, help, start, get, go, come, keep, put,
                  show, move, run, turn, set, ask, need, want, tell, work, call, feel,
                  look, know, think, block, connect, continue, approach, container,
                  project, big, small, many, few, main, major, important, good, bad,
                  thing, place, level, number, area, violation, agreement, warning,
                  attack, change, open, close, allow, stop, hold, send, bring
                ════════════════════════════════════════════════════════════════════════

                DEFINITION FORMAT — every definition MUST follow this exact pattern:
                  "[brief meaning] — used when [specific situation or condition]"

                  ✓ "to prevent access to an area by surrounding it — used when a military or political power wants to cut off a region from outside contact"
                  ✗ "relating to the sea" — REJECTED: missing "used when" clause

                EXAMPLE SENTENCE — strict rules:
                  • Use a COMPLETELY DIFFERENT TOPIC: cooking, sports, school, shopping, travel, relationships, workplace.
                  • Must NOT reference the article's topic, country, or any person named in the article.

                ════ SELF-CHECK before outputting ════════════════════════════════════
                For each of your 5 words:
                  1. Is it the BASE FORM (not inflected)?  If NO → fix it.
                  2. Does it or an inflected form appear in the content?  If NO → replace it.
                  3. Is it on the FORBIDDEN LIST?  If YES → replace it.
                  4. Does the definition end with a "used when" clause?  If NO → rewrite it.
                ════════════════════════════════════════════════════════════════════════

                Return ONLY this JSON — no other text:
                {"vocabularies": [{"word": "...", "definition": "...", "exampleSentence": "..."}, ...]}

                [CONTENT]
                %s
                """.formatted(level.name(), spec, generatedContent);
    }

    // ── Step 3: Quiz ──────────────────────────────────────────────────────────

    public String buildQuizPrompt(String generatedContent, String vocabJson, DifficultyLevel level) {
        return """
                You are an expert quiz designer for English language learners (%s level).

                INPUT:
                  (A) A news article — [CONTENT] below
                  (B) 5 vocabulary words from that article — [VOCABULARY] below

                Design EXACTLY 3 quizzes.

                ════ QUIZ 1 — MULTIPLE_CHOICE — Comprehension / Reasoning ═════════
                Test a cause-effect relationship or inference from the article.
                The correct answer must require understanding the passage, not just finding one sentence.

                BANNED question starters:
                  ✗ "What percentage..." ✗ "How many..." ✗ "Which country..."
                  ✗ "When did..." ✗ Any question about a specific number or date

                GOOD question starters:
                  ✓ "Why did X lead to Y?" ✓ "What does the author imply about...?"
                  ✓ "What would most likely happen if...?" ✓ "What is the underlying reason for...?"

                4 choices (A/B/C/D). ONE correct. Wrong choices are plausible misconceptions.
                Each choice: non-empty explanation. "correctAnswer": exactly "A", "B", "C", or "D".

                ════ QUIZ 2 — MULTIPLE_CHOICE — Vocabulary Application ═══════════
                ⚠ THIS IS NOT A COMPREHENSION QUESTION. ⚠
                ⚠ Do NOT ask about the article content. ⚠
                ⚠ Do NOT ask "What does the passage suggest..." ⚠

                This quiz tests whether a learner can USE a vocabulary word correctly.

                MANDATORY STEPS — follow exactly:
                  STEP A: From [VOCABULARY] below, pick one word. Call it WORD_X.
                  STEP B: Write a brief everyday scenario completely unrelated to the article
                           (school, cooking, sports, travel, workplace, relationships).
                  STEP C: The question asks which sentence uses WORD_X correctly in that scenario.
                  STEP D: Write exactly 4 answer choices — each MUST contain WORD_X:
                           A → WORD_X used CORRECTLY in the scenario   ← this is the correct answer
                           B → WORD_X used with wrong meaning
                           C → WORD_X used in wrong context
                           D → WORD_X used with opposite meaning
                  STEP E: Set "correctAnswer" to the letter of the correct choice.

                ════ QUIZ 3 — SHORT_ANSWER — Fill-in-the-blank ════════════════════
                ⚠ MANDATORY: The answer MUST be one of the 5 words from [VOCABULARY]. ⚠

                STEPS:
                  STEP A: Write out your 5 vocabulary words:
                          Vocab word 1: [word]  ...  Vocab word 5: [word]
                  STEP B: Pick ONE of those 5 words as the answer. Call it ANSWER_WORD.
                  STEP C: Write an everyday sentence (NOT the article topic) with one blank.
                          The sentence should make ANSWER_WORD the natural fit for the blank.
                  STEP D: Set "correctAnswer" to exactly ANSWER_WORD.
                  STEP E: "options" MUST be exactly: {}

                BANNED answers: any word NOT in [VOCABULARY].

                ════ FINAL CHECKLIST ═══════════════════════════════════════════════
                  □ Q1: requires reasoning, not factual lookup.
                  □ Q2: NOT about the article. All 4 choices contain WORD_X. WORD_X is in [VOCABULARY].
                  □ Q3: "correctAnswer" is EXACTLY one of the 5 words in [VOCABULARY]; "options" is {}.
                  □ All MCQs: exactly 4 choices, each with non-empty key, text, explanation.
                  □ Total: exactly 3 quiz objects.

                [VOCABULARY]
                %s

                Return ONLY this JSON — no prose, no markdown:
                {
                  "quizzes": [
                    {
                      "type": "MULTIPLE_CHOICE",
                      "question": "...",
                      "options": {"choices": [{"key": "A", "text": "...", "explanation": "..."}, {"key": "B", "text": "...", "explanation": "..."}, {"key": "C", "text": "...", "explanation": "..."}, {"key": "D", "text": "...", "explanation": "..."}]},
                      "correctAnswer": "A",
                      "explanation": "..."
                    },
                    {
                      "type": "MULTIPLE_CHOICE",
                      "question": "...",
                      "options": {"choices": [{"key": "A", "text": "...", "explanation": "..."}, {"key": "B", "text": "...", "explanation": "..."}, {"key": "C", "text": "...", "explanation": "..."}, {"key": "D", "text": "...", "explanation": "..."}]},
                      "correctAnswer": "B",
                      "explanation": "..."
                    },
                    {
                      "type": "SHORT_ANSWER",
                      "question": "...",
                      "options": {},
                      "correctAnswer": "...",
                      "explanation": "..."
                    }
                  ]
                }

                [CONTENT]
                %s
                """.formatted(level.name(), vocabJson, generatedContent);
    }

    // ── JSON output schemas for each step ────────────────────────────────────

    // ── Retry prompts (corrective instructions for known failure modes) ───────

    /**
     * Builds a corrective retry prompt for Step 1.
     * @param retryReason "too_short" | "too_long" | other (no special correction added)
     */
    public String buildContentRetryPrompt(String originalArticle, DifficultyLevel level, String retryReason) {
        String correction = switch (retryReason) {
            case "too_short" -> "\n⚠ CORRECTION: The previous draft was too short. " +
                    "Expand the article while preserving the same level and all key facts.";
            case "too_long"  -> "\n⚠ CORRECTION: The previous draft was too long. " +
                    "Rewrite more concisely while preserving ALL key facts.";
            default          -> "";
        };
        String base = buildContentPrompt(originalArticle, level);
        // Insert the correction instruction after the first line (after the system persona)
        return base.replaceFirst(
                "(You are an expert English content rewriter for language learners\\.)",
                "$1" + correction);
    }

    /**
     * Builds a corrective retry prompt for Step 2.
     * @param retryReason "word_not_in_content" | other
     */
    public String buildVocabularyRetryPrompt(String generatedContent, DifficultyLevel level, String retryReason) {
        String correction = switch (retryReason) {
            case "word_not_in_content" -> "\n⚠ CORRECTION: Choose only words that appear in the generated article body " +
                    "as base or inflected forms. Do NOT invent words absent from the text.";
            default -> "";
        };
        String base = buildVocabularyPrompt(generatedContent, level);
        return base.replaceFirst(
                "(You are an English vocabulary educator\\.)",
                "$1" + correction);
    }

    /**
     * Builds a corrective retry prompt for Step 3.
     * @param retryReason "q2_not_vocab_application" | "q3_answer_not_in_vocab" | other
     */
    public String buildQuizRetryPrompt(String generatedContent, String vocabJson,
                                       DifficultyLevel level, String retryReason) {
        String correction = switch (retryReason) {
            case "q2_not_vocab_application" ->
                    "\n⚠ CORRECTION: Regenerate Q2 as a vocabulary application question in a new everyday context. " +
                    "Do NOT ask about the article content. All 4 choices must contain the chosen vocabulary word.";
            case "q3_answer_not_in_vocab" ->
                    "\n⚠ CORRECTION: Regenerate Q3 so the correct answer is exactly one of the 5 vocabulary words " +
                    "listed in [VOCABULARY]. The fill-in-the-blank sentence must make that word the natural fit.";
            default -> "";
        };
        String base = buildQuizPrompt(generatedContent, vocabJson, level);
        return base.replaceFirst(
                "(You are an expert quiz designer for English language learners)",
                "$1" + correction);
    }


    public static Map<String, Object> contentSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("content", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("content"));
        schema.put("properties", props);
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> vocabularySchema() {
        Map<String, Object> itemProps = new LinkedHashMap<>();
        itemProps.put("word", Map.of("type", "string"));
        itemProps.put("definition", Map.of("type", "string"));
        itemProps.put("exampleSentence", Map.of("type", "string"));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("required", List.of("word", "definition", "exampleSentence"));
        item.put("properties", itemProps);
        item.put("additionalProperties", false);

        Map<String, Object> arr = new LinkedHashMap<>();
        arr.put("type", "array");
        arr.put("minItems", 5);
        arr.put("maxItems", 5);
        arr.put("items", item);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("vocabularies", arr);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("vocabularies"));
        schema.put("properties", props);
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> quizSchema() {
        Map<String, Object> quizProps = new LinkedHashMap<>();
        quizProps.put("type", Map.of("type", "string", "enum", List.of("MULTIPLE_CHOICE", "SHORT_ANSWER")));
        quizProps.put("question", Map.of("type", "string"));
        quizProps.put("options", Map.of("type", "object"));
        quizProps.put("correctAnswer", Map.of("type", "string"));
        quizProps.put("explanation", Map.of("type", "string"));

        Map<String, Object> quizItem = new LinkedHashMap<>();
        quizItem.put("type", "object");
        quizItem.put("required", List.of("type", "question", "options", "correctAnswer", "explanation"));
        quizItem.put("properties", quizProps);
        quizItem.put("additionalProperties", false);

        Map<String, Object> arr = new LinkedHashMap<>();
        arr.put("type", "array");
        arr.put("minItems", 3);
        arr.put("maxItems", 3);
        arr.put("items", quizItem);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("quizzes", arr);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("quizzes"));
        schema.put("properties", props);
        schema.put("additionalProperties", false);
        return schema;
    }
}
