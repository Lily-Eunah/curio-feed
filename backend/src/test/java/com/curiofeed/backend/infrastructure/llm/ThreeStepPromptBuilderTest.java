package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.infrastructure.llm.validation.ContentValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThreeStepPromptBuilderTest {

    private ThreeStepPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ThreeStepPromptBuilder();
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 3~4 문단 지침이 포함된다")
    void buildContentPrompt_containsParagraphGuideline(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("3 to 4 natural paragraphs");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 빈 줄 구분 지침이 포함된다")
    void buildContentPrompt_containsBlankLineSeparation(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("Separate paragraphs with a blank line");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 bullet list 금지 지침이 포함된다")
    void buildContentPrompt_containsBulletListProhibition(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("Do not use bullet points, numbered lists");
    }

    @Test
    @DisplayName("buildContentPrompt: EASY 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_easyWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, false);
        assertThat(prompt).contains("180~240 words");
        assertThat(prompt).contains("280 words");
    }

    @Test
    @DisplayName("buildContentPrompt: MEDIUM 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_mediumWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.MEDIUM, false);
        assertThat(prompt).contains("290~340 words");
        assertThat(prompt).contains("380 words");
    }

    @Test
    @DisplayName("buildContentPrompt: HARD 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_hardWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.HARD, false);
        assertThat(prompt).contains("410~470 words");
        assertThat(prompt).contains("520 words");
    }

    @Test
    @DisplayName("buildContentPrompt: MEDIUM은 EASY와 구조적으로 다르라는 지침을 받는다")
    void buildContentPrompt_mediumIsStructurallyDistinctFromEasy() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.MEDIUM, false);
        assertThat(prompt).contains("MORE demanding than EASY");
        assertThat(prompt).contains("structural, not decorative");
    }

    @Test
    @DisplayName("buildContentPrompt: HARD는 유의어 치환을 금지당한다")
    void buildContentPrompt_hardForbidsThesaurusProse() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.HARD, false);
        assertThat(prompt).contains("Do NOT swap plain words for rarer synonyms");
        assertThat(prompt).contains("Thesaurus prose is a failure");
    }

    @Test
    @DisplayName("buildContentPrompt: HARD는 원문에 없는 대립구도를 만들지 말라고 지시받는다")
    void buildContentPrompt_hardMustNotManufactureDebate() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.HARD, true);
        assertThat(prompt).contains("Never stage a debate the source does not have");
        assertThat(prompt).contains("If its experts broadly agree, present them as agreeing");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 온보딩과 동일한 레벨 보정 예시가 포함된다")
    void buildContentPrompt_containsLevelCalibration(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("LEVEL CALIBRATION");
        assertThat(prompt).contains("Many people are moving to smaller homes");
    }

    @Test
    @DisplayName("buildContentPrompt: digest 기반 플래그가 소스 컨텍스트 문구에 반영된다")
    void buildContentPrompt_digestBasedFlag() {
        String digestPrompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, true);
        String originalPrompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, false);

        assertThat(digestPrompt).contains("SOURCE DIGEST");
        assertThat(originalPrompt).contains("ORIGINAL ARTICLE");
    }

    // ── buildQuizPrompt ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildQuizPrompt: Q1은 passage comprehension MCQ 지침이 포함된다")
    void buildQuizPrompt_q1IsPassageComprehension(DifficultyLevel level) {
        String prompt = builder.buildQuizPrompt("content", "[]", level);
        assertThat(prompt).contains("Passage Comprehension");
        assertThat(prompt).containsAnyOf("main idea", "central point", "central situation", "main concern");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildQuizPrompt: Q2는 passage reasoning MCQ 지침이 포함된다")
    void buildQuizPrompt_q2IsPassageReasoning(DifficultyLevel level) {
        String prompt = builder.buildQuizPrompt("content", "[]", level);
        assertThat(prompt).contains("Passage Reasoning");
        assertThat(prompt).containsAnyOf("cause", "inference", "imply", "infer");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildQuizPrompt: Q3는 passage-grounded vocabulary SHORT_ANSWER 지침이 포함된다")
    void buildQuizPrompt_q3IsPassageGroundedShortAnswer(DifficultyLevel level) {
        String prompt = builder.buildQuizPrompt("content", "[]", level);
        assertThat(prompt).contains("SHORT_ANSWER");
        assertThat(prompt).contains("Passage-Grounded Vocabulary");
        assertThat(prompt).contains("article");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildQuizPrompt: Q3 correctAnswer는 vocab word를 포함한 완전한 문장이어야 함을 명시한다")
    void buildQuizPrompt_q3ModelAnswerMustContainVocabWord(DifficultyLevel level) {
        String prompt = builder.buildQuizPrompt("content", "[]", level);
        assertThat(prompt).containsAnyOf("TARGET_WORD", "target vocab word", "vocabulary word");
        assertThat(prompt).contains("correctAnswer");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildQuizPrompt: Q3 단순 fill-in-the-blank 금지 지침이 포함된다")
    void buildQuizPrompt_q3FillInBlankBanned(DifficultyLevel level) {
        String prompt = builder.buildQuizPrompt("content", "[]", level);
        assertThat(prompt).containsAnyOf("fill-in-the-blank", "unrelated to the article");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildQuizPrompt: MCQ correctAnswer는 A/B/C/D 중 하나여야 함을 명시한다")
    void buildQuizPrompt_mcqCorrectAnswerMustBeABCD(DifficultyLevel level) {
        String prompt = builder.buildQuizPrompt("content", "[]", level);
        assertThat(prompt).contains("\"A\", \"B\", \"C\", or \"D\"");
    }

    @Test
    @DisplayName("buildQuizRetryPrompt: q3_not_passage_grounded 수정 지침이 포함된다")
    void buildQuizRetryPrompt_q3NotPassageGrounded() {
        String prompt = builder.buildQuizRetryPrompt("content", "[]", DifficultyLevel.MEDIUM, "q3_not_passage_grounded");
        assertThat(prompt).contains("CORRECTION");
        assertThat(prompt).containsAnyOf("article content", "vocabulary word");
    }

    @Test
    @DisplayName("buildQuizRetryPrompt: q2_not_reasoning 수정 지침이 포함된다")
    void buildQuizRetryPrompt_q2NotReasoning() {
        String prompt = builder.buildQuizRetryPrompt("content", "[]", DifficultyLevel.MEDIUM, "q2_not_reasoning");
        assertThat(prompt).contains("CORRECTION");
        assertThat(prompt).containsAnyOf("passage reasoning", "cause/effect");
    }

    @Test
    @DisplayName("buildSourceDigestPrompt: 사람 디테일을 1~2개 보존하되 원문 표현은 쓰지 말라고 지시한다")
    void buildSourceDigestPrompt_keepsHumanDetailAsIndirectSpeech() {
        String prompt = builder.buildSourceDigestPrompt("Original Title", "Original article body");

        assertThat(prompt).contains("KEEP 1-2 HUMAN DETAILS");
        assertThat(prompt).contains("indirect speech");
        assertThat(prompt).contains("RESTATE EVERYTHING");
        assertThat(prompt).contains("Never lift a run of words from the article");
        assertThat(prompt).contains("Name the person if the article names them");
        assertThat(prompt).contains("humanDetails");
    }

    @Test
    @DisplayName("buildSourceDigestRetryPrompt: 복제 사유일 때 제목이 아니라 재진술을 교정한다")
    void buildSourceDigestRetryPrompt_copiedSourceBranch() {
        String copied = builder.buildSourceDigestRetryPrompt("Original Title", "body", "copied_source");
        String title = builder.buildSourceDigestRetryPrompt("Original Title", "body", "title_too_similar");

        assertThat(copied).contains("reused the article's own wording");
        assertThat(copied).doesNotContain("too similar to the original title");

        assertThat(title).contains("too similar to the original title");
        assertThat(title).doesNotContain("reused the article's own wording");
    }

    @Test
    @DisplayName("buildSourceDigestPrompt: 가장 긴 레벨을 지탱할 만큼 추출하라고 지시한다")
    void buildSourceDigestPrompt_extractsEnoughForLongestLevel() {
        String prompt = builder.buildSourceDigestPrompt("Original Title", "body");
        assertThat(prompt).contains("at least 12 substantive items");
        assertThat(prompt).contains("cannot invent what you omit");
    }

    @Test
    @DisplayName("buildContentPrompt: EASY는 다이제스트가 넘치게 주어진다는 전제로 취사선택을 지시받는다")
    void buildContentPrompt_easyIsToldToSelectNotCoverEverything() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, true);
        assertThat(prompt).contains("SELECT the few facts");
        assertThat(prompt).contains("is a failure at this level, not thoroughness");
    }

    @Test
    @DisplayName("sourceDigestSchema: humanDetails가 who/what 구조로 필수 필드에 포함된다")
    void sourceDigestSchema_requiresHumanDetails() {
        var schema = ThreeStepPromptBuilder.sourceDigestSchema();
        var digest = (java.util.Map<?, ?>) ((java.util.Map<?, ?>) schema.get("properties")).get("sourceDigest");

        assertThat(digest.get("required").toString()).contains("humanDetails");

        var items = (java.util.Map<?, ?>) ((java.util.Map<?, ?>) ((java.util.Map<?, ?>) digest.get("properties"))
                .get("humanDetails")).get("items");
        assertThat(items.get("required").toString()).contains("who").contains("what");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 사람 디테일을 최소 1개 넣으라는 지침이 모든 난이도에 포함된다")
    void buildContentPrompt_requiresAHumanDetail(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, true);
        assertThat(prompt).contains("Work at least one HUMAN DETAIL");
        assertThat(prompt).doesNotContain("Omit minor examples, repeated details, secondary quotes");
    }

    @Test
    @DisplayName("buildContentRetryPrompt: retryReason이 null이어도 NPE 없이 기본 프롬프트를 만든다")
    void buildContentRetryPrompt_nullRetryReason_doesNotThrow() {
        // Safety and parse failures leave retryReason unset; switching on it used to throw NPE
        // and burn a retry attempt without ever reaching the LLM.
        ContentValidationResult result = ContentValidationResult.builder()
                .success(false)
                .level(DifficultyLevel.EASY)
                .retryReason(null)
                .build();

        String prompt = builder.buildContentRetryPrompt("source text", DifficultyLevel.EASY, result, true);

        assertThat(prompt).contains("3 to 4 natural paragraphs");
        assertThat(prompt).doesNotContain("CORRECTION");
    }
}
