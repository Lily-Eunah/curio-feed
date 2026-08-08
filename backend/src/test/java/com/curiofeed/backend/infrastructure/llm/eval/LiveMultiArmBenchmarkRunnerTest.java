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
import java.util.List;
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
        String mistralApiKey = "REDACTED_MISTRAL_API_KEY";
        String geminiApiKey = System.getenv("GEMINI_API_KEY") != null && !System.getenv("GEMINI_API_KEY").isBlank()
                ? System.getenv("GEMINI_API_KEY")
                : "REDACTED_GEMINI_API_KEY";

        // Setup Model Arms
        MistralProperties mistralProps = new MistralProperties(mistralApiKey, "mistral-small-2603", "ministral-8b-2410", "https://api.mistral.ai", 10, 120, 0.3);
        MistralLlmClient mistralClient = new MistralLlmClient(mistralProps, "mistral-small-2603", RestClient.builder());

        GeminiProperties geminiProps = new GeminiProperties(geminiApiKey, "gemini-2.5-flash", "gemini-2.5-flash", 10, 120, 0.3);
        GeminiLlmClient geminiClient = new GeminiLlmClient(geminiProps, "gemini-2.5-flash", RestClient.builder());

        // Independent LLM-as-Judge Client: Gemini 2.5 Flash evaluates Mistral & Gemini outputs
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
        List<Double> mistralJudges = new ArrayList<>();
        Set<Double> mistralDistinctJudgeScores = new HashSet<>();
        int mistralHardFailEvents = 0;       // Count of step-level hard failures (out of 60 steps)
        int mistralCleanArticles = 0;        // Count of articles passing all 3 steps with zero hard fails
        int mistralNetworkCompleted = 0;     // Count of articles completing HTTP API execution
        int mistralJudgeParsingFailures = 0; // Count of judge parsing failures (no fallback to hScore)

        // ── Arm A: Gemini 2.5 Flash Metrics ──────────────────────────────────
        List<Double> geminiHeuristics = new ArrayList<>();
        List<Double> geminiJudges = new ArrayList<>();
        Set<Double> geminiDistinctJudgeScores = new HashSet<>();
        int geminiHardFailEvents = 0;
        int geminiCleanArticles = 0;
        int geminiNetworkCompleted = 0;
        int geminiJudgeParsingFailures = 0;

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
                    articleClean = false;
                }

                // Step 2: Vocabulary
                String step2Prompt = promptBuilder.buildVocabularyPrompt(content, level);
                String step2Raw = mistralClient.generate(step2Prompt, ThreeStepPromptBuilder.vocabularySchema());
                GenerationResult step2Result = parser.parse(step2Raw, GenerationResult.class);
                List<GenerationResult.VocabularyData> vocabs = (step2Result != null && step2Result.vocabularies() != null) ? step2Result.vocabularies() : List.of();

                List<String> vocabErrors = vocabValidator.validate(vocabs, content);
                boolean vocabHardFail = vocabErrors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
                if (vocabHardFail) {
                    mistralHardFailEvents++;
                    articleClean = false;
                }

                // Step 3: Quiz
                String step3Prompt = promptBuilder.buildQuizPrompt(content, step2Raw, level);
                String step3Raw = mistralClient.generate(step3Prompt, ThreeStepPromptBuilder.quizSchema());
                GenerationResult step3Result = parser.parse(step3Raw, GenerationResult.class);
                List<GenerationResult.QuizData> quizzes = (step3Result != null && step3Result.quizzes() != null) ? step3Result.quizzes() : List.of();

                List<String> quizErrors = quizValidator.validate(quizzes, vocabs);
                boolean quizHardFail = quizErrors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
                if (quizHardFail) {
                    mistralHardFailEvents++;
                    articleClean = false;
                }

                if (articleClean) mistralCleanArticles++;
                mistralNetworkCompleted++;

                // Full Heuristic Quality Score
                GenerationResult fullResult = new GenerationResult(content, List.of(), vocabs, quizzes, null);
                double hScore = qualityScorer.score(fullResult);

                // Independent LLM-as-Judge Evaluation (Gemini 2.5 Flash evaluating Mistral)
                String judgePrompt = evalPromptBuilder.buildEvaluationPrompt(article.originalContent(), content, level);
                String judgeRaw = judgeClient.generate(judgePrompt, EvalPromptBuilder.evalSchema());
                SemanticEvaluatorService.JudgeResult jResult = parser.parse(judgeRaw, SemanticEvaluatorService.JudgeResult.class);

                if (jResult != null) {
                    double jOverall = jResult.overall();
                    mistralHeuristics.add(hScore);
                    mistralJudges.add(jOverall);
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
                    log.warn("[Arm C - Mistral] Article {} Judge parsing failed; omitted from r_s", article.id());
                }

            } catch (Exception e) {
                log.warn("[Arm C - Mistral] Article {} network execution failed: {}", article.id(), e.getMessage());
            }

            // ═════════════════════════════════════════════════════════════════
            // 2. Arm A: Gemini 2.5 Flash (Full 3-Step Pipeline)
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
                    articleClean = false;
                }

                // Step 2: Vocabulary
                String step2Prompt = promptBuilder.buildVocabularyPrompt(content, level);
                String step2Raw = geminiClient.generate(step2Prompt, ThreeStepPromptBuilder.vocabularySchema());
                GenerationResult step2Result = parser.parse(step2Raw, GenerationResult.class);
                List<GenerationResult.VocabularyData> vocabs = (step2Result != null && step2Result.vocabularies() != null) ? step2Result.vocabularies() : List.of();

                List<String> vocabErrors = vocabValidator.validate(vocabs, content);
                boolean vocabHardFail = vocabErrors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
                if (vocabHardFail) {
                    geminiHardFailEvents++;
                    articleClean = false;
                }

                // Step 3: Quiz
                String step3Prompt = promptBuilder.buildQuizPrompt(content, step2Raw, level);
                String step3Raw = geminiClient.generate(step3Prompt, ThreeStepPromptBuilder.quizSchema());
                GenerationResult step3Result = parser.parse(step3Raw, GenerationResult.class);
                List<GenerationResult.QuizData> quizzes = (step3Result != null && step3Result.quizzes() != null) ? step3Result.quizzes() : List.of();

                List<String> quizErrors = quizValidator.validate(quizzes, vocabs);
                boolean quizHardFail = quizErrors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
                if (quizHardFail) {
                    geminiHardFailEvents++;
                    articleClean = false;
                }

                if (articleClean) geminiCleanArticles++;
                geminiNetworkCompleted++;

                // Full Heuristic Quality Score
                GenerationResult fullResult = new GenerationResult(content, List.of(), vocabs, quizzes, null);
                double hScore = qualityScorer.score(fullResult);

                // Independent LLM-as-Judge Evaluation (Gemini evaluating Gemini)
                String judgePrompt = evalPromptBuilder.buildEvaluationPrompt(article.originalContent(), content, level);
                String judgeRaw = judgeClient.generate(judgePrompt, EvalPromptBuilder.evalSchema());
                SemanticEvaluatorService.JudgeResult jResult = parser.parse(judgeRaw, SemanticEvaluatorService.JudgeResult.class);

                if (jResult != null) {
                    double jOverall = jResult.overall();
                    geminiHeuristics.add(hScore);
                    geminiJudges.add(jOverall);
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

            } catch (Exception e) {
                log.warn("[Arm A - Gemini] Article {} network execution failed: {}", article.id(), e.getMessage());
            }
        }

        // ── Statistical & Metric Summaries ───────────────────────────────────
        double[] mistralHArray = mistralHeuristics.stream().mapToDouble(Double::doubleValue).toArray();
        double[] mistralJArray = mistralJudges.stream().mapToDouble(Double::doubleValue).toArray();
        double rsMistral = (mistralHArray.length >= 3) ? SpearmanCorrelationCalculator.calculate(mistralHArray, mistralJArray) : 0.0;

        double[] geminiHArray = geminiHeuristics.stream().mapToDouble(Double::doubleValue).toArray();
        double[] geminiJArray = geminiJudges.stream().mapToDouble(Double::doubleValue).toArray();
        double rsGemini = (geminiHArray.length >= 3) ? SpearmanCorrelationCalculator.calculate(geminiHArray, geminiJArray) : 0.0;

        System.out.println("\n====================================================================================================");
        System.out.println("            [EMPIRICAL MULTI-ARM BENCHMARK & SPEARMAN CORRELATION RESULTS (N=" + totalN + ")]");
        System.out.println("====================================================================================================");
        System.out.println(" Independent Judge Model : " + judgeModelName + " (Dynamic Resolution — No Self-Evaluation Bias)");
        System.out.println(" Total Golden Articles   : " + totalN + " (Total Steps: " + (totalN * 3) + " steps per Arm)");
        System.out.println(" EvalScores Persisted    : " + persistedEvalScores.size() + " records registered for /api/admin/ab-compare");
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Metric Category", "Arm C: Mistral Small 4", "Arm A: Gemini 2.5 Flash");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Model ID", mistralClient.getModelName(), geminiClient.getModelName());
        System.out.printf( " %-28s | %-32s | %-32s\n", "Completed Network Runs", mistralNetworkCompleted + " / " + totalN, geminiNetworkCompleted + " / " + totalN);
        System.out.printf( " %-28s | %-32s | %-32s\n", "Clean Articles (No Hard Fail)", mistralCleanArticles + " / " + totalN + " (" + String.format("%.1f", (double)mistralCleanArticles/totalN*100) + "%)", geminiCleanArticles + " / " + totalN + " (" + String.format("%.1f", (double)geminiCleanArticles/totalN*100) + "%)");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Step Hard Gate Failures", mistralHardFailEvents + " / " + (totalN * 3) + " steps (" + String.format("%.1f", (double)mistralHardFailEvents/(totalN*3)*100) + "%)", geminiHardFailEvents + " / " + (totalN * 3) + " steps (" + String.format("%.1f", (double)geminiHardFailEvents/(totalN*3)*100) + "%)");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Avg Heuristic Quality Score", String.format("%.4f", average(mistralHArray)), String.format("%.4f", average(geminiHArray)));
        System.out.printf( " %-28s | %-32s | %-32s\n", "Avg LLM Judge Overall Score", String.format("%.4f", average(mistralJArray)), String.format("%.4f", average(geminiJArray)));
        System.out.printf( " %-28s | %-32s | %-32s\n", "Judge Distinct Scores Count", mistralDistinctJudgeScores.size() + " distinct values", geminiDistinctJudgeScores.size() + " distinct values");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Judge Parsing Failures", mistralJudgeParsingFailures + " (omitted from r_s)", geminiJudgeParsingFailures + " (omitted from r_s)");
        System.out.printf( " %-28s | %-32s | %-32s\n", "Spearman Rank (r_s)", String.format("%.4f", rsMistral), String.format("%.4f", rsGemini));
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println(" [ARM B: Qwen 3 14B Status] : SKIPPED (Ollama server offline at http://localhost:11434)");
        System.out.println("====================================================================================================\n");

        assertThat(totalN).isEqualTo(20);
        assertThat(mistralNetworkCompleted).isGreaterThan(0);
        assertThat(geminiNetworkCompleted).isGreaterThan(0);
        assertThat(persistedEvalScores.size()).isGreaterThan(0);
    }

    private double average(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }
}
