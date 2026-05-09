package com.curiofeed.backend.domain.entity;

public enum GenerationStepType {
    CONTENT,
    VOCABULARY,
    QUIZ;

    /** Steps that must complete before this one can start. */
    public boolean dependsOn(GenerationStepType other) {
        return switch (this) {
            case CONTENT -> false;
            case VOCABULARY -> other == CONTENT;
            case QUIZ -> other == CONTENT || other == VOCABULARY;
        };
    }
}
