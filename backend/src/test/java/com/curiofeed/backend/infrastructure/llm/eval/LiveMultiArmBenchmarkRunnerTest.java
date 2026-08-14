package com.curiofeed.backend.infrastructure.llm.eval;

import com.curiofeed.backend.config.GeminiProperties;
import com.curiofeed.backend.config.MistralProperties;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.infrastructure.llm.*;
import com.curiofeed.backend.infrastructure.llm.validation.ContentStepValidator;
import com.curiofeed.backend.infrastructure.llm.validation.ContentValidationResult;
import com.curiofeed.backend.infrastructure.llm.validation.QuizStepValidator;
import com.curiofeed.backend.infrastructure.llm.validation.VocabLemmatizer;
import com.curiofeed.backend.infrastructure.llm.validation.VocabStepValidator;
import com.curiofeed.backend.domain.service.SemanticEvaluatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("mistral")
class LiveMultiArmBenchmarkRunnerTest {

    private static final Logger log = LoggerFactory.getLogger(LiveMultiArmBenchmarkRunnerTest.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QualityScorer qualityScorer = new QualityScorer();
    private final DefaultLlmResponseParser parser = new DefaultLlmResponseParser(objectMapper);
    private final ThreeStepPromptBuilder promptBuilder = new ThreeStepPromptBuilder();
    private final EvalPromptBuilder evalPromptBuilder = new EvalPromptBuilder();

    private final ContentStepValidator contentValidator = new ContentStepValidator();
    private final VocabLemmatizer lemmatizer = new VocabLemmatizer();
    private final VocabStepValidator vocabValidator = new VocabStepValidator(lemmatizer);
    private final QuizStepValidator quizValidator = new QuizStepValidator();

    public record PersistedEvalRecord(
            String articleId,
            String generatorModel,
            String evaluatorModel,
            double heuristicScore,
            double factualAccuracy,
            double levelAppropriateness,
            double engagement,
            double safety,
            double overallScore
    ) {}

    @Test
    @DisplayName("Execute Empirical Multi-Arm Benchmark on Golden Dataset (N=20) with Independent LLM Judge")
    void runLiveMultiArmBenchmark() throws Exception {
        String mistralApiKey = requireApiKey("MISTRAL_API_KEY");
        String geminiApiKey = requireApiKey("GEMINI_API_KEY");

        // Setup Model Arms
        MistralProperties mistralProps = new MistralProperties(mistralApiKey, "mistral-small-2603", "ministral-8b-2410", "https://api.mistral.ai", 10, 120, 0.3);
        MistralLlmClient mistralClient = new MistralLlmClient(mistralProps, "mistral-small-2603", RestClient.builder());

        GeminiProperties geminiProps = new GeminiProperties(geminiApiKey, "gemini-3.5-flash-lite", "gemini-3.5-flash-lite", 10, 120, 0.3);
        GeminiLlmClient geminiClient = new GeminiLlmClient(geminiProps, "gemini-3.5-flash-lite", RestClient.builder());

        // Independent LLM-as-Judge Client: Gemini 3.5 Flash Lite evaluates Mistral & Gemini outputs
        LlmClient judgeClient = geminiClient;
        String judgeModelName = judgeClient.getModelName();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] articles = resolver.getResources("classpath:golden_dataset/source_articles/*.json");
        assertThat(articles).hasSizeGreaterThanOrEqualTo(20);

        List<GoldenArticle> goldenArticles = new ArrayList<>();
        for (Resource res : articles) {
            try (InputStream is = res.getInputStream()) {
                goldenArticles.add(objectMapper.readValue(is, GoldenArticle.class));
            }
        }

        int totalN = goldenArticles.size();
        List<PersistedEvalRecord> persistedEvalScores = new ArrayList<>();

        // ── Arm C: Mistral Small 4 Metrics ──────────────────────────────────
        List<Double> mistralHeuristics = new ArrayList<>();
        List<Double> mistralJudgesForCorrelation = new ArrayList<>();
        List<Double> mistralHeuristicsForCorrelation = new ArrayList<>();
        Set<Double> mistralDistinctJudgeScores = new HashSet<>();
        int mistralHardFailEvents = 0;       // Step-level hard validation failures
        int mistralCleanArticles = 0;        // Articles with zero hard failures across 3 steps
        int mistralNetworkCompleted = 0;     // Articles completing full 3-step generation
        int mistralJudgeNetworkFailures = 0; // Judge HTTP/Quota errors
        int mistralJudgeParsingFailures = 0; // Judge JSON parsing errors
        int[] mistralStepHardFails = new int[3];                  // [0]=content, [1]=vocab, [2]=quiz
        Map<String, Integer> mistralFailReasons = new LinkedHashMap<>();

        // ── Arm A: Gemini 3.5 Flash Lite Metrics ─────────────────────────────
        List<Double> geminiHeuristics = new ArrayList<>();
        List<Double> geminiJudgesForCorrelation = new ArrayList<>();
        List<Double> geminiHeuristicsForCorrelation = new ArrayList<>();
        Set<Double> geminiDistinctJudgeScores = new HashSet<>();
        int geminiHardFailEvents = 0;
        int geminiCleanArticles = 0;
        int geminiNetworkCompleted = 0;
        int geminiJudgeNetworkFailures = 0;
        int geminiJudgeParsingFailures = 0;
        int[] geminiStepHardFails = new int[3];
        Map<String, Integer> geminiFailReasons = new LinkedHashMap<>();

        log.info("Starting Empirical Multi-Arm Benchmark on N={} articles. Independent Judge: {}", totalN, judgeModelName);

        for (int i = 0; i < totalN; i++) {
            GoldenArticle article = goldenArticles.get(i);
            DifficultyLevel level = DifficultyLevel.EASY;

            // ═════════════════════════════════════════════════════════════════
            // 1. Arm C: Mistral Small 4 (Full 3-Step Pipeline)
            // ═════════════════════════════════════════════════════════════════
            try {
                boolean articleClean = true;

                // Step 1: Content
                String step1Prompt = promptBuilder.buildContentPrompt(article.originalContent(), level, false);
                String step1Raw = mistralClient.generate(step1Prompt, ThreeStepPromptBuilder.contentSchema());
                GenerationResult step1Result = parser.parse(step1Raw, GenerationResult.class);
                String content = (step1Result != null && step1Result.content() != null) ? step1Result.content() : step1Raw;

                ContentValidationResult contentVal = contentValidator.validate(content, level);
                if (contentVal.isHardFail()) {
                    mistralHardFailEvents++;
                    mistralStepHardFails[0]++;
                    tally(mistralFailReasons, "STEP1 content " + contentVal.getStatus());
                    log.warn("[Arm C - Mistral] {} STEP1 HARD FAIL: {} (words={}, hardRange={}..{})",
                            article.id(), contentVal.getStatus(), contentVal.getActualWordCount(),
                            contentVal.getHardMin(), contentVal.getHardMax());
                    articleClean = false;
                }

                // Step 2: Vocabulary
                String step2Prompt = promptBuilder.buildVocabularyPrompt(content, level);
                String step2Raw = mistralClient.generate(step2Prompt, ThreeStepPromptBuilder.vocabularySchema());
                GenerationResult step2Result = parser.parse(step2Raw, GenerationResult.class);
                List<GenerationResult.VocabularyData> vocabs = (step2Result != null && step2Result.vocabularies() != null) ? step2Result.vocabularies() : List.of();

                List<String> vocabErrors = vocabValidator.validate(vocabs, content);
                List<String> vocabHardErrors = vocabErrors.stream().filter(e -> !e.startsWith("[SOFT]")).toList();
                if (!vocabHardErrors.isEmpty()) {
                    mistralHardFailEvents++;
                    mistralStepHardFails[1]++;
                    vocabHardErrors.forEach(e -> tally(mistralFailReasons, "STEP2 vocab " + normalizeReason(e)));
                    log.warn("[Arm C - Mistral] {} STEP2 HARD FAIL: {}", article.id(), vocabHardErrors);
                    articleClean = false;
                }

                // Step 3: Quiz
                String step3Prompt = promptBuilder.buildQuizPrompt(content, step2Raw, level);
                String step3Raw = mistralClient.generate(step3Prompt, ThreeStepPromptBuilder.quizSchema());
                GenerationResult step3Result = parser.parse(step3Raw, GenerationResult.class);
                List<GenerationResult.QuizData> quizzes = (step3Result != null && step3Result.quizzes() != null) ? step3Result.quizzes() : List.of();

                List<String> quizErrors = quizValidator.validate(quizzes, vocabs);
                List<String> quizHardErrors = quizErrors.stream().filter(e -> !e.startsWith("[SOFT]")).toList();
                if (!quizHardErrors.isEmpty()) {
                    mistralHardFailEvents++;
                    mistralStepHardFails[2]++;
                    quizHardErrors.forEach(e -> tally(mistralFailReasons, "STEP3 quiz " + normalizeReason(e)));
                    log.warn("[Arm C - Mistral] {} STEP3 HARD FAIL: {}", article.id(), quizHardErrors);
                    articleClean = false;
                }

                if (articleClean) mistralCleanArticles++;
                mistralNetworkCompleted++;

                // Full Heuristic Quality Score (Added ALWAYS for N=20)
                GenerationResult fullResult = new GenerationResult(content, List.of(), vocabs, quizzes, null);
                double hScore = qualityScorer.score(fullResult);
                mistralHeuristics.add(hScore);

                // Dedicated Inner Try-Catch for LLM-as-Judge (Gemini 3.5 Flash Lite evaluating Mistral)
                try {
                    String judgePrompt = evalPromptBuilder.buildEvaluationPrompt(article.originalContent(), content, level);
                    String judgeRaw = judgeClient.generate(judgePrompt, EvalPromptBuilder.evalSchema());
                    SemanticEvaluatorService.JudgeResult jResult = parser.parse(judgeRaw, SemanticEvaluatorService.JudgeResult.class);

                    if (jResult != null) {
                        double jOverall = jResult.overall();
                        mistralJudgesForCorrelation.add(jOverall);
                        mistralHeuristicsForCorrelation.add(hScore);
                        mistralDistinctJudgeScores.add(jOverall);

                        persistedEvalScores.add(new PersistedEvalRecord(
                                article.id(),
                                mistralClient.getModelName(),
                                judgeModelName,
                                hScore,
                                jResult.factualAccuracy(),
                                jResult.levelAppropriateness(),
                                jResult.engagement(),
                                jResult.safety(),
                                jOverall
                        ));

                        log.info("[Arm C - Mistral] Article {} | Heuristic: {} | Judge ({}): {} [Fact: {}, Level: {}, Eng: {}, Safe: {}]",
                                article.id(), String.format("%.4f", hScore), judgeModelName, String.format("%.4f", jOverall),
                                String.format("%.2f", jResult.factualAccuracy()), String.format("%.2f", jResult.levelAppropriateness()),
                                String.format("%.2f", jResult.engagement()), String.format("%.2f", jResult.safety()));
                    } else {
                        mistralJudgeParsingFailures++;
                        log.warn("[Arm C - Mistral] Article {} Judge JSON parsing failed", article.id());
                    }
                } catch (Exception je) {
                    mistralJudgeNetworkFailures++;
                    log.warn("[Arm C - Mistral] Article {} Judge network call failed (429/quota): {}", article.id(), je.getMessage());
                }

            } catch (Exception e) {
                log.warn("[Arm C - Mistral] Article {} network generation failed: {}", article.id(), e.getMessage());
            }

            // ═════════════════════════════════════════════════════════════════
            // 2. Arm A: Gemini 3.5 Flash Lite (Full 3-Step Pipeline)
            // ═════════════════════════════════════════════════════════════════
            try {
                boolean articleClean = true;

                // Step 1: Content
                String step1Prompt = promptBuilder.buildContentPrompt(article.originalContent(), level, false);
                String step1Raw = geminiClient.generate(step1Prompt, ThreeStepPromptBuilder.contentSchema());
                GenerationResult step1Result = parser.parse(step1Raw, GenerationResult.class);
                String content = (step1Result != null && step1Result.content() != null) ? step1Result.content() : step1Raw;

                ContentValidationResult contentVal = contentValidator.validate(content, level);
                if (contentVal.isHardFail()) {
                    geminiHardFailEvents++;
                    geminiStepHardFails[0]++;
                    tally(geminiFailReasons, "STEP1 content " + contentVal.getStatus());
                    log.warn("[Arm A - Gemini] {} STEP1 HARD FAIL: {} (words={}, hardRange={}..{})",
                            article.id(), contentVal.getStatus(), contentVal.getActualWordCount(),
                            contentVal.getHardMin(), contentVal.getHardMax());
                    articleClean = false;
                }

                // Step 2: Vocabulary
                String step2Prompt = promptBuilder.buildVocabularyPrompt(content, level);
                String step2Raw = geminiClient.generate(step2Prompt, ThreeStepPromptBuilder.vocabularySchema());
                GenerationResult step2Result = parser.parse(step2Raw, GenerationResult.class);
                List<GenerationResult.VocabularyData> vocabs = (step2Result != null && step2Result.vocabularies() != null) ? step2Result.vocabularies() : List.of();

                List<String> vocabErrors = vocabValidator.validate(vocabs, content);
                List<String> vocabHardErrors = vocabErrors.stream().filter(e -> !e.startsWith("[SOFT]")).toList();
                if (!vocabHardErrors.isEmpty()) {
                    geminiHardFailEvents++;
                    geminiStepHardFails[1]++;
                    vocabHardErrors.forEach(e -> tally(geminiFailReasons, "STEP2 vocab " + normalizeReason(e)));
                    log.warn("[Arm A - Gemini] {} STEP2 HARD FAIL: {}", article.id(), vocabHardErrors);
                    articleClean = false;
                }

                // Step 3: Quiz
                String step3Prompt = promptBuilder.buildQuizPrompt(content, step2Raw, level);
                String step3Raw = geminiClient.generate(step3Prompt, ThreeStepPromptBuilder.quizSchema());
                GenerationResult step3Result = parser.parse(step3Raw, GenerationResult.class);
                List<GenerationResult.QuizData> quizzes = (step3Result != null && step3Result.quizzes() != null) ? step3Result.quizzes() : List.of();

                List<String> quizErrors = quizValidator.validate(quizzes, vocabs);
                List<String> quizHardErrors = quizErrors.stream().filter(e -> !e.startsWith("[SOFT]")).toList();
                if (!quizHardErrors.isEmpty()) {
                    geminiHardFailEvents++;
                    geminiStepHardFails[2]++;
                    quizHardErrors.forEach(e -> tally(geminiFailReasons, "STEP3 quiz " + normalizeReason(e)));
                    log.warn("[Arm A - Gemini] {} STEP3 HARD FAIL: {}", article.id(), quizHardErrors);
                    articleClean = false;
                }

                if (articleClean) geminiCleanArticles++;
                geminiNetworkCompleted++;

                // Full Heuristic Quality Score (Added ALWAYS)
                GenerationResult fullResult = new GenerationResult(content, List.of(), vocabs, quizzes, null);
                double hScore = qualityScorer.score(fullResult);
                geminiHeuristics.add(hScore);

                // Dedicated Inner Try-Catch for LLM-as-Judge (Gemini evaluating Gemini)
                try {
                    String judgePrompt = evalPromptBuilder.buildEvaluationPrompt(article.originalContent(), content, level);
                    String judgeRaw = judgeClient.generate(judgePrompt, EvalPromptBuilder.evalSchema());
                    SemanticEvaluatorService.JudgeResult jResult = parser.parse(judgeRaw, SemanticEvaluatorService.JudgeResult.class);

                    if (jResult != null) {
                        double jOverall = jResult.overall();
                        geminiJudgesForCorrelation.add(jOverall);
                        geminiHeuristicsForCorrelation.add(hScore);
                        geminiDistinctJudgeScores.add(jOverall);

                        persistedEvalScores.add(new PersistedEvalRecord(
                                article.id(),
                                geminiClient.getModelName(),
                                judgeModelName,
                                hScore,
                                jResult.factualAccuracy(),
                                jResult.levelAppropriateness(),
                                jResult.engagement(),
                                jResult.safety(),
                                jOverall
                        ));

                        log.info("[Arm A - Gemini] Article {} | Heuristic: {} | Judge ({}): {}",
                                article.id(), String.format("%.4f", hScore), judgeModelName, String.format("%.4f", jOverall));
                    } else {
                        geminiJudgeParsingFailures++;
                    }
                } catch (Exception je) {
                    geminiJudgeNetworkFailures++;
                }

            } catch (Exception e) {
                log.warn("[Arm A - Gemini] Article {} network generation failed: {}", article.id(), e.getMessage());
            }
        }

        // ── Statistical & Metric Summaries (Mathematically Correct Denominators) ──
        double[] mistralHAll = mistralHeuristics.stream().mapToDouble(Double::doubleValue).toArray();
        double[] mistralHCorr = mistralHeuristicsForCorrelation.stream().mapToDouble(Double::doubleValue).toArray();
        double[] mistralJCorr = mistralJudgesForCorrelation.stream().mapToDouble(Double::doubleValue).toArray();
        double rsMistral = (mistralHCorr.length >= 2) ? SpearmanCorrelationCalculator.calculate(mistralHCorr, mistralJCorr) : 0.0;

        double[] geminiHAll = geminiHeuristics.stream().mapToDouble(Double::doubleValue).toArray();
        double[] geminiHCorr = geminiHeuristicsForCorrelation.stream().mapToDouble(Double::doubleValue).toArray();
        double[] geminiJCorr = geminiJudgesForCorrelation.stream().mapToDouble(Double::doubleValue).toArray();
        double rsGemini = (geminiHCorr.length >= 2) ? SpearmanCorrelationCalculator.calculate(geminiHCorr, geminiJCorr) : 0.0;

        int mistralTotalSteps = mistralNetworkCompleted * 3;
        int geminiTotalSteps = geminiNetworkCompleted * 3;

        System.out.println("\n====================================================================================================");
        System.out.println("            [EMPIRICAL MULTI-ARM BENCHMARK & SPEARMAN CORRELATION RESULTS (N=" + totalN + ")]");
        System.out.println("====================================================================================================");
        System.out.println(" LLM-as-Judge Model      : " + judgeModelName + " (independent for Arm C / Mistral; SELF-EVALUATION for Arm A / Gemini)");
        System.out.println(" Total Golden Articles   : " + totalN + " Golden Dataset Articles");
        System.out.println(" EvalScores Persisted    : " + persistedEvalScores.size() + " records registered for /api/admin/ab-compare");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Metric Category", "Arm C: Mistral Small 4", "Arm A: Gemini 3.5 Flash Lite");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Model ID", mistralClient.getModelName(), geminiClient.getModelName());
        System.out.printf( " %-28s | %-32s | %-32s\n", "Completed Network Runs", mistralNetworkCompleted + " / " + totalN, geminiNetworkCompleted + " / " + totalN);
        System.out.printf( " %-28s | %-32s | %-32s\n", "Clean Articles (No Hard Fail)", mistralCleanArticles + " / " + mistralNetworkCompleted + " (" + formatPct(mistralCleanArticles, mistralNetworkCompleted) + ")", geminiCleanArticles + " / " + geminiNetworkCompleted + " (" + formatPct(geminiCleanArticles, geminiNetworkCompleted) + ")");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Step Hard Gate Failures", mistralHardFailEvents + " / " + mistralTotalSteps + " steps (" + formatPct(mistralHardFailEvents, mistralTotalSteps) + ")", geminiHardFailEvents + " / " + geminiTotalSteps + " steps (" + formatPct(geminiHardFailEvents, geminiTotalSteps) + ")");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Avg Heuristic Quality Score", String.format("%.4f (n=%d)", average(mistralHAll), mistralHAll.length), String.format("%.4f (n=%d)", average(geminiHAll), geminiHAll.length));
        System.out.printf( " %-28s | %-32s | %-32s\n", "Avg LLM Judge Overall Score", String.format("%.4f (n=%d)", average(mistralJCorr), mistralJCorr.length), String.format("%.4f (n=%d)", average(geminiJCorr), geminiJCorr.length));
        System.out.printf( " %-28s | %-32s | %-32s\n", "Judge Distinct Scores Count", mistralDistinctJudgeScores.size() + " distinct values", geminiDistinctJudgeScores.size() + " distinct values");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Judge Network Failures (429)", mistralJudgeNetworkFailures + " calls", geminiJudgeNetworkFailures + " calls");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Judge JSON Parse Failures", mistralJudgeParsingFailures + " calls", geminiJudgeParsingFailures + " calls");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Spearman Rank Correlation (r_s)", String.format("%.4f (n_judge=%d)", rsMistral, mistralJCorr.length), String.format("%.4f (n_judge=%d)", rsGemini, geminiJCorr.length));
        System.out.printf( " %-28s | %-32s | %-32s\n", "Hard Fails by Step (S1/S2/S3)",
                mistralStepHardFails[0] + " / " + mistralStepHardFails[1] + " / " + mistralStepHardFails[2],
                geminiStepHardFails[0] + " / " + geminiStepHardFails[1] + " / " + geminiStepHardFails[2]);
        System.out.println("----------------------------------------------------------------------------------------------------");
        printFailReasons("Arm C: Mistral Small 4 — hard gate failure reasons", mistralFailReasons);
        printFailReasons("Arm A: Gemini — hard gate failure reasons", geminiFailReasons);
        System.out.println("====================================================================================================\n");

        assertThat(totalN).isEqualTo(20);
        assertThat(mistralNetworkCompleted).isGreaterThan(0);
        assertThat(geminiNetworkCompleted).isGreaterThan(0);
    }

    private static void tally(Map<String, Integer> counts, String reason) {
        counts.merge(reason, 1, Integer::sum);
    }

    /** Strips article-specific detail so failures of the same kind aggregate into one bucket. */
    private static String normalizeReason(String rawError) {
        String reason = rawError.replaceAll("\\[\\d+\\]", "[i]").replaceAll("got \\d+", "got N");
        int colon = reason.indexOf(':');
        return (colon > 0 ? reason.substring(0, colon) : reason).trim();
    }

    private void printFailReasons(String title, Map<String, Integer> counts) {
        System.out.println(" " + title + (counts.isEmpty() ? " — none" : ""));
        counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> System.out.printf("   %-4d x  %s\n", e.getValue(), e.getKey()));
    }

    /** Reads a live API key from the environment — the same source application.yml resolves. Never hardcode keys here. */
    private static String requireApiKey(String envVar) {
        String value = System.getenv(envVar);
        Assumptions.assumeTrue(value != null && !value.isBlank(),
                envVar + " is not set — skipping live benchmark. Export it before running ./gradlew multiArmBenchmark.");
        return value;
    }

    private String formatPct(int num, int denom) {
        if (denom == 0) return "0.0%";
        return String.format("%.1f%%", ((double) num / denom) * 100.0);
    }

    private double average(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }
}
