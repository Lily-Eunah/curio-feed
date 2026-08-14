package com.curiofeed.backend.infrastructure.llm.eval;

public record EvalBaseline(
        double minQualityScore,
        int minWordCount,
        int maxWordCount,
        int expectedVocabCount,
        int expectedQuizCount
) {}
