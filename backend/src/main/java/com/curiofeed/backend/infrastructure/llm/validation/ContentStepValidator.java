package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates Step 1 (content-only) output.
 * Hard failures cause a retry of Step 1.
 */
@Component
public class ContentStepValidator {

    // Hard limits: outside this range → hard fail (retry Step 1).
    // EASY   (A2-B1,  ~90 wpm): preferred 180-260 → hard floor 160, ceiling 320
    // MEDIUM (B1-B2, ~120 wpm): preferred 220-320 → hard floor 190, ceiling 380
    // HARD   (C1,    ~140 wpm): preferred 280-420 → hard floor 240, ceiling 500
    private static final Map<DifficultyLevel, int[]> WORD_COUNT_RANGES = Map.of(
        DifficultyLevel.EASY,   new int[]{160, 320},
        DifficultyLevel.MEDIUM, new int[]{190, 380},
        DifficultyLevel.HARD,   new int[]{240, 500}
    );

    // Preferred (soft warning) ranges — what the prompt targets.
    // EASY 2-3 min / MEDIUM 2-3 min / HARD 3-4 min
    private static final Map<DifficultyLevel, int[]> PREFERRED_RANGES = Map.of(
        DifficultyLevel.EASY,   new int[]{180, 260},
        DifficultyLevel.MEDIUM, new int[]{220, 320},
        DifficultyLevel.HARD,   new int[]{280, 420}
    );

    /**
     * Returns the list of validation errors.
     * Empty list = pass.  Non-empty = fail/warn.
     * Hard failures do NOT start with "[SOFT]".
     */
    public List<String> validate(String content, DifficultyLevel level) {
        List<String> errors = new ArrayList<>();

        if (content == null || content.isBlank()) {
            errors.add("content is blank or missing");
            return errors;
        }

        int wordCount = countWords(content);
        int[] hard = WORD_COUNT_RANGES.get(level);
        int[] preferred = PREFERRED_RANGES.get(level);

        if (wordCount < hard[0]) {
            errors.add("content too short: " + wordCount + " words (min " + hard[0] + " for " + level + ")");
        } else if (wordCount > hard[1]) {
            errors.add("content too long: " + wordCount + " words (max " + hard[1] + " for " + level + ")");
        } else if (wordCount < preferred[0]) {
            errors.add("[SOFT] content shorter than preferred (" + wordCount + " words, preferred min " + preferred[0] + ")");
        } else if (wordCount > preferred[1]) {
            errors.add("[SOFT] content longer than preferred (" + wordCount + " words, preferred max " + preferred[1] + ")");
        }

        return errors;
    }

    public boolean isHardFail(List<String> errors) {
        return errors.stream().anyMatch(e -> !e.startsWith("[SOFT]"));
    }

    private int countWords(String text) {
        return text.trim().split("\\s+").length;
    }
}
