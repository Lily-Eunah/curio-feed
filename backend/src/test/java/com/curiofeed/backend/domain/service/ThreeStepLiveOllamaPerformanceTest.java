package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live diagnostic/performance test for the 3-step Ollama pipeline.
 *
 * This test intentionally calls a real Ollama server and is disabled by default.
 *
 * PowerShell:
 *   $env:RUN_LIVE_OLLAMA_TESTS="true"
 *   $env:OLLAMA_BASE_URL="http://192.168.45.100:11434"
 *   ./gradlew --no-daemon test --tests ThreeStepLiveOllamaPerformanceTest -i
 *
 * Purpose:
 *   - Verify live Ollama connectivity.
 *   - Measure SOURCE_DIGEST / CONTENT / VOCABULARY / QUIZ durations.
 *   - Confirm long articles enter the SOURCE_DIGEST path.
 *   - Confirm jobs always reach a terminal state, not PROCESSING forever.
 *   - Expose validation/failure reasons clearly when generation fails.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("ollama")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_OLLAMA_TESTS", matches = "true")
class ThreeStepLiveOllamaPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ThreeStepLiveOllamaPerformanceTest.class);

    private static final Path LONG_ARTICLE_FIXTURE =
            Path.of("src/test/resources/fixtures/articles/vande_bharat_long_article.txt");

    private static final Path SHORT_ARTICLE_FIXTURE =
            Path.of("src/test/resources/fixtures/articles/short_ai_tools_article.txt");

    private static final int LONG_ARTICLE_THRESHOLD_WORDS = 600;

    @Autowired
    private Environment environment;

    @Autowired
    private ThreeStepSubJobWorker worker;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleGenerationJobRepository jobRepository;

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

    @Autowired
    private ArticleGenerationStepJobRepository stepJobRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ArticleContentRepository contentRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        String uniqueName = "LiveTest-" + UUID.randomUUID();
        category = categoryRepository.saveAndFlush(new Category(uniqueName, uniqueName, 1, true));
    }

    /**
     * Smoke test:
     * - Uses a realistic short article.
     * - SOURCE_DIGEST should be SKIPPED.
     * - EASY generation is expected to complete.
     */
    @Test
    @Timeout(value = 12, unit = TimeUnit.MINUTES)
    void measurePerformance_shortArticle_EasyLevel_completes() throws Exception {
        String originalContent = readFixtureOrFallback(SHORT_ARTICLE_FIXTURE, shortAiToolsFallback());
        int originalWordCount = countWords(originalContent);

        Article article = createArticle("Short AI Tools " + UUID.randomUUID(), originalContent);
        ArticleGenerationSubJob subJob = createSubJob(article, DifficultyLevel.EASY);

        logLiveEnvironment("shortArticle_EasyLevel", DifficultyLevel.EASY, originalWordCount);
        assertThat(originalWordCount)
                .as("short article fixture should stay below or equal to digest threshold")
                .isLessThanOrEqualTo(LONG_ARTICLE_THRESHOLD_WORDS);

        Instant start = Instant.now();

        worker.process(subJob.getId());
        waitForTerminalStatus(subJob.getId(), List.of(JobStatus.COMPLETED, JobStatus.FAILED), 300);

        Instant end = Instant.now();
        log.info("[performance-test] shortArticle_EasyLevel totalDurationMs={}",
                end.toEpochMilli() - start.toEpochMilli());

        PipelineReport report = verifyAndReport(
                subJob.getId(),
                DifficultyLevel.EASY,
                article.getId(),
                VerificationMode.REQUIRE_COMPLETED
        );

        ArticleGenerationStepJob digestStep = report.findStep(GenerationStepType.SOURCE_DIGEST)
                .orElseThrow(() -> new AssertionError("SOURCE_DIGEST step should exist even for short articles"));

        assertThat(digestStep.getStatus())
                .as("SOURCE_DIGEST should be skipped for short articles")
                .isEqualTo(JobStatus.SKIPPED);

        String skipDetail = diagnosticText(digestStep);
        assertThat(skipDetail)
                .as("Skipped digest step should expose SHORT_ARTICLE reason when available")
                .containsIgnoringCase("SHORT");
    }

    /**
     * Diagnostic test:
     * - Uses a realistic long news-style article.
     * - SOURCE_DIGEST must run.
     * - HARD content should use the digest path.
     * - COMPLETED and FAILED are both acceptable terminal states.
     * - PROCESSING forever is not acceptable.
     * - If FAILED, an actionable failure reason must be present.
     */
    @Test
    @Timeout(value = 25, unit = TimeUnit.MINUTES)
    void measurePerformance_longArticle_HardLevel_reportsTerminalState() throws Exception {
        String originalContent = readFixtureOrFallback(LONG_ARTICLE_FIXTURE, vandeBharatLongArticleFallback());
        int originalWordCount = countWords(originalContent);

        Article article = createArticle("Vande Bharat Sleeper " + UUID.randomUUID(), originalContent);
        ArticleGenerationSubJob subJob = createSubJob(article, DifficultyLevel.HARD);

        logLiveEnvironment("longArticle_HardLevel", DifficultyLevel.HARD, originalWordCount);
        assertThat(originalWordCount)
                .as("long article fixture should trigger SOURCE_DIGEST")
                .isGreaterThan(LONG_ARTICLE_THRESHOLD_WORDS);

        Instant start = Instant.now();

        worker.process(subJob.getId());
        waitForTerminalStatus(subJob.getId(), List.of(JobStatus.COMPLETED, JobStatus.FAILED), 1_200);

        Instant end = Instant.now();
        log.info("[performance-test] longArticle_HardLevel totalDurationMs={}",
                end.toEpochMilli() - start.toEpochMilli());

        PipelineReport report = verifyAndReport(
                subJob.getId(),
                DifficultyLevel.HARD,
                article.getId(),
                VerificationMode.ALLOW_FAILED_BUT_REQUIRE_REASON
        );

        ArticleGenerationStepJob digestStep = report.findStep(GenerationStepType.SOURCE_DIGEST)
                .orElseThrow(() -> new AssertionError("SOURCE_DIGEST step must exist for long articles"));

        assertThat(digestStep.getStatus())
                .as("SOURCE_DIGEST must reach terminal state")
                .isIn(JobStatus.COMPLETED, JobStatus.FAILED);

        if (digestStep.getStatus() == JobStatus.COMPLETED) {
            ArticleGenerationStepJob contentStep = report.findStep(GenerationStepType.CONTENT)
                    .orElseThrow(() -> new AssertionError("CONTENT step must exist after SOURCE_DIGEST completes"));

            assertThat(contentStep.getStatus())
                    .as("CONTENT step must not remain PROCESSING")
                    .isNotEqualTo(JobStatus.PROCESSING);

            log.info("[performance-report] Digest path verified by SOURCE_DIGEST=COMPLETED and CONTENT step present. " +
                    "Check worker logs for isDigestUsed=true marker.");
        }
    }

    private ArticleGenerationSubJob createSubJob(Article article, DifficultyLevel level) {
        ArticleGenerationJob job = jobRepository.saveAndFlush(
                new ArticleGenerationJob(article.getId(), JobStatus.PROCESSING)
        );

        return subJobRepository.saveAndFlush(
                new ArticleGenerationSubJob(job, level, JobStatus.PENDING)
        );
    }

    private Article createArticle(String title, String content) {
        return articleRepository.saveAndFlush(Article.create(
                title,
                "Tech",
                "http://example.com/" + UUID.randomUUID(),
                content,
                Instant.now(),
                category,
                "slug-" + UUID.randomUUID()
        ));
    }

    private PipelineReport verifyAndReport(
            UUID subJobId,
            DifficultyLevel level,
            UUID articleId,
            VerificationMode mode
    ) {
        ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId).orElseThrow();
        List<ArticleGenerationStepJob> steps = stepJobRepository.findBySubJobIdOrderByStepType(subJobId);

        log.info("[performance-report] Level={} FinalSubJobStatus={} StepCount={}",
                level, subJob.getStatus(), steps.size());

        assertThat(subJob.getStatus())
                .as("SubJob must reach terminal state")
                .isIn(JobStatus.COMPLETED, JobStatus.FAILED);

        for (ArticleGenerationStepJob step : steps) {
            long durationMs = durationMs(step);

            log.info(
                    "[performance-report] Step={} Status={} Duration={}ms ValidationStatus={} ValidationErrors={} ErrorMessage={} FailureReason={} SkipReason={} Attempts={}",
                    step.getStepType(),
                    step.getStatus(),
                    durationMs,
                    step.getValidationStatus(),
                    step.getValidationErrors(),
                    step.getErrorMessage(),
                    reflectedString(step, "getFailureReason").orElse(""),
                    reflectedString(step, "getSkipReason").orElse(""),
                    step.getAttemptCount()
            );

            assertThat(step.getStatus())
                    .as("No step may remain PROCESSING after SubJob terminal state. step=" + step.getStepType())
                    .isNotEqualTo(JobStatus.PROCESSING);
        }

        if (mode == VerificationMode.REQUIRE_COMPLETED) {
            assertThat(subJob.getStatus())
                    .as("Smoke test requires successful completion")
                    .isEqualTo(JobStatus.COMPLETED);
        }

        if (subJob.getStatus() == JobStatus.FAILED) {
            assertThat(hasActionableFailureDetail(steps))
                    .as("FAILED sub-job must expose validationErrors, errorMessage, failureReason, or skipReason")
                    .isTrue();
        }

        contentRepository.findByArticleIdAndLevel(articleId, level).ifPresent(content -> {
            int generatedWordCount = countWords(content.getContent());
            LengthPolicy policy = LengthPolicy.forLevel(level);

            log.info(
                    "[performance-report] GeneratedContent level={} words={} preferred={}~{} hard={}~{}",
                    level,
                    generatedWordCount,
                    policy.preferredMin,
                    policy.preferredMax,
                    policy.hardMin,
                    policy.hardMax
            );

            if (subJob.getStatus() == JobStatus.COMPLETED) {
                assertThat(generatedWordCount)
                        .as("Completed content must stay within hard range")
                        .isBetween(policy.hardMin, policy.hardMax);
            }
        });

        return new PipelineReport(subJob, steps);
    }

    private boolean hasActionableFailureDetail(List<ArticleGenerationStepJob> steps) {
        return steps.stream().anyMatch(step -> !diagnosticText(step).isBlank());
    }

    private String diagnosticText(ArticleGenerationStepJob step) {
        return String.join(" ",
                safe(step.getValidationErrors()),
                safe(step.getErrorMessage()),
                reflectedString(step, "getFailureReason").orElse(""),
                reflectedString(step, "getSkipReason").orElse("")
        ).trim();
    }

    private long durationMs(ArticleGenerationStepJob step) {
        if (step.getStartedAt() == null || step.getCompletedAt() == null) {
            return -1;
        }
        return step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli();
    }

    private void waitForTerminalStatus(
            UUID subJobId,
            List<JobStatus> terminalStatuses,
            int timeoutSeconds
    ) throws InterruptedException {
        for (int i = 0; i < timeoutSeconds * 2; i++) {
            ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId).orElseThrow();
            if (terminalStatuses.contains(subJob.getStatus())) {
                return;
            }

            if (i % 20 == 0) {
                logCurrentState(subJobId, subJob.getStatus(), i / 2);
            }

            Thread.sleep(500);
        }

        logCurrentState(subJobId, JobStatus.PROCESSING, timeoutSeconds);
        throw new RuntimeException("Timeout waiting for subJob terminal status: " + subJobId);
    }

    private void logCurrentState(UUID subJobId, JobStatus currentStatus, int elapsedSeconds) {
        List<ArticleGenerationStepJob> steps = stepJobRepository.findBySubJobIdOrderByStepType(subJobId);

        log.warn("[performance-test] Waiting for terminal state. subJob={} elapsedSeconds={} currentStatus={} steps={}",
                subJobId,
                elapsedSeconds,
                currentStatus,
                steps.stream()
                        .map(step -> step.getStepType() + ":" + step.getStatus() + ":attempts=" + step.getAttemptCount())
                        .toList()
        );

        for (ArticleGenerationStepJob step : steps) {
            log.warn(
                    "[performance-test] Step snapshot step={} status={} startedAt={} completedAt={} lastHeartbeat={} validation={} errors={} message={} reason={}",
                    step.getStepType(),
                    step.getStatus(),
                    step.getStartedAt(),
                    step.getCompletedAt(),
                    step.getLastHeartbeatAt(),
                    step.getValidationStatus(),
                    step.getValidationErrors(),
                    step.getErrorMessage(),
                    reflectedString(step, "getFailureReason").orElse("")
            );
        }
    }

    private void logLiveEnvironment(String testName, DifficultyLevel level, int articleWordCount) {
        log.info(
                "[performance-test] test={} RUN_LIVE_OLLAMA_TESTS={} OLLAMA_BASE_URL={} activeProfiles={} level={} articleWordCount={}",
                testName,
                System.getenv("RUN_LIVE_OLLAMA_TESTS"),
                System.getenv("OLLAMA_BASE_URL"),
                String.join(",", environment.getActiveProfiles()),
                level,
                articleWordCount
        );
    }

    private String readFixtureOrFallback(Path path, String fallback) throws Exception {
        if (Files.exists(path)) {
            String content = Files.readString(path);
            log.info("[performance-test] Loaded fixture path={} words={}", path, countWords(content));
            return content;
        }

        log.warn("[performance-test] Fixture file not found. Using fallback content. path={}", path);
        return fallback;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Optional<String> reflectedString(Object target, String getterName) {
        try {
            Method method = target.getClass().getMethod(getterName);
            Object value = method.invoke(target);
            if (value == null) {
                return Optional.empty();
            }
            String text = value.toString();
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private String shortAiToolsFallback() {
        return """
                A group of small businesses in Seoul has started testing artificial intelligence tools to help workers handle daily tasks more quickly. The tools are used to draft simple emails, summarize customer questions, organize meeting notes, and prepare first versions of product descriptions.

                Several shop owners said the technology saves time, especially during busy hours when staff must answer online messages while also helping customers in person. One bakery owner said the tool does not replace workers, but it helps them avoid spending too much time on repeated writing tasks.

                Experts say these systems can be useful when people check the results carefully. They warn that AI tools may make mistakes, misunderstand local context, or produce text that sounds confident but is not fully accurate. For that reason, many companies are training employees to review AI-generated work before sending it to customers.

                Supporters believe the technology can give smaller companies access to services that once required larger teams. Critics say businesses should be careful not to depend on the tools too heavily. The early results suggest that AI is most helpful when it acts as an assistant rather than a final decision-maker.
                """;
    }

    private String vandeBharatLongArticleFallback() {
        return """
                The first sleeper in India's semi-high-speed Vande Bharat fleet is drawing crowds, selfies and intense curiosity, but many travellers still want to know what it is actually like on board.

                Lightning flashed above Kolkata's Howrah Junction station as rain lashed platform six. The waiting passengers barely seemed to notice, however, as they jostled and angled their phones to get selfies next to the sleek orange, black and grey nose of Indian Railways' newest object of fascination: the Vande Bharat Express sleeper train.

                Launched in January 2026, this is the first sleeper in the Vande Bharat semi-high-speed fleet, a train series that has become a point of national pride since services began in 2019. Clips of the sleeper's first journeys quickly went viral, and when one traveller visited Kolkata three months later, the excitement had hardly dimmed, with the train's 823 berths still selling out weeks in advance.

                Vande Bharat means "Salute to India" in Sanskrit. Designed and built in India, the fleet has been promoted as a cleaner, more modern upgrade from the country's older long-distance stock. The trains feature aerodynamic noses, sliding automatic doors and more comfortable interiors. Indian newspapers have described the sleeper carriages as unusually stylish for the country's long-distance rail network, and Prime Minister Narendra Modi has personally flagged off many Vande Bharat services, helping turn them into a symbol of national ambition.

                Tickets for the full Kolkata-Guwahati route cost around 2,400 rupees for third-class, where six berths share each open-plan compartment. It costs about 3,100 rupees in second-class, where four berths sit behind curtains, and about 3,800 rupees for a first-class berth in a closed cabin shared with three others.

                The new sleeper service runs six times a week in each direction between Kolkata in West Bengal and Guwahati in Assam. The journey takes about 14 hours, down from as much as 18 hours on older trains. With India's average monthly wage around 21,000 rupees, even the cheapest fare is beyond the reach of many people. That means the sleeper train is largely aimed at business travellers who want a more comfortable alternative to flying between two major commercial centres.

                The route also has wider appeal. Guwahati is home to the hilltop Kamakhya Temple, one of India's most important Hindu pilgrimage sites. For tourists, the train also stops at New Jalpaiguri Junction, a gateway to Darjeeling's famous tea fields. From Guwahati, visitors can continue to Shillong, a waterfall-dotted hill station known as the "Scotland of the East", or to Pobitora Wildlife Sanctuary, where jeep safaris offer the chance to see one-horned rhinos.

                After boarding at Howrah Junction, the traveller found the berth clean and orderly. The space included a plug socket, a reading light, USB and USB-C charging points, clean sheets, a blanket and a pillow. The carriage felt clean enough for passengers to walk around in socks, something that would have felt uncomfortable on some older Indian sleeper trains.

                At 18:20, the train pulled out exactly on time. A recently retired Indian Railways inspector in a smart purple shirt said he was riding simply to experience the new train. He photographed the railway logo woven into the sheet's floral pattern while other passengers settled in quietly with the low hum of the train in the background.

                Indian sleeper trains have long inspired both affection and dread. They are symbols of adventurous slow travel, but they are also associated with crowded carriages and difficult washrooms. After the Vande Bharat sleeper launched, an Indian Railways staffer's online post warning passengers to use proper toilet manners sparked debate. Another video showing rubbish across a carriage caused widespread dismay.

                On this journey, the chrome toilets, both Western and squat-style, stayed shiny throughout. Staff appeared very aware of the online criticism. Soon after departure, a cleaner carrying a cordless vacuum walked past the berth looking for the evening's first specks of dust. He introduced himself as Raju Nath and proudly showed off his favourite washroom, pointing to the clean toilet, the shower curtain and a dispenser that released a flowery scent.
                """;
    }

    private enum VerificationMode {
        REQUIRE_COMPLETED,
        ALLOW_FAILED_BUT_REQUIRE_REASON
    }

    private record PipelineReport(
            ArticleGenerationSubJob subJob,
            List<ArticleGenerationStepJob> steps
    ) {
        Optional<ArticleGenerationStepJob> findStep(GenerationStepType stepType) {
            return steps.stream()
                    .filter(step -> step.getStepType() == stepType)
                    .findFirst();
        }
    }

    private record LengthPolicy(
            int preferredMin,
            int preferredMax,
            int hardMin,
            int hardMax
    ) {
        static LengthPolicy forLevel(DifficultyLevel level) {
            return switch (level) {
                case EASY -> new LengthPolicy(180, 260, 160, 320);
                case MEDIUM -> new LengthPolicy(220, 320, 190, 380);
                case HARD -> new LengthPolicy(280, 420, 240, 500);
            };
        }
    }
}