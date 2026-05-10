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

    // ── EASY (hard [160, 320], preferred [180, 260]) ──────────────────────────

    @Test
    void easy_scenarios() {
        // 150 words → hard fail too_short
        ContentValidationResult r150 = validator.validate(words(150), DifficultyLevel.EASY);
        assertThat(r150.isHardFail()).isTrue();
        assertThat(r150.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL);

        // 170 words → valid with BELOW_PREFERRED_RANGE warning
        ContentValidationResult r170 = validator.validate(words(170), DifficultyLevel.EASY);
        assertThat(r170.isHardFail()).isFalse();
        assertThat(r170.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE);

        // 250 words → valid
        ContentValidationResult r250 = validator.validate(words(250), DifficultyLevel.EASY);
        assertThat(r250.isSuccess()).isTrue();
        assertThat(r250.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.VALID);

        // 300 words → valid with ABOVE_PREFERRED_RANGE warning
        ContentValidationResult r300 = validator.validate(words(300), DifficultyLevel.EASY);
        assertThat(r300.isHardFail()).isFalse();
        assertThat(r300.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE);

        // 330 words → hard fail too_long
        ContentValidationResult r330 = validator.validate(words(330), DifficultyLevel.EASY);
        assertThat(r330.isHardFail()).isTrue();
        assertThat(r330.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL);
    }

    // ── MEDIUM (hard [190, 380], preferred [220, 320]) ────────────────────────

    @Test
    void medium_scenarios() {
        // 180 words → hard fail too_short
        ContentValidationResult r180 = validator.validate(words(180), DifficultyLevel.MEDIUM);
        assertThat(r180.isHardFail()).isTrue();
        assertThat(r180.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL);

        // 200 words → valid with BELOW_PREFERRED_RANGE warning
        ContentValidationResult r200 = validator.validate(words(200), DifficultyLevel.MEDIUM);
        assertThat(r200.isHardFail()).isFalse();
        assertThat(r200.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE);

        // 300 words → valid
        ContentValidationResult r300 = validator.validate(words(300), DifficultyLevel.MEDIUM);
        assertThat(r300.isSuccess()).isTrue();
        assertThat(r300.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.VALID);

        // 360 words → valid with ABOVE_PREFERRED_RANGE warning
        ContentValidationResult r360 = validator.validate(words(360), DifficultyLevel.MEDIUM);
        assertThat(r360.isHardFail()).isFalse();
        assertThat(r360.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE);

        // 390 words → hard fail too_long
        ContentValidationResult r390 = validator.validate(words(390), DifficultyLevel.MEDIUM);
        assertThat(r390.isHardFail()).isTrue();
        assertThat(r390.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL);
    }

    // ── HARD (hard [240, 500], preferred [280, 420]) ──────────────────────────

    @Test
    void hard_scenarios() {
        // 230 words → hard fail too_short
        ContentValidationResult r230 = validator.validate(words(230), DifficultyLevel.HARD);
        assertThat(r230.isHardFail()).isTrue();
        assertThat(r230.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL);

        // 260 words → valid with BELOW_PREFERRED_RANGE warning
        ContentValidationResult r260 = validator.validate(words(260), DifficultyLevel.HARD);
        assertThat(r260.isHardFail()).isFalse();
        assertThat(r260.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE);

        // 400 words → valid
        ContentValidationResult r400 = validator.validate(words(400), DifficultyLevel.HARD);
        assertThat(r400.isSuccess()).isTrue();
        assertThat(r400.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.VALID);

        // 460 words → valid with ABOVE_PREFERRED_RANGE warning
        ContentValidationResult r460 = validator.validate(words(460), DifficultyLevel.HARD);
        assertThat(r460.isHardFail()).isFalse();
        assertThat(r460.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE);

        // 510 words → hard fail too_long
        ContentValidationResult r510 = validator.validate(words(510), DifficultyLevel.HARD);
        assertThat(r510.isHardFail()).isTrue();
        assertThat(r510.getStatus()).isEqualTo(ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL);
    }
}
