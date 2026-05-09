package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 3-step pipeline experiment: content → vocab → quiz, each as a separate LLM call.
 * Baseline comparison: OllamaLlmPipelineIntegrationTest (single-shot).
 * DO NOT modify production pipeline code.
 *
 * Live Ollama test — disabled by default.
 * Run manually:
 *   Git Bash : RUN_OLLAMA_TESTS=true ./gradlew test --tests "*OllamaThreeStepPipelineExperimentTest*"
 *   PowerShell: $env:RUN_OLLAMA_TESTS="true"; ./gradlew test --tests "*OllamaThreeStepPipelineExperimentTest*"
 */
@Tag("ollama")
@EnabledIfEnvironmentVariable(named = "RUN_OLLAMA_TESTS", matches = "true")
class OllamaThreeStepPipelineExperimentTest {

    private static final Logger log = LoggerFactory.getLogger(OllamaThreeStepPipelineExperimentTest.class);

    private static final String BASE_URL = "http://192.168.45.100:11434";
    private static final String MODEL = "gemma4:e4b";
    private static final double TEMPERATURE = 0.3;
    private static final int NUM_CTX = 16384;

    // Article 1: same as baseline (Strait of Hormuz)
    private static final String ARTICLE_HORMUZ = """
            # Strait of Hormuz closed again, Iran says, as ships attacked
            Iran says it is closing the Strait of Hormuz again to commercial vessels and that any ship that approaches it will be targeted.
            The closure came as reports emerged of vessels in or near the strait, including a tanker, were targeted by Tehran on Saturday.
            The Islamic Revolution Guard Corps (IRGC) blamed a continuing US blockade for its decision, which comes a day after Iran's foreign minister announced the key global shipping channel had been temporarily reopened.
            US President Donald Trump said Iran cannot "blackmail" the US with threats regarding the waterway, which Tehran has effectively blocked for nearly two months - causing global energy prices to soar.
            The Islamic Revolutionary Guard Corps (IRGC) Navy warned in a statement on Saturday that "no vessel is to move from its anchorage in the Persian Gulf or the Sea of Oman".
            It said a number of vessels had passed through the strait under its management since Friday night, but that it would shut again until the US stopped its blockade of Iranian ports.
            "Approaching the Strait of Hormuz will be considered co-operation with the enemy, and the offending vessel will be targeted," the IRGC added.
            Trump said on Friday that a naval blockade of Iranian ports would continue until a peace deal was agreed between the two countries. A two-week ceasefire currently in effect is due to expire on 22 April.
            The US said it had turned away 23 ships since it began enforcing the blockade on 13 April.
            Iran's Supreme National Security Council (SNSC) said this was a violation of the ceasefire agreement and that it would stop the reopening of the strait while it was still in place.
            On negotiations to bring about an end to the war, the SNSC said new proposals had been put forward by the US, which Tehran was "currently reviewing and has not yet responded to". Peace talks held earlier this month ended without an agreement.
            "We have very good conversations going on. It's working out very well," Trump said about the state of negotiations with Tehran on Saturday.
            There have been several reports of vessels being attacked by Iran on Saturday.
            Two Iranian gunboats opened fire on a tanker in the strait, the UK Maritime Trade Operations (UKMTO) said.
            A container ship was also hit by "an unknown projectile" off the north-eastern coast of Oman, damaging some containers, according to the UKMTO.
            Separately, at least two merchant vessels said they were hit by gunfire as they attempted to cross the strait, sources told news agency Reuters.
            India's foreign ministry said it had summoned the Iranian ambassador to convey its "deep concern at the shooting incident earlier today involving two Indian-flagged ships in the Strait of Hormuz".
            Data from tracking site MarineTraffic showed some vessels were able to make it through the strait while it was briefly open. Others were forced to change their route after the IRGC denied them access.
            About 20% of the world's oil and liquefied natural gas (LNG) is usually transported through the strait, but the number of ships making the journey has dramatically decreased during the recent conflict, which began when the US and Israel attacked Iran on 28 February.
            The narrow chokepoint connects the Gulf to the Arabian Sea and is the only way to reach several oil-producing states by sea. The crisis has seen the price of a barrel of oil surge above $100 ($74) at points.
            Iran has previously threatened to attack tankers and other ships, as well as warning it had laid mines.
            """;

