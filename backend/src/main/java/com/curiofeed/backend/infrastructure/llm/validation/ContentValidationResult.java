package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ContentValidationResult {
    public enum ValidationStatus {
        VALID,
        BELOW_PREFERRED_RANGE,
        ABOVE_PREFERRED_RANGE,
        TOO_SHORT_HARD_FAIL,
        TOO_LONG_HARD_FAIL
    }

    private final boolean success;
    private final DifficultyLevel level;
    private final int actualWordCount;
    private final int preferredMin;
    private final int preferredMax;
    private final int hardMin;
    private final int hardMax;
    private final ValidationStatus status;
    private final String retryReason; // e.g., "too_long", "too_short"
    private final String message;
    private final List<String> errors;

    public boolean isHardFail() {
        return status == ValidationStatus.TOO_SHORT_HARD_FAIL || status == ValidationStatus.TOO_LONG_HARD_FAIL;
    }
}
