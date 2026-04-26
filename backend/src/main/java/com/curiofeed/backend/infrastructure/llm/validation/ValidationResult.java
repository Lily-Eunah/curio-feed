package com.curiofeed.backend.infrastructure.llm.validation;

import java.util.List;

public record ValidationResult(
        boolean valid,
        List<String> errors,
        double score
) {
    public static ValidationResult pass(double score) {
        return new ValidationResult(true, List.of(), score);
    }

    public static ValidationResult fail(List<String> errors, double score) {
        return new ValidationResult(false, List.copyOf(errors), score);
    }
}