    // Article 2: different topic (Japan economy) for robustness testing
    private static final String ARTICLE_JAPAN = """
            # Japan exits recession as economy grows in fourth quarter
            Japan's economy expanded 0.4% in the fourth quarter, ending a technical recession after two consecutive quarters of contraction. Growth was led by a rise in business investment of 2.0%, as companies upgraded equipment amid a weak yen that made imports expensive.
            Private consumption, which accounts for more than half of Japan's economy, grew 0.2% after three straight quarters of decline. Economists warned the recovery remained fragile, as real wages continued to fall because pay rises failed to keep pace with inflation.
            The yen's sharp depreciation over the past two years has had mixed effects. Exporters like Toyota posted record profits from overseas sales converted back into yen. But households faced higher prices for imported goods, squeezing their spending power.
            The Bank of Japan kept interest rates near zero while other major central banks raised rates aggressively to combat inflation. Officials signalled the bank might begin adjusting its ultra-loose monetary policy in coming months, a move that could trigger significant shifts in global capital markets as investors anticipate higher Japanese yields.
            The government announced a fresh stimulus package worth 17 trillion yen aimed at supporting low-income families with rising energy costs and helping small businesses cope with supply chain pressures. Analysts cautioned that without stronger wage growth, consumer demand would remain too weak to sustain a durable recovery.
            """;

    private static final Map<String, Object> STEP1_SCHEMA = buildStep1Schema();
    private static final Map<String, Object> STEP2_SCHEMA = buildStep2Schema();
    private static final Map<String, Object> STEP3_SCHEMA = buildStep3Schema();

