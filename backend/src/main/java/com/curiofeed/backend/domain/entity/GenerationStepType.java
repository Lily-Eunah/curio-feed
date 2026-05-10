package com.curiofeed.backend.domain.entity;

public enum GenerationStepType {
    SOURCE_DIGEST,
    CONTENT,
    VOCABULARY,
    QUIZ;

    /** Steps that must complete before this one can start. */
    public boolean dependsOn(GenerationStepType other) {
        return switch (this) {
            case SOURCE_DIGEST -> false;
            case CONTENT -> other == SOURCE_DIGEST; // Optional dependency handled in worker
            case VOCABULARY -> other == CONTENT;
            case QUIZ -> other == CONTENT || other == VOCABULARY;
        };
    }
}
