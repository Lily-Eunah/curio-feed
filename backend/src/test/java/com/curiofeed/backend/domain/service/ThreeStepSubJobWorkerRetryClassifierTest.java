package com.curiofeed.backend.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the static retry-reason classifier methods in ThreeStepSubJobWorker.
 * These are pure logic tests — no Spring context needed.
 */
class ThreeStepSubJobWorkerRetryClassifierTest {

    // ── CONTENT classifiers ───────────────────────────────────────────────────

    @Test
    @DisplayName("content too short error → too_short reason")
    void classifyContent_tooShort() {
        String error = "content too short: 140 words (min 160 for EASY)";
        assertThat(ThreeStepSubJobWorker.classifyContentRetryReason(error))
                .isEqualTo("too_short");
    }

    @Test
    @DisplayName("content too long error → too_long reason")
    void classifyContent_tooLong() {
        String error = "content too long: 410 words (max 380 for MEDIUM)";
        assertThat(ThreeStepSubJobWorker.classifyContentRetryReason(error))
                .isEqualTo("too_long");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"some unknown error", "content is blank or missing", ""})
    @DisplayName("unrecognised / null content error → unknown reason")
    void classifyContent_unknown(String error) {
        assertThat(ThreeStepSubJobWorker.classifyContentRetryReason(error))
                .isEqualTo("unknown");
    }

    @Test
    @DisplayName("content error matching is case-insensitive")
    void classifyContent_caseInsensitive() {
        assertThat(ThreeStepSubJobWorker.classifyContentRetryReason("Content Too Short: 130 words"))
                .isEqualTo("too_short");
        assertThat(ThreeStepSubJobWorker.classifyContentRetryReason("Content Too Long: 500 words"))
                .isEqualTo("too_long");
    }

    // ── VOCABULARY classifiers ────────────────────────────────────────────────

    @Test
    @DisplayName("vocab word not found in content → word_not_in_content reason")
    void classifyVocab_wordNotInContent() {
        String error = "vocab[2] word not found in content (base or inflected form): targeted";
        assertThat(ThreeStepSubJobWorker.classifyVocabRetryReason(error))
                .isEqualTo("word_not_in_content");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"expected 5 vocabularies, got 4", "vocab[0].word is blank", ""})
    @DisplayName("unrecognised / null vocab error → unknown reason")
    void classifyVocab_unknown(String error) {
        assertThat(ThreeStepSubJobWorker.classifyVocabRetryReason(error))
                .isEqualTo("unknown");
    }

    @Test
    @DisplayName("vocab error matching is case-insensitive")
    void classifyVocab_caseInsensitive() {
        assertThat(ThreeStepSubJobWorker.classifyVocabRetryReason("Word Not Found In Content: surge"))
                .isEqualTo("word_not_in_content");
    }

    // ── QUIZ classifiers ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Q2 choices do not contain vocab word → q2_not_vocab_application reason")
    void classifyQuiz_q2NotVocabApplication() {
        String error = "[SOFT] quiz[1] (Q2) choices do not appear to contain a vocab word — may not be vocabulary-application";
        assertThat(ThreeStepSubJobWorker.classifyQuizRetryReason(error))
                .isEqualTo("q2_not_vocab_application");
    }

    @Test
    @DisplayName("Q3 correctAnswer not in vocab list → q3_answer_not_in_vocab reason")
    void classifyQuiz_q3AnswerNotInVocab() {
        String error = "[SOFT] quiz[2] correctAnswer 'restricting' not found in vocab list [restrict, surge, ...]";
        assertThat(ThreeStepSubJobWorker.classifyQuizRetryReason(error))
                .isEqualTo("q3_answer_not_in_vocab");
    }

    @Test
    @DisplayName("when both Q2 and Q3 errors present, Q2 reason is preferred")
    void classifyQuiz_preferQ2OverQ3() {
        String bothErrors = "[SOFT] quiz[1] (Q2) choices do not appear to contain a vocab word; " +
                "[SOFT] quiz[2] correctAnswer 'unknown' not found in vocab list [...]";
        assertThat(ThreeStepSubJobWorker.classifyQuizRetryReason(bothErrors))
                .isEqualTo("q2_not_vocab_application");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"expected 3 quizzes, got 2", "quiz[0] must be MULTIPLE_CHOICE", ""})
    @DisplayName("unrecognised / null quiz error → unknown reason")
    void classifyQuiz_unknown(String error) {
        assertThat(ThreeStepSubJobWorker.classifyQuizRetryReason(error))
                .isEqualTo("unknown");
    }

    @Test
    @DisplayName("quiz error matching is case-insensitive")
    void classifyQuiz_caseInsensitive() {
        assertThat(ThreeStepSubJobWorker.classifyQuizRetryReason(
                "Choices Do Not Appear To Contain A Vocab Word"))
                .isEqualTo("q2_not_vocab_application");
    }
}
