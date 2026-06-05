package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
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
        assertThat(prompt).contains("200~280 words");
        assertThat(prompt).contains("320 words");
    }

    @Test
    @DisplayName("buildContentPrompt: MEDIUM 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_mediumWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.MEDIUM, false);
        assertThat(prompt).contains("220~320 words");
        assertThat(prompt).contains("380 words");
    }

    @Test
    @DisplayName("buildContentPrompt: HARD 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_hardWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.HARD, false);
        assertThat(prompt).contains("280~420 words");
        assertThat(prompt).contains("500 words");
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
}
