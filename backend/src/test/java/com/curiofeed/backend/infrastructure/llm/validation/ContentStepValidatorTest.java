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
        List<String> errors = validator.validate("", DifficultyLevel.EASY);
        assertThat(validator.isHardFail(errors)).isTrue();
    }

    @Test
    void nullContent_isHardFail() {
        List<String> errors = validator.validate(null, DifficultyLevel.EASY);
        assertThat(validator.isHardFail(errors)).isTrue();
    }

    // ── EASY (hard [160, 320], preferred [180, 260]) ──────────────────────────

    @Test
    void easy_belowHardMin_isHardFail() {
        // 159 words — below EASY hard min (160)
        List<String> errors = validator.validate(words(159), DifficultyLevel.EASY);
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors.get(0)).contains("too short");
    }

    @Test
    void easy_atHardMin_softWarnOnly() {
        // 160 words — at hard-min; below preferred 180, so soft warn expected
        List<String> errors = validator.validate(words(160), DifficultyLevel.EASY);
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]"));
    }

    @Test
    void easy_inPreferredRange_noErrors() {
        // 220 words — inside preferred [180, 260]
        List<String> errors = validator.validate(words(220), DifficultyLevel.EASY);
        assertThat(errors).isEmpty();
    }

    @Test
    void easy_abovePreferredBelowHardMax_softWarn() {
        // 300 words — above preferred max (260) but below hard max (320)
        List<String> errors = validator.validate(words(300), DifficultyLevel.EASY);
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]") && e.contains("longer"));
    }

    @Test
    void easy_aboveHardMax_isHardFail() {
        // 321 words — above EASY hard max (320)
        List<String> errors = validator.validate(words(321), DifficultyLevel.EASY);
        assertThat(validator.isHardFail(errors)).isTrue();
        assertThat(errors.get(0)).contains("too long");
    }

    // ── MEDIUM (hard [190, 380], preferred [220, 320]) ────────────────────────

    @Test
    void medium_belowHardMin_isHardFail() {
        // 189 words — below MEDIUM hard min (190)
        List<String> errors = validator.validate(words(189), DifficultyLevel.MEDIUM);
        assertThat(validator.isHardFail(errors)).isTrue();
    }

    @Test
    void medium_atHardMin_softWarnOnly() {
        // 190 words — at hard-min; below preferred 220, so soft warn expected
        List<String> errors = validator.validate(words(190), DifficultyLevel.MEDIUM);
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]"));
    }

    @Test
    void medium_inPreferredRange_noErrors() {
        // 270 words — inside preferred [220, 320]
        List<String> errors = validator.validate(words(270), DifficultyLevel.MEDIUM);
        assertThat(errors).isEmpty();
    }

    @Test
    void medium_aboveHardMax_isHardFail() {
        // 381 words — above MEDIUM hard max (380)
        List<String> errors = validator.validate(words(381), DifficultyLevel.MEDIUM);
        assertThat(validator.isHardFail(errors)).isTrue();
    }

    // ── HARD (hard [240, 500], preferred [280, 420]) ──────────────────────────

    @Test
    void hard_belowHardMin_isHardFail() {
        // 239 words — below HARD hard min (240)
        List<String> errors = validator.validate(words(239), DifficultyLevel.HARD);
        assertThat(validator.isHardFail(errors)).isTrue();
    }

    @Test
    void hard_atHardMin_softWarnOnly() {
        // 240 words — at hard-min; below preferred 280, so soft warn expected
        List<String> errors = validator.validate(words(240), DifficultyLevel.HARD);
        assertThat(validator.isHardFail(errors)).isFalse();
        assertThat(errors).anyMatch(e -> e.startsWith("[SOFT]"));
    }

    @Test
    void hard_inPreferredRange_noErrors() {
        // 350 words — inside preferred [280, 420]
        List<String> errors = validator.validate(words(350), DifficultyLevel.HARD);
        assertThat(errors).isEmpty();
    }

    @Test
    void hard_aboveHardMax_isHardFail() {
        // 501 words — above HARD hard max (500)
        List<String> errors = validator.validate(words(501), DifficultyLevel.HARD);
        assertThat(validator.isHardFail(errors)).isTrue();
    }
}
