package com.curiofeed.backend.infrastructure.llm.eval;

public record GoldenArticle(
        String id,
        String originalTitle,
        String originalContent,
        String category
) {}