    private RestClient restClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder().baseUrl(BASE_URL).build();
        objectMapper = new ObjectMapper();
    }

    // ── Test methods ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "[HORMUZ] 3-step pipeline — {0}")
    @EnumSource(DifficultyLevel.class)
    void hormuz_threeStepPipeline(DifficultyLevel level) {
        runThreeStepPipeline("HORMUZ", ARTICLE_HORMUZ, level);
    }

    @ParameterizedTest(name = "[JAPAN] 3-step pipeline — {0}")
    @EnumSource(DifficultyLevel.class)
    void japan_threeStepPipeline(DifficultyLevel level) {
        runThreeStepPipeline("JAPAN", ARTICLE_JAPAN, level);
    }

    // ── Pipeline ───────────────────────────────────────────────────────────────

    private void runThreeStepPipeline(String articleId, String originalArticle, DifficultyLevel level) {
        log.info("\n\n╔══════════════════════════════════════════════════════╗");
        log.info("║  3-STEP PIPELINE  [{} | {}]", articleId, level);
        log.info("╚══════════════════════════════════════════════════════╝\n");

        // ── Step 1: Content Generation ─────────────────────────────────────────
        String step1Raw = callOllama(buildStep1Prompt(originalArticle, level),
                STEP1_SCHEMA, articleId + "/" + level + "/S1");
        String content = extractField(step1Raw, "content");

        int wordCount = wordCount(content);
        log.info("── STEP 1 CONTENT ── {} words ──\n{}", wordCount, content);

        assertThat(content).as("[%s|%s] Step1 content must not be blank", articleId, level).isNotBlank();
        if (wordCount < 100) {
            log.warn("⚠ [{} | {}] STEP 1 content too short: {} words (expected ≥ 100) — continuing pipeline anyway",
                    articleId, level, wordCount);
        }
        if (wordCount > 500) {
            log.warn("⚠ [{} | {}] STEP 1 content too long: {} words (expected ≤ 500)", articleId, level, wordCount);
        }

        // ── Step 2: Vocabulary Extraction ─────────────────────────────────────
        String step2Raw = callOllama(buildStep2Prompt(content, level),
                STEP2_SCHEMA, articleId + "/" + level + "/S2");
        List<VocabEntry> vocabs = extractVocabularies(step2Raw);

        String contentLower = content.toLowerCase();
        log.info("── STEP 2 VOCABULARIES ──");
        long presentInContent = 0;
        for (VocabEntry v : vocabs) {
            boolean inContent = wordInContent(v.word(), contentLower);
            if (inContent) presentInContent++;
            log.info("  {} '{}' inContent={} | def='{}' | ex='{}'",
                    inContent ? "✓" : "✗", v.word(), inContent, v.definition(), v.exampleSentence());
        }
        log.info("  vocab-content alignment: {}/5", presentInContent);

        assertThat(vocabs).as("[%s|%s] Step2 must return exactly 5 vocab items", articleId, level).hasSize(5);

        // ── Step 3: Quiz Generation ────────────────────────────────────────────
        String vocabJson = toVocabJson(vocabs);
        String step3Raw = callOllama(buildStep3Prompt(content, vocabJson, level),
                STEP3_SCHEMA, articleId + "/" + level + "/S3");
        List<QuizEntry> quizzes = extractQuizzes(step3Raw);

        List<String> vocabWords = vocabs.stream().map(v -> v.word().toLowerCase()).toList();

        log.info("── STEP 3 QUIZZES ──");
        for (int i = 0; i < quizzes.size(); i++) {
            QuizEntry q = quizzes.get(i);
            log.info("  [Q{}] type={} | q='{}'", i + 1, q.type(), q.question());
            log.info("       answer='{}' | expl='{}'", q.correctAnswer(), q.explanation());
            if (q.choices() != null) {
                q.choices().forEach(c -> log.info("       {} '{}' | expl='{}'", c.key(), c.text(), c.explanation()));
            }
        }

        assertThat(quizzes).as("[%s|%s] Step3 must return exactly 3 quizzes", articleId, level).hasSize(3);

        if (quizzes.size() >= 1)
            assertThat(quizzes.get(0).type()).as("Quiz1 must be MULTIPLE_CHOICE").isEqualTo("MULTIPLE_CHOICE");
        if (quizzes.size() >= 2)
            assertThat(quizzes.get(1).type()).as("Quiz2 must be MULTIPLE_CHOICE").isEqualTo("MULTIPLE_CHOICE");
        if (quizzes.size() >= 3)
            assertThat(quizzes.get(2).type()).as("Quiz3 must be SHORT_ANSWER").isEqualTo("SHORT_ANSWER");

        // Cross-reference: Q2 vocab word check
        boolean q2UsesVocab = false;
        if (quizzes.size() >= 2 && quizzes.get(1).choices() != null) {
            q2UsesVocab = quizzes.get(1).choices().stream()
                    .anyMatch(c -> c.text() != null
                            && vocabWords.stream().anyMatch(w -> c.text().toLowerCase().contains(w)));
        }

        // Cross-reference: Q3 answer is a vocab word
        boolean q3InVocab = false;
        if (quizzes.size() >= 3 && quizzes.get(2).correctAnswer() != null) {
            String q3ans = quizzes.get(2).correctAnswer().toLowerCase().trim();
            q3InVocab = vocabWords.contains(q3ans);
        }

        // Q1 shallow-question check (heuristic)
        boolean q1Shallow = false;
        if (!quizzes.isEmpty()) {
            String q1 = quizzes.get(0).question().toLowerCase();
            q1Shallow = q1.contains("what percentage") || q1.contains("how many")
                    || q1.contains("which country") || q1.contains("what year")
                    || q1.contains("when did") || q1.matches(".*\\d+%.*");
        }

        // ── Summary ────────────────────────────────────────────────────────────
        log.info("\n┌─────────────────────────────────────────────────────┐");
        log.info("│  SUMMARY  [{} | {}]", articleId, level);
        log.info("│  Content words    : {}", wordCount);
        log.info("│  Vocab/content    : {}/5", presentInContent);
        log.info("│  Quiz count       : {}", quizzes.size());
        log.info("│  Q2 uses vocab    : {}", q2UsesVocab);
        log.info("│  Q3 ans in vocab  : {}", q3InVocab);
        log.info("│  Q1 shallow?      : {}", q1Shallow);
        log.info("└─────────────────────────────────────────────────────┘\n");

        // Soft assertions logged (not hard-failing so we see all results)
        if (presentInContent < 4) {
            log.warn("⚠ [{} | {}] vocab-content alignment LOW: only {}/5 vocab words found in content",
                    articleId, level, presentInContent);
        }
        if (!q2UsesVocab) {
            log.warn("⚠ [{} | {}] Q2 does NOT appear to use a vocab word in choices", articleId, level);
        }
        if (!q3InVocab) {
            log.warn("⚠ [{} | {}] Q3 correctAnswer '{}' is NOT in vocab list {}",
                    articleId, level,
                    quizzes.size() >= 3 ? quizzes.get(2).correctAnswer() : "N/A",
                    vocabWords);
        }
        if (q1Shallow) {
            log.warn("⚠ [{} | {}] Q1 appears to be a shallow factual-lookup question", articleId, level);
        }
    }

    // ── Step 1 Prompt (v3) ────────────────────────────────────────────────────

    private String buildStep1Prompt(String article, DifficultyLevel level) {
        String spec = switch (level) {
            case EASY -> """
                    EASY (A2-B1 level):
                    • Keep sentences short — aim for 12–15 words per sentence.
                    • Use only common vocabulary a 10-year-old native speaker would know.
                    • Replace any difficult words with simpler ones — NEVER remove facts.
                    • Cover all major events: the closure, the attacks, the US blockade, peace talks, oil impact.
                    • Write AT LEAST 180 words. Write AT MOST 250 words.
                    • If your draft is shorter than 180 words, add more facts from the original article.""";
            case MEDIUM -> """
                    MEDIUM (B1-B2 level):
                    • Natural news-writing style with moderate sentence variety.
                    • Topic-specific vocabulary is acceptable.
                    • Retain all cause-effect relationships, actor motivations, and key figures.
                    • Write AT LEAST 200 words. Write AT MOST 280 words.""";
            case HARD -> """
                    HARD (C1 level):
                    • Advanced vocabulary and complex, varied sentence structures.
                    • Formal, precise register. Dense information delivery.
                    • Preserve nuance, expert viewpoints, and geopolitical context.
                    • Write AT LEAST 220 words. Write AT MOST 300 words.""";
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
                """.formatted(spec, article);
    }

    // ── Step 2 Prompt (v2) ────────────────────────────────────────────────────

    private String buildStep2Prompt(String content, DifficultyLevel level) {
        String spec = switch (level) {
            case EASY -> "EASY (A2-B1): Words a learner would encounter in B1 reading but may not fully understand. Not trivial, not too complex.";
            case MEDIUM -> "MEDIUM (B1-B2): Academic or journalism-register vocabulary that appears naturally in news writing.";
            case HARD -> "HARD (C1): Advanced, domain-specific vocabulary that signals sophisticated writing.";
        };

        return """
                You are an English vocabulary educator.

                Extract exactly 5 vocabulary words from the %s-level news article below.

                DIFFICULTY TARGET: %s

                ════ BASE FORM RULE — CRITICAL ════════════════════════════════════
                Always list the dictionary base form, never an inflected form.

                  content: "targeted"   → vocab word: "target"    ✓
                  content: "targeted"   → vocab word: "targeted"  ✗ WRONG
                  content: "surged"     → vocab word: "surge"     ✓
                  content: "surged"     → vocab word: "surged"    ✗ WRONG
                  content: "restricting"→ vocab word: "restrict"  ✓
                  content: "announced"  → vocab word: "announce"  ✓
                  content: "blocking"   → vocab word: "block"     ✓

                The appearance check passes if ANY inflected form of the base word is in the content.
                ════════════════════════════════════════════════════════════════════

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
                ════════════════════════════════════════════════════════════════════

                DEFINITION FORMAT — every definition MUST follow this exact pattern:
                  "[brief meaning] — used when [specific situation or condition]"

                  ✓ "to prevent access to an area by surrounding it — used when a military or political power wants to cut off a region from outside contact"
                  ✓ "a strong rise in amount or intensity — used when something increases sharply over a short period"
                  ✗ "relating to the sea" — REJECTED: missing "used when" clause
                  ✗ "a ship" — REJECTED: no meaning and no "used when" clause

                EXAMPLE SENTENCE — strict rules:
                  • Use a COMPLETELY DIFFERENT TOPIC: cooking, sports, school, shopping, travel, relationships, workplace.
                  • Must NOT reference: ships, oil, Iran, US, blockade, strait, Japan, yen, GDP, economy, or any person named in the article.
                  ✓ "The coach restricted training to one session per day to prevent injuries."
                  ✗ Any sentence that could be about the article's topic.

                ════ FINAL SELF-CHECK before outputting ════════════════════════════
                For each of your 5 words, answer:
                  1. Is it the BASE FORM (not inflected)?  If NO → fix it.
                  2. Does it or an inflected form appear in the content?  If NO → replace it.
                  3. Is it on the FORBIDDEN LIST?  If YES → replace it.
                  4. Does the definition end with a "used when" clause?  If NO → rewrite it.
                ════════════════════════════════════════════════════════════════════

                Return ONLY this JSON — no other text:
                {"vocabularies": [{"word": "...", "definition": "...", "exampleSentence": "..."}, ...]}

                [CONTENT]
                %s
                """.formatted(level.name(), spec, content);
    }

    // ── Step 3 Prompt (v2) ────────────────────────────────────────────────────

    private String buildStep3Prompt(String content, String vocabJson, DifficultyLevel level) {
        return """
                You are an expert quiz designer for English language learners (%s level).

                INPUT:
                  (A) A news article — [CONTENT] below
                  (B) 5 vocabulary words from that article — [VOCABULARY] below

                Design EXACTLY 3 quizzes.

                ════════════════════════════════════════════════════════════════════════
                QUIZ 1 — MULTIPLE_CHOICE — Comprehension / Reasoning
                ════════════════════════════════════════════════════════════════════════
                Test a cause-effect relationship or inference from the article.
                The correct answer must require understanding the passage, not just finding a sentence.

                BANNED question starters:
                  ✗ "What percentage..." ✗ "How many..." ✗ "Which country..."
                  ✗ "When did..." ✗ Any question about a specific number or date

                GOOD question starters:
                  ✓ "Why did X lead to Y?" ✓ "What does the author imply about...?"
                  ✓ "What would most likely happen if...?" ✓ "What is the underlying reason for...?"

                4 choices (A/B/C/D). ONE correct. Wrong choices are plausible misconceptions.
                Each choice: non-empty explanation. "correctAnswer": exactly "A", "B", "C", or "D".

                ════════════════════════════════════════════════════════════════════════
                QUIZ 2 — MULTIPLE_CHOICE — Vocabulary Application (NOT comprehension)
                ════════════════════════════════════════════════════════════════════════
                ⚠ THIS IS NOT A COMPREHENSION QUESTION. ⚠
                ⚠ Do NOT ask about the article content. ⚠
                ⚠ Do NOT ask "What does the passage suggest..." or "According to the article..." ⚠

                This quiz tests whether a learner can USE a vocabulary word correctly.

                MANDATORY STEPS — follow exactly:
                  STEP A: From [VOCABULARY] below, pick one word. Call it WORD_X.
                           Write: "I chose WORD_X = [the word]"
                  STEP B: Write a brief everyday scenario completely unrelated to the article
                           (school, cooking, sports, travel, workplace, relationships).
                  STEP C: The question asks: "Which sentence uses WORD_X correctly in this situation?"
                           (do NOT name WORD_X in the question itself)
                  STEP D: Write exactly 4 answer choices — each MUST contain WORD_X:
                           A → WORD_X used CORRECTLY in the scenario   ← this is the correct answer
                           B → WORD_X used with wrong meaning
                           C → WORD_X used in wrong context
                           D → WORD_X used with opposite meaning
                  STEP E: Set "correctAnswer" to the letter of the correct choice.

                VERIFICATION: Every one of the 4 choices must contain the literal word WORD_X.
                              If any choice is missing WORD_X → rewrite that choice.

                ════════════════════════════════════════════════════════════════════════
                QUIZ 3 — SHORT_ANSWER — Fill-in-the-blank
                ════════════════════════════════════════════════════════════════════════
                ⚠ MANDATORY: The answer MUST be one of the 5 words from [VOCABULARY]. ⚠

                STEPS:
                  STEP A: Write out your 5 vocabulary words:
                          Vocab word 1: [word]
                          Vocab word 2: [word]
                          ... (list all 5)
                  STEP B: Pick ONE of those 5 words as the answer. Call it ANSWER_WORD.
                  STEP C: Write an everyday sentence (NOT the article topic) with one blank.
                          The sentence should make ANSWER_WORD the natural fit for the blank.
                  STEP D: Set "correctAnswer" to exactly ANSWER_WORD.
                  STEP E: "options" MUST be exactly: {}

                BANNED answers: any word NOT in [VOCABULARY].
                If your answer is not one of the 5 vocab words → REWRITE this quiz.

                ════════════════════════════════════════════════════════════════════════
                FINAL CHECKLIST — verify each item before outputting
                ════════════════════════════════════════════════════════════════════════
                  □ Q1: requires reasoning, not factual lookup.
                  □ Q2: NOT about the article. All 4 choices contain WORD_X. WORD_X is in [VOCABULARY].
                  □ Q3: "correctAnswer" is EXACTLY one of the 5 words listed in [VOCABULARY].
                  □ Q3: "options" is exactly {}.
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
                """.formatted(level.name(), vocabJson, content);
    }

    // ── Ollama client ──────────────────────────────────────────────────────────

    private String callOllama(String prompt, Map<String, Object> schema, String tag) {
        log.debug("▶ Calling Ollama [{}] ({} chars prompt)", tag, prompt.length());
        OllamaRequest req = new OllamaRequest(
                MODEL, prompt, false, new OllamaRequest.Options(NUM_CTX, TEMPERATURE), schema);
        try {
            OllamaResponse resp = restClient.post()
                    .uri("/api/generate")
                    .body(req)
                    .retrieve()
                    .onStatus(s -> s.isError(), (r, res) -> {
                        throw new LlmClientException("Ollama HTTP " + res.getStatusCode() + " [" + tag + "]");
                    })
                    .body(OllamaResponse.class);

            if (resp == null || resp.response() == null) {
                throw new LlmClientException("Ollama null response [" + tag + "]");
            }
            log.debug("◀ Ollama [{}] done ({} chars)", tag, resp.response().length());
            return resp.response();
        } catch (LlmClientException e) {
            throw e;
        } catch (RestClientException e) {
            fail("Ollama endpoint unreachable at " + BASE_URL + " [" + tag + "]: " + e.getMessage());
            throw new AssertionError("unreachable");
        }
    }

    // ── JSON parsing ───────────────────────────────────────────────────────────

    private String extractField(String raw, String field) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                throw new LlmParseException("Field '" + field + "' missing. Raw: " + abbrev(raw));
            }
            return node.asText();
        } catch (LlmParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmParseException("Parse error extracting '" + field + "': " + abbrev(raw));
        }
    }

    private List<VocabEntry> extractVocabularies(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            JsonNode arr = root.get("vocabularies");
            if (arr == null || !arr.isArray()) {
                throw new LlmParseException("'vocabularies' missing or not array. Raw: " + abbrev(raw));
            }
            List<VocabEntry> result = new ArrayList<>();
            for (JsonNode n : arr) {
                result.add(new VocabEntry(
                        n.path("word").asText(""),
                        n.path("definition").asText(""),
                        n.path("exampleSentence").asText("")
                ));
            }
            return result;
        } catch (LlmParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmParseException("Failed to parse vocabularies: " + e.getMessage() + " Raw: " + abbrev(raw));
        }
    }

    private List<QuizEntry> extractQuizzes(String raw) {
        try {
            String json = fixMalformedOptions(cleanJson(raw));
            JsonNode root = objectMapper.readTree(json);
            JsonNode arr = root.get("quizzes");
            if (arr == null || !arr.isArray()) {
                throw new LlmParseException("'quizzes' missing or not array. Raw: " + abbrev(raw));
            }
            List<QuizEntry> result = new ArrayList<>();
            for (JsonNode n : arr) {
                if (n.path("type").isMissingNode() || n.path("question").isMissingNode()) continue;
                List<ChoiceEntry> choices = new ArrayList<>();
                for (JsonNode c : n.path("options").path("choices")) {
                    choices.add(new ChoiceEntry(c.path("key").asText(""), c.path("text").asText(""), c.path("explanation").asText("")));
                }
                result.add(new QuizEntry(
                        n.path("type").asText(""),
                        n.path("question").asText(""),
                        choices,
                        n.path("correctAnswer").asText(""),
                        n.path("explanation").asText("")
                ));
            }
            return result;
        } catch (LlmParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmParseException("Failed to parse quizzes: " + e.getMessage() + " Raw: " + abbrev(raw));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean wordInContent(String baseWord, String contentLower) {
        String w = baseWord.toLowerCase();
        return contentLower.contains(" " + w + " ")
                || contentLower.contains(" " + w + "s ")
                || contentLower.contains(" " + w + "ed ")
                || contentLower.contains(" " + w + "ing ")
                || contentLower.contains(" " + w + "d ")
                || contentLower.contains(" " + w + "ion ")
                || contentLower.contains(" " + w + "er ")
                || contentLower.contains(" " + w + "ly ")
                || contentLower.contains(" " + w + "s,")
                || contentLower.contains(" " + w + "ed,")
                || contentLower.contains(" " + w + "ing,")
                || contentLower.contains(" " + w + ".")
                || contentLower.contains(" " + w + ",")
                || contentLower.startsWith(w + " ")
                || contentLower.contains("\n" + w + " ");
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    private String toVocabJson(List<VocabEntry> vocabs) {
        try {
            List<Map<String, String>> list = vocabs.stream()
                    .map(v -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("word", v.word());
                        m.put("definition", v.definition());
                        m.put("exampleSentence", v.exampleSentence());
                        return m;
                    }).toList();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize vocab", e);
        }
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        // Strip markdown fences
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl != -1) {
                s = s.substring(nl + 1).trim();
                if (s.endsWith("```")) s = s.substring(0, s.length() - 3).trim();
            }
        }
        // Find first { in case of leading prose
        int start = s.indexOf('{');
        return start > 0 ? s.substring(start) : s;
    }

    private String fixMalformedOptions(String json) {
        // Replaces {"A","B",...} style options (no colons) with {} to avoid parse errors
        Matcher m = Pattern.compile("\"options\"\\s*:\\s*\\{([^{}]*)\\}", Pattern.DOTALL).matcher(json);
        return m.replaceAll(mr -> mr.group(1).contains(":") ? mr.group() : "\"options\": {}");
    }

    private String abbrev(String s) {
        if (s == null) return "null";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    // ── Schema builders ────────────────────────────────────────────────────────

    private static Map<String, Object> buildStep1Schema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("content", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("content"));
        schema.put("properties", props);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> buildStep2Schema() {
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

    private static Map<String, Object> buildStep3Schema() {
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

    // ── Local DTOs ─────────────────────────────────────────────────────────────

    private record VocabEntry(String word, String definition, String exampleSentence) {}
    private record ChoiceEntry(String key, String text, String explanation) {}
    private record QuizEntry(String type, String question, List<ChoiceEntry> choices, String correctAnswer, String explanation) {}
}
