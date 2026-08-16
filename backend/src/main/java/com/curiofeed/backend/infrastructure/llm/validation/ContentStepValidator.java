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
    // Kept in sync with the per-level targets in ThreeStepPromptBuilder.buildContentPrompt.
    private static final Map<DifficultyLevel, int[]> HARD_RANGES = Map.of(
        DifficultyLevel.EASY,   new int[]{150, 280},
        DifficultyLevel.MEDIUM, new int[]{230, 380},
        DifficultyLevel.HARD,   new int[]{330, 520}
    );

    // Preferred (soft warning) ranges — what the prompt targets.
    // These must NOT overlap between levels: when they did, EASY and MEDIUM converged on the
    // same length and the levels became hard to tell apart.
    private static final Map<DifficultyLevel, int[]> PREFERRED_RANGES = Map.of(
        DifficultyLevel.EASY,   new int[]{180, 240},
        DifficultyLevel.MEDIUM, new int[]{270, 330},
        DifficultyLevel.HARD,   new int[]{380, 450}
    );

    /**
     * Returns the validation result including status and word count metadata.
     */
    public ContentValidationResult validate(String content, DifficultyLevel level) {
        List<String> errors = new ArrayList<>();

        if (content == null || content.isBlank()) {
            errors.add("content is blank or missing");
            return ContentValidationResult.builder()
                    .success(false)
                    .level(level)
                    .status(ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL)
                    .retryReason("too_short")
                    .errors(errors)
                    .build();
        }

        int wordCount = countWords(content);
        int[] hard = HARD_RANGES.get(level);
        int[] preferred = PREFERRED_RANGES.get(level);

        ContentValidationResult.ValidationStatus status = ContentValidationResult.ValidationStatus.VALID;
        String retryReason = null;

        if (wordCount < hard[0]) {
            status = ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL;
            retryReason = "too_short";
            errors.add("HARD FAIL: content too short (" + wordCount + " < " + hard[0] + ")");
        } else if (wordCount > hard[1]) {
            status = ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL;
            retryReason = "too_long";
            errors.add("HARD FAIL: content too long (" + wordCount + " > " + hard[1] + ")");
        } else if (wordCount < preferred[0]) {
            status = ContentValidationResult.ValidationStatus.BELOW_PREFERRED_RANGE;
            errors.add("SOFT WARN: content below preferred (" + wordCount + " < " + preferred[0] + ")");
        } else if (wordCount > preferred[1]) {
            status = ContentValidationResult.ValidationStatus.ABOVE_PREFERRED_RANGE;
            errors.add("SOFT WARN: content above preferred (" + wordCount + " > " + preferred[1] + ")");
        }

        boolean success = (status != ContentValidationResult.ValidationStatus.TOO_SHORT_HARD_FAIL &&
                           status != ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL);

        return ContentValidationResult.builder()
                .success(success)
                .level(level)
                .actualWordCount(wordCount)
                .preferredMin(preferred[0])
                .preferredMax(preferred[1])
                .hardMin(hard[0])
                .hardMax(hard[1])
                .status(status)
                .retryReason(retryReason)
                .message(errors.isEmpty() ? "VALID" : String.join("; ", errors))
                .errors(errors)
                .build();
    }

    private int countWords(String text) {
        return text.trim().split("\\s+").length;
    }
}
