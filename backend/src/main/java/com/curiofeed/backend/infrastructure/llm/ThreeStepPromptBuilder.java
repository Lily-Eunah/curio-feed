package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.infrastructure.llm.validation.ContentValidationResult;
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

    public String buildSourceDigestPrompt(String originalArticle) {
        return """
                You are a precise information analyst.
                Your task is to compress a long news article into a structured "Source Digest" that will be used by another AI to write a short version for language learners.

                COMPRESSION RULES:
                1. DO NOT write a story. Extract core information in bullet points or short sentences.
                2. PRESERVE: Central story, main actors, main cause-effect relationships, and the final outcome.
                3. REMOVE: Minor examples, repeated details, secondary quotes, scene-setting descriptions, and non-essential background.
                4. DO NOT add any information or interpretations not present in the original article.
                5. Ensure the digest is concise but contains enough factual density for a 260-420 word summary.

                Return ONLY this JSON:
                {
                  "sourceDigest": {
                    "centralStory": "1-2 sentences summarizing the main event",
                    "coreFacts": ["Fact 1", "Fact 2", ...],
                    "supportingDetails": ["Detail 1", ...],
                    "omittedDetails": ["Briefly list what was removed (e.g., 'specific weather descriptions', 'secondary quotes from neighbors')"]
                  }
                }

                [ORIGINAL ARTICLE]
                %s
                """.formatted(originalArticle);
    }

    public String buildContentPrompt(String sourceText, DifficultyLevel level, boolean isDigestBased) {
        String sourceContext = isDigestBased
                ? "You are writing from a compressed SOURCE DIGEST, not from the full article. Use ONLY the facts in the digest. Do not try to restore omitted details."
                : "You are writing from the ORIGINAL ARTICLE provided below.";

        String spec = switch (level) {
            case EASY -> """
                    EASY (A2-B1 level):
                    • Keep sentences short — aim for 12-15 words per sentence.
                    • Use only common vocabulary a 10-year-old native speaker would know.
                    • Explain difficult ideas in simple words.
                    • Include about 5-6 core facts from the source.
                    • Target: 180~260 words.
                    • Absolute hard limit: 320 words.""";
            case MEDIUM -> """
                    MEDIUM (B1-B2 level):
                    • Natural news-writing style with moderate sentence variety.
                    • Topic-specific vocabulary is acceptable if context makes it clear.
                    • Include about 6-8 core facts from the source.
                    • Preserve the main cause-effect relationships and actor motivations.
                    • Target: 220~320 words.
                    • Absolute hard limit: 380 words.""";
            case HARD -> """
                    HARD (C1 level):
                    • Use advanced vocabulary and varied sentence structures.
                    • Use formal, precise register with dense information delivery.
                    • Preserve nuance, expert viewpoints, and causal chains.
                    • Include about 8-10 core facts from the source.
                    • Target: 280~420 words.
                    • Absolute hard limit: 500 words.""";
        };

        return """
                You are an expert English content adapter for language learners.

                %s

                LEVEL REQUIREMENTS:
                %s

                STRICT LENGTH POLICY — HIGHEST PRIORITY:
                1. The absolute hard limit is mandatory. NEVER exceed it.
                2. Aim for the target range (preferred range).
                3. Word limit is more important than preserving every detail.
                4. Check your word count. If below the target minimum, add more detail from the source.

                CONTENT SELECTION RULES:
                1. Preserve the central story, main actors, main event, main causes, and main consequences.
                2. Omit minor examples, repeated details, secondary quotes, and non-essential background.
                3. Do NOT add information not found in the source.
                4. Natural flowing prose only — no bullet points, headers, or lists.

                PARAGRAPH FORMAT — REQUIRED:
                • Write the article in 3 to 4 natural paragraphs.
                • Do not return one large block of text.
                • Separate paragraphs with a blank line.
                • Each paragraph should cover one coherent idea or stage of the story.
                • Do not use bullet points, numbered lists, or line breaks after every sentence.

                Return ONLY this JSON — no other text, no markdown:
                {"content": "your rewritten article here"}

                [SOURCE TEXT]
                %s
                """.formatted(sourceContext, spec, sourceText);
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
                  (B) 5 vocabulary words extracted from that article — [VOCABULARY] below

                Design EXACTLY 3 quizzes that test reading comprehension and vocabulary in context.

                ════ QUIZ 1 — MULTIPLE_CHOICE — Passage Comprehension ══════════════
                Test whether the learner understood the article's central point or main situation.

                BANNED question types:
                  ✗ Any question about a specific number, percentage, date, or country name
                  ✗ Copying a single sentence from the article and asking who/what/when

                GOOD question starters:
                  ✓ "What best summarizes the main concern described in the article?"
                  ✓ "Which statement best reflects the article's central point?"
                  ✓ "What is the overall situation described in the article?"
                  ✓ "According to the article, what is the main challenge facing...?"

                4 choices (A/B/C/D). ONE correct. Wrong choices are plausible but contradict the passage.
                Each choice: non-empty explanation. "correctAnswer": exactly "A", "B", "C", or "D".

                ════ QUIZ 2 — MULTIPLE_CHOICE — Passage Reasoning ══════════════════
                Test cause-effect, motivation, or inference from the article.
                The correct answer must require understanding the passage, not just finding one sentence.

                ⚠ Do NOT ask about vocabulary word definitions. ⚠
                ⚠ Do NOT ask "Which sentence uses the word X correctly?" ⚠

                GOOD question starters:
                  ✓ "Why did X happen despite Y?"
                  ✓ "What can be inferred from the fact that...?"
                  ✓ "What does the author imply about...?"
                  ✓ "What is the most likely reason that...?"
                  ✓ "What would most likely happen if...?"

                4 choices (A/B/C/D). ONE correct. Wrong choices are plausible misconceptions.
                Each choice: non-empty explanation. "correctAnswer": exactly "A", "B", "C", or "D".

                ════ QUIZ 3 — SHORT_ANSWER — Passage-Grounded Vocabulary Application ════
                ⚠ This tests both reading comprehension AND vocabulary in context. ⚠
                ⚠ Do NOT write a fill-in-the-blank sentence unrelated to the article. ⚠
                ⚠ Do NOT ask "Which vocabulary word fits the blank?" ⚠

                MANDATORY STEPS — follow exactly:
                  STEP A: List your 5 vocabulary words from [VOCABULARY].
                  STEP B: Pick ONE word from the list. Call it TARGET_WORD.
                  STEP C: Write a question that asks the learner to explain something from the article
                           in their own words — and explicitly requires them to use TARGET_WORD.
                           Format: "In one sentence, [article-based task]. Use the word '[TARGET_WORD]' in your answer."
                  STEP D: Set "correctAnswer" to a complete model answer sentence that:
                           • answers the article-based task
                           • contains TARGET_WORD (or an inflected form)
                  STEP E: Set "explanation" to: "Target word: TARGET_WORD"
                  STEP F: "options" MUST be exactly: {}

                BANNED for Q3:
                  ✗ Fill-in-the-blank sentences unrelated to the article
                  ✗ "Use the word in any sentence" (must be grounded in article content)
                  ✗ correctAnswer that does not contain TARGET_WORD or its inflected form

                ════ FINAL CHECKLIST ═══════════════════════════════════════════════
                  □ Q1: comprehension — main idea or central situation, not a factual lookup.
                  □ Q2: reasoning — cause/effect or inference, not a vocabulary definition question.
                  □ Q3: "question" explicitly names the target vocab word and asks about the article.
                  □ Q3: "correctAnswer" is a complete sentence containing the target vocab word.
                  □ Q3: "options" is {}.
                  □ All MCQs: exactly 4 choices (A/B/C/D), each with non-empty text and explanation.
                  □ All MCQs: "correctAnswer" is exactly "A", "B", "C", or "D".
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
                      "question": "In one sentence, [article-based task]. Use the word '[TARGET_WORD]' in your answer.",
                      "options": {},
                      "correctAnswer": "A complete model answer sentence containing [TARGET_WORD].",
                      "explanation": "Target word: [TARGET_WORD]"
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
     */
    public String buildContentRetryPrompt(String sourceText, DifficultyLevel level,
                                          ContentValidationResult result, boolean isDigestBased) {
        String retryReason = result.getRetryReason();
        int actual = result.getActualWordCount();
        int hardMin = result.getHardMin();
        int hardMax = result.getHardMax();
        int prefMin = result.getPreferredMin();
        int prefMax = result.getPreferredMax();

        String correction = switch (retryReason) {
            case "too_short" -> """
                    
                    ⚠ CORRECTION: The previous draft was too short (%d words, hard minimum is %d).
                    Expand the article to the preferred range (%d~%d words).
                    Add more details from the source (central story, core facts, background).
                    STAY BELOW the absolute hard limit of %d words.""".formatted(actual, hardMin, prefMin, prefMax, hardMax);
            case "too_long" -> """
                    
                    ⚠ CORRECTION: The previous draft was too long (%d words, absolute hard limit is %d).
                    Rewrite more concisely to fit the preferred range (%d~%d words).
                    STRICT WORD LIMIT IS HIGHER PRIORITY than detail preservation.
                    Remove minor background, repeated details, and secondary quotes.
                    Keep only the central event, main actors, and main consequences.""".formatted(actual, hardMax, prefMin, prefMax);
            default -> "";
        };

        String base = buildContentPrompt(sourceText, level, isDigestBased);
        return base.replaceFirst(
                "(You are an expert English content adapter for language learners\\.)",
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
            case "q1_shallow" ->
                    "\n⚠ CORRECTION: Regenerate Q1 as a passage comprehension question about the article's main " +
                    "idea or central situation. Do NOT ask about specific numbers, dates, or named countries.";
            case "q2_not_reasoning" ->
                    "\n⚠ CORRECTION: Regenerate Q2 as a passage reasoning question (cause/effect or inference). " +
                    "Do NOT ask about vocabulary word meanings or usage.";
            case "q3_not_passage_grounded" ->
                    "\n⚠ CORRECTION: Regenerate Q3 so the question asks about the article content and explicitly " +
                    "requires the learner to use a vocabulary word from [VOCABULARY] in their answer. " +
                    "The correctAnswer must be a complete sentence containing that vocabulary word.";
            case "q3_answer_missing_vocab" ->
                    "\n⚠ CORRECTION: Regenerate Q3 so the correctAnswer is a complete sentence that contains " +
                    "the target vocabulary word named in the question.";
            default -> "";
        };
        String base = buildQuizPrompt(generatedContent, vocabJson, level);
        return base.replaceFirst(
                "(You are an expert quiz designer for English language learners)",
                "$1" + correction);
    }


    public static Map<String, Object> sourceDigestSchema() {
        Map<String, Object> digestProps = new LinkedHashMap<>();
        digestProps.put("centralStory", Map.of("type", "string"));
        digestProps.put("coreFacts", Map.of("type", "array", "items", Map.of("type", "string")));
        digestProps.put("supportingDetails", Map.of("type", "array", "items", Map.of("type", "string")));
        digestProps.put("omittedDetails", Map.of("type", "array", "items", Map.of("type", "string")));

        Map<String, Object> digestObj = new LinkedHashMap<>();
        digestObj.put("type", "object");
        digestObj.put("required", List.of("centralStory", "coreFacts", "supportingDetails", "omittedDetails"));
        digestObj.put("properties", digestProps);
        digestObj.put("additionalProperties", false);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("sourceDigest"));
        schema.put("properties", Map.of("sourceDigest", digestObj));
        schema.put("additionalProperties", false);
        return schema;
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
        // Choice item: {key, text, explanation}
        Map<String, Object> choiceItemProps = new LinkedHashMap<>();
        choiceItemProps.put("key", Map.of("type", "string"));
        choiceItemProps.put("text", Map.of("type", "string"));
        choiceItemProps.put("explanation", Map.of("type", "string"));
        Map<String, Object> choiceItem = new LinkedHashMap<>();
        choiceItem.put("type", "object");
        choiceItem.put("required", List.of("key", "text"));
        choiceItem.put("properties", choiceItemProps);

        // options: {choices: [...]} — choices is optional so Q3 (SHORT_ANSWER) can be {}
        Map<String, Object> optionsProps = new LinkedHashMap<>();
        optionsProps.put("choices", Map.of("type", "array", "items", choiceItem));
        Map<String, Object> optionsSchema = new LinkedHashMap<>();
        optionsSchema.put("type", "object");
        optionsSchema.put("properties", optionsProps);

        Map<String, Object> quizProps = new LinkedHashMap<>();
        quizProps.put("type", Map.of("type", "string", "enum", List.of("MULTIPLE_CHOICE", "SHORT_ANSWER")));
        quizProps.put("question", Map.of("type", "string"));
        quizProps.put("options", optionsSchema);
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
