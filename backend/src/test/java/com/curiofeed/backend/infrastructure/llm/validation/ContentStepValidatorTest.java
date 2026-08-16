package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentStepValidatorTest {

    private ContentStepValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ContentStepValidator();
    }

    private static String words(int count) {
        return "word ".repeat(count).trim();
    }

    // ── Null / blank ──────────────────────────────────────────────────────────

    @Test
    void blankContent_isHardFail() {
        ContentValidationResult result = validator.validate("", DifficultyLevel.EASY);
        assertThat(result.isHardFail()).isTrue();
    }

    @Test
    void nullContent_isHardFail() {
        ContentValidationResult result = validator.validate(null, DifficultyLevel.EASY);
        assertThat(result.isHardFail()).isTrue();
    }

    // ── EASY (hard [150, 280], preferred [180, 240]) ──────────────────────────

    @Test
    void easy_scenarios() {
        assertHardFailTooShort(140, DifficultyLevel.EASY);
        assertStatus(165, DifficultyLevel.EASY, ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE);
        assertValid(210, DifficultyLevel.EASY);
        assertStatus(265, DifficultyLevel.EASY, ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE);
        assertHardFailTooLong(300, DifficultyLevel.EASY);
    }

    // ── MEDIUM (hard [230, 380], preferred [270, 330]) ────────────────────────

    @Test
    void medium_scenarios() {
        assertHardFailTooShort(220, DifficultyLevel.MEDIUM);
        assertStatus(250, DifficultyLevel.MEDIUM, ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE);
        assertValid(300, DifficultyLevel.MEDIUM);
        assertStatus(355, DifficultyLevel.MEDIUM, ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE);
        assertHardFailTooLong(400, DifficultyLevel.MEDIUM);
    }

    // ── HARD (hard [330, 520], preferred [380, 450]) ──────────────────────────

    @Test
    void hard_scenarios() {
        assertHardFailTooShort(320, DifficultyLevel.HARD);
        assertStatus(350, DifficultyLevel.HARD, ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE);
        assertValid(410, DifficultyLevel.HARD);
        assertStatus(480, DifficultyLevel.HARD, ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE);
        assertHardFailTooLong(540, DifficultyLevel.HARD);
    }

    // ── Preferred ranges must not overlap, or the levels blur together ────────

    @Test
    void preferredRanges_doNotOverlapBetweenLevels() {
        // A length that is ideal for one level must not be ideal for another.
        assertValid(210, DifficultyLevel.EASY);
        assertThat(validator.validate(words(210), DifficultyLevel.MEDIUM).getStatus())
                .isNotEqualTo(ContentValidationResult.ValidationStatus.VALID);

        assertValid(300, DifficultyLevel.MEDIUM);
        assertThat(validator.validate(words(300), DifficultyLevel.EASY).getStatus())
                .isNotEqualTo(ContentValidationResult.ValidationStatus.VALID);
        assertThat(validator.validate(words(300), DifficultyLevel.HARD).getStatus())
                .isNotEqualTo(ContentValidationResult.ValidationStatus.VALID);

        assertValid(410, DifficultyLevel.HARD);
        assertThat(validator.validate(words(410), DifficultyLevel.MEDIUM).getStatus())
                .isNotEqualTo(ContentValidationResult.ValidationStatus.VALID);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertHardFailTooShort(int wordCount, DifficultyLevel level) {
        ContentValidationResult r = validator.validate(words(wordCount), level);
        assertThat(r.isHardFail()).as("%d words at %s", wordCount, level).isTrue();
        assertThat(r.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL);
    }

    private void assertHardFailTooLong(int wordCount, DifficultyLevel level) {
        ContentValidationResult r = validator.validate(words(wordCount), level);
        assertThat(r.isHardFail()).as("%d words at %s", wordCount, level).isTrue();
        assertThat(r.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL);
    }

    private void assertValid(int wordCount, DifficultyLevel level) {
        ContentValidationResult r = validator.validate(words(wordCount), level);
        assertThat(r.isSuccess()).as("%d words at %s", wordCount, level).isTrue();
        assertThat(r.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.VALID);
    }

    private void assertStatus(int wordCount, DifficultyLevel level, ContentValidationResult.ValidationStatus expected) {
        ContentValidationResult r = validator.validate(words(wordCount), level);
        assertThat(r.isHardFail()).as("%d words at %s", wordCount, level).isFalse();
        assertThat(r.getStatus()).isEqualTo(expected);
    }
}
