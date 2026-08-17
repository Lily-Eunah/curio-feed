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

    public static final String PROMPT_VERSION = "v3.0";

    public String getPromptVersion() {
        return PROMPT_VERSION;
    }

    // ── Step 1: Content ───────────────────────────────────────────────────────

    public String buildSourceDigestPrompt(String originalTitle, String originalArticle) {
        return """
                You are a precise information analyst.
                Your task is to compress a long news article into a structured "Source Digest" that will be used by another AI to write a short version for language learners.

                COMPRESSION RULES:
                1. DO NOT write a story. Extract core information in bullet points or short sentences.
                2. PRESERVE: Central story, main actors, main cause-effect relationships, and the final outcome.
                3. KEEP 1-2 HUMAN DETAILS. A digest of pure statistics produces a lifeless article.
                   Pick the one or two concrete, specific details that make the central story real to a
                   reader — what a named person experienced, a price they paid, a number that lands.
                   Report them in your own words as indirect speech. NEVER copy the original wording:
                   write `a British mother said her family ate a street-food meal for about one pound`,
                   not the sentence the article used. Put these in humanDetails, not coreFacts.
                4. REMOVE: repeated examples, third and later quotes making the same point,
                   scene-setting description, and non-essential background.
                5. DO NOT add any information or interpretations not present in the original article.
                   This digest is reused for all three difficulty levels, so a single invented fact
                   appears in three published articles. Be stricter than feels necessary.
                   In particular:
                   • Do NOT attach a date to an event unless the article attaches that date to that
                     same event. If the article dates one thing to 1946 and says another happened
                     "after the war", keep the second one vague.
                   • Do NOT convert a described role into a different one. "the third generation to
                     lead the company" is not "the grandson of the founder", even if that follows.
                   • Do NOT resolve what the article leaves open. If it says "some sellers", do not
                     write "most sellers".
                   • When the article is vague, your digest must stay equally vague.
                6. Ensure the digest is concise but contains enough factual density for a 260-420 word summary.
                7. Generate a completely new, engaging English title based solely on the extracted facts.
                   ORIGINAL TITLE (do NOT reuse any phrase of 3 or more consecutive words from this): "%s"
                   Use a different angle, verb, or structure to express the same facts.
                   Keep it plain and concrete. No metaphors stacked on metaphors — a reader at A2 level
                   must understand the title without decoding imagery.

                Return ONLY this JSON:
                {
                  "sourceDigest": {
                    "suggestedTitle": "A clear, original English news title",
                    "centralStory": "1-2 sentences summarizing the main event",
                    "coreFacts": ["Fact 1", "Fact 2", ...],
                    "supportingDetails": ["Detail 1", ...],
                    "humanDetails": [
                      {"who": "who this is, e.g. 'a British mother who moved to Bangkok'",
                       "what": "what they did or experienced, in YOUR words, never the article's"}
                    ],
                    "omittedDetails": ["Briefly list what was removed (e.g., 'specific weather descriptions', 'repeated quotes from other residents')"]
                  }
                }

                [ORIGINAL ARTICLE]
                %s
                """.formatted(originalTitle, originalArticle);
    }

    public String buildContentPrompt(String sourceText, DifficultyLevel level, boolean isDigestBased) {
        String sourceContext = isDigestBased
                ? "You are writing from a compressed SOURCE DIGEST, not from the full article. Use ONLY the facts in the digest. Do not try to restore omitted details."
                : "You are writing from the ORIGINAL ARTICLE provided below.";

        String spec = switch (level) {
            case EASY -> """
                    EASY — simplified newspaper style for motivated adult learners (A2→B1 transition):
                    Write like a real news article that has been made easier — NOT like a children's book.

                    WHAT TO KEEP (do NOT simplify these):
                    • Specific facts: exact numbers, real organization names, real places, named people.
                      "social media ad revenue is projected to reach $317 billion" — keep the number.
                      "Instagram and TikTok use algorithms" — keep the word 'algorithm', do not write 'smart computers'.
                    • The natural vocabulary of the subject: words like significant, reveal, decline, strategy,
                      consequence, debate, attempt, challenge, influence, concern, trend, recognize, respond.
                      These are normal news words — do not replace them with simpler synonyms.
                    • Connectives that show logic: however, therefore, although, as a result, meanwhile.

                    WHAT TO SIMPLIFY:
                    • Long complex sentences: break into two if a single sentence has more than 3 clauses.
                    • Technical jargon beyond B1: briefly clarify in a parenthetical or follow-up phrase.
                    • Idiomatic or culture-specific expressions that a non-native reader would not recognize.

                    • Average sentence length about 12 words. Never write a sentence over 22 words.
                    • One main idea per sentence. At most one subordinate clause.
                    • Include about 4-5 core facts from the source.
                    • Target: 180~240 words.
                    • Absolute hard limit: 280 words.
                    • Paragraph structure: Write EXACTLY 3 paragraphs.
                      Paragraph 1 — the main event (what happened).
                      Paragraph 2 — key details (who did what, immediate consequences).
                      Paragraph 3 — background or broader impact.""";
            case MEDIUM -> """
                    MEDIUM — standard news reporting for learners moving from B1 to B2.
                    This must read as clearly MORE demanding than EASY, not as EASY with a few
                    longer words. The difference is structural, not decorative.

                    SENTENCE CONSTRUCTION:
                    • Average sentence length about 18 words. Most sentences carry one subordinate clause.
                    • Combine related facts into a single sentence instead of listing them separately.
                      EASY would write: "Panama came first. It scored well on personal finance."
                      MEDIUM writes:    "Panama took the top spot for the third year running, helped by
                                        its strong scores for personal finance and working conditions."
                    • Make cause and effect explicit with connectives: because, which meant, as a result,
                      even though, while.

                    WHAT MEDIUM ADDS OVER EASY:
                    • Attribute claims to whoever made them (the survey, the researchers, a named expert).
                    • Keep one short direct quotation if the source has a memorable one.
                    • Keep the qualifications and exceptions EASY would drop ("in most cases", "though
                      the picture differs outside the US").
                    • Include about 6-7 core facts from the source.

                    • Target: 290~340 words.
                      (Stated slightly above the accepted range: the model reliably lands short.)
                    • Absolute hard limit: 380 words.
                    • Paragraph structure: Write 3 to 4 paragraphs covering the main event,
                      key details, and context or consequences.""";
            case HARD -> """
                    HARD — the register of a quality broadsheet feature (BBC long-read, The Economist).
                    C1 reading level.

                    COMPLEXITY COMES FROM IDEAS AND SYNTAX, NOT FROM RARE WORDS:
                    • Do NOT swap plain words for rarer synonyms. Write "closely tied to", not
                      "inextricably linked to"; "sell off unsold stock", not "liquidate stagnant
                      inventory"; "a secret project called", not "a clandestine initiative dubbed".
                      Thesaurus prose is a failure, not a success, at this level.
                    • Difficulty should come from: longer sentences with embedded clauses,
                      nominalisation, hedged and qualified claims, and information density.
                    • Use a rarer word ONLY when it is the precise term and a plain one would be vague.

                    WHAT HARD ADDS OVER MEDIUM:
                    • Competing viewpoints and the tension between them, not just the headline claim.
                    • The reasoning behind expert positions, not only their conclusions.
                    • Caveats, limitations, and what remains unresolved.
                    • Include about 9-11 core facts from the source.

                    • Target: 410~470 words.
                      (Stated above the accepted range: undershoot grows with length — a 380-word
                      instruction produced 363 words in testing.)
                    • Absolute hard limit: 520 words.
                    • Paragraph structure: Write 3 to 4 paragraphs covering the main event,
                      key details, and context or consequences.""";
        };

        return """
                You are a journalist writing calibrated news articles for English learners at different proficiency levels.

                %s

                LEVEL REQUIREMENTS:
                %s

                LEVEL CALIBRATION — these three sentences describe the same fact at each level.
                Readers are shown them when choosing their level, so your output must match this gradient:
                  EASY   "Many people are moving to smaller homes to save money and live more simply."
                  MEDIUM "A growing number of young professionals are choosing smaller living spaces as
                          urban housing costs continue to rise."
                  HARD   "An emerging demographic shift toward compact urban dwelling reflects broader
                          anxieties about affordability, sustainability, and the perceived superfluity
                          of space."

                STRICT LENGTH POLICY — HIGHEST PRIORITY:
                1. The absolute hard limit is mandatory. NEVER exceed it.
                2. Aim for the target range (preferred range).
                3. Word limit is more important than preserving every detail.
                4. Check your word count. If below the target minimum, add more detail from the source.

                CONTENT SELECTION RULES:
                1. Preserve the central story, main actors, main event, main causes, and main consequences.
                2. Work at least one HUMAN DETAIL from the source into the article — a named person and
                   what they did, paid, or experienced. A piece built only from rankings and percentages
                   is accurate and unreadable. Report it as indirect speech in your own words.
                3. Omit repeated examples and non-essential background.
                4. Do NOT add information not found in the source.
                5. Natural flowing prose only — no bullet points, headers, or lists.

                PARAGRAPH FORMAT — REQUIRED:
                • Write the article in 3 to 4 natural paragraphs.
                • Do not return one large block of text.
                • Separate paragraphs with a blank line.
                • Each paragraph should cover one coherent idea or stage of the story.
                • Do not use bullet points, numbered lists, or line breaks after every sentence.

                Return ONLY this JSON — no other text, no markdown.
                Paragraphs MUST be separated by \\n\\n inside the JSON string:
                {"content": "First paragraph text here.\\n\\nSecond paragraph text here.\\n\\nThird paragraph text here."}

                [SOURCE TEXT]
                %s
                """.formatted(sourceContext, spec, sourceText);
    }

    // ── Step 2: Vocabulary ────────────────────────────────────────────────────

    public String buildVocabularyPrompt(String generatedContent, DifficultyLevel level) {
        String spec = switch (level) {
            case EASY -> """
                    EASY — target B1 CEFR level words.
                    MINIMUM BAR: The word must require intentional study for an A2 learner.
                    REJECT if any of the following is true:
                      • A 10-year-old native speaker would already know this word without studying → too simple (A1/A2)
                        Examples of words to ALWAYS reject at EASY: exercise, research, progress, improve, amount, benefit, digital, request, stable, record, difficult, simple, modern, popular, important, create, develop, problem, result, process
                      • The word is the article's own topic keyword (e.g., in a strength-training article, reject 'exercise', 'training', 'muscle'; in a tech article, reject 'chip', 'computer', 'data', 'software')
                      • The word appears so frequently across all news that it carries no distinctive learning value""";
            case MEDIUM -> """
                    MEDIUM — target B2 CEFR level words.
                    MINIMUM BAR: The word should challenge a confident B1 learner.
                    REJECT if any of the following is true:
                      • A learner who has finished a B1 course would already know and control this word comfortably → too simple
                      • The word is a basic B1 word clearly more suited to the EASY level
                        Examples of words to ALWAYS reject at MEDIUM: standard, traditional, expand, analyze, apply, consider, suggest, provide, increase, various, achieve, feature, structure""";
            case HARD -> """
                    HARD — target C1 CEFR level words.
                    MINIMUM BAR: The word should require intentional vocabulary study even for a B2 learner.
                    REJECT if any of the following is true:
                      • A B2 learner would recognize and actively use this word without difficulty → too simple
                      • The word is a B1/B2 word more appropriate for EASY or MEDIUM level""";
        };

        return """
                You are an English vocabulary educator.
                Your task has TWO phases. Complete Phase 0 BEFORE Phase 1.

                DIFFICULTY TARGET:
                %s

                ════ BASE FORM RULE — CRITICAL ════════════════════════════════════════
                Always use the dictionary base form, never an inflected form.

                  content: "targeted"    → vocab word: "target"     ✓
                  content: "targeted"    → vocab word: "targeted"   ✗ WRONG
                  content: "surged"      → vocab word: "surge"      ✓
                  content: "restricting" → vocab word: "restrict"   ✓
                  content: "announced"   → vocab word: "announce"   ✓
                  content: "depreciation"→ vocab word: "depreciate" ✓
                ════════════════════════════════════════════════════════════════════════

                ══ PHASE 0 — CANDIDATE SCAN ════════════════════════════════════════════
                Scan EVERY word in the [CONTENT] below.
                Write the BASE FORM of every word that satisfies ALL of the following:
                  (a) The base form or an inflected form appears in the content.
                  (b) The word passes the DIFFICULTY TARGET — none of the REJECT conditions apply.

                Write all qualifying words in the "candidates" array.
                Do NOT skip rare or less-common words — include every eligible word you find.
                Aim for at least 8–15 candidates. If you find fewer than 5, slightly lower your bar.

                ══ PHASE 1 — SELECTION ═════════════════════════════════════════════════
                From your "candidates" list, choose exactly 5 words for "vocabularies".

                Prefer words that:
                  • Offer the highest learning value (learners will encounter them in other contexts)
                  • Are specific and memorable, not vague or generic
                  • Are NOT near-synonyms of each other (e.g., do not pick both 'incorporate' and 'integrate')

                DEFINITION FORMAT — every definition MUST follow this exact pattern:
                  "[brief meaning] — used when [specific situation or condition]"

                  ✓ "to prevent access to an area by surrounding it — used when a military or political power wants to cut off a region from outside contact"
                  ✗ "relating to the sea" — REJECTED: missing "used when" clause

                EXAMPLE SENTENCE — strict rules:
                  • Use a COMPLETELY DIFFERENT TOPIC: cooking, sports, school, shopping, travel, relationships, workplace.
                  • Must NOT reference the article's topic, country, or any person named in the article.

                ════ SELF-CHECK before outputting ════════════════════════════════════
                For each of your 5 selected words:
                  1. Is it in the "candidates" list?  If NO → it is not allowed.
                  2. Is it the BASE FORM (not inflected)?  If NO → fix it.
                  3. Does it pass the DIFFICULTY TARGET minimum bar?  If NO → replace it.
                  4. Does the definition end with a "used when" clause?  If NO → rewrite it.
                  5. Is another selected word a near-synonym of this one?  If YES → replace the weaker of the two.
                ════════════════════════════════════════════════════════════════════════

                Return ONLY this JSON — no other text:
                {"candidates": ["word1", "word2", ...], "vocabularies": [{"word": "...", "definition": "...", "exampleSentence": "..."}, ...]}

                [CONTENT]
                %s
                """.formatted(spec, generatedContent);
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

                ⚠ Do NOT ask about vocabulary word definitions. ⚠
                ⚠ Do NOT ask "Which sentence uses the word X correctly?" ⚠
                ⚠ CRITICAL: The correct answer must NOT be found in a single sentence of the article.
                  If a learner can locate one sentence and immediately answer, it is a lookup — redesign as Q1.
                  Q2 requires connecting information from at least two different parts of the article,
                  OR drawing a logical conclusion that goes beyond what is explicitly stated. ⚠

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
                     → If Q1 begins with "What can be inferred", that is a Q2 pattern — redesign Q1.
                  □ Q2: reasoning — cause/effect or inference, not a vocabulary definition question.
                     → If the correct answer appears in a single sentence of the article, Q2 is too shallow — add synthesis or inference depth.
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
     * Builds a corrective retry prompt for SOURCE_DIGEST when the suggested title
     * is too similar to the original (detected by TitleSimilarityValidator).
     */
    public String buildSourceDigestRetryPrompt(String originalTitle, String originalArticle) {
        String correction = "\n⚠ CORRECTION: The suggested title was too similar to the original title. " +
                "Generate a completely different title — avoid repeating these specific words and phrases: \"" +
                originalTitle + "\". " +
                "Choose a different angle, verb, and sentence structure.";
        String base = buildSourceDigestPrompt(originalTitle, originalArticle);
        return base.replaceFirst(
                "(You are a precise information analyst\\.)",
                "$1" + correction);
    }

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

        // retryReason is only set for word-count failures; a safety or parse failure leaves it
        // null, and switching on null throws NPE and burns the retry attempt.
        String correction = switch (retryReason == null ? "" : retryReason) {
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
                "(You are a journalist writing calibrated news articles for English learners at different proficiency levels\\.)",
                "$1" + correction);
    }

    /**
     * Builds a corrective retry prompt for Step 2 when selected words overlap with recent articles.
     * Appends a small exclusion list (only the duplicate words) to the base prompt.
     */
    public String buildVocabularyDeduplicationRetryPrompt(String generatedContent, DifficultyLevel level,
                                                          List<String> excludeWords) {
        String exclusionList = String.join(", ", excludeWords);
        String correction = "\n⚠ CORRECTION: The following words have been used in recent articles and must NOT appear in your Phase 1 selection: "
                + exclusionList + ". In Phase 1, choose different words from your Phase 0 candidates list that do NOT match this exclusion list.";
        String base = buildVocabularyPrompt(generatedContent, level);
        return base.replaceFirst(
                "(You are an English vocabulary educator\\.)",
                "$1" + correction);
    }

    /**
     * Builds a corrective retry prompt for Step 2.
     * @param retryReason "word_not_in_content" | other
     */
    public String buildVocabularyRetryPrompt(String generatedContent, DifficultyLevel level, String retryReason) {
        String correction = switch (retryReason) {
            case "word_not_in_content" -> "\n⚠ CORRECTION: In Phase 0, list only words whose base or inflected form " +
                    "actually appears in the content. In Phase 1, select only from the Phase 0 candidates. " +
                    "Do NOT invent words absent from the text. " +
                    "Also ensure every word meets the DIFFICULTY TARGET minimum bar for this level.";
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
        digestProps.put("suggestedTitle", Map.of("type", "string"));
        digestProps.put("centralStory", Map.of("type", "string"));
        digestProps.put("coreFacts", Map.of("type", "array", "items", Map.of("type", "string")));
        digestProps.put("supportingDetails", Map.of("type", "array", "items", Map.of("type", "string")));

        Map<String, Object> humanDetailProps = new LinkedHashMap<>();
        humanDetailProps.put("who", Map.of("type", "string"));
        humanDetailProps.put("what", Map.of("type", "string"));
        Map<String, Object> humanDetailObj = new LinkedHashMap<>();
        humanDetailObj.put("type", "object");
        humanDetailObj.put("required", List.of("who", "what"));
        humanDetailObj.put("properties", humanDetailProps);
        humanDetailObj.put("additionalProperties", false);
        digestProps.put("humanDetails", Map.of("type", "array", "items", humanDetailObj));

        digestProps.put("omittedDetails", Map.of("type", "array", "items", Map.of("type", "string")));

        Map<String, Object> digestObj = new LinkedHashMap<>();
        digestObj.put("type", "object");
        digestObj.put("required", List.of("suggestedTitle", "centralStory", "coreFacts", "supportingDetails", "humanDetails", "omittedDetails"));
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
        // candidates: Phase 0 scan result — string array, declared first so Gemini generates it before vocabularies
        Map<String, Object> candidatesArr = new LinkedHashMap<>();
        candidatesArr.put("type", "array");
        candidatesArr.put("items", Map.of("type", "string"));

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
        props.put("candidates", candidatesArr);  // before vocabularies — order matters for chain-of-thought
        props.put("vocabularies", arr);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("candidates", "vocabularies"));
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
