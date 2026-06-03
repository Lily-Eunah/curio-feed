package com.curiofeed.backend.infrastructure.llm;

import java.util.List;
import java.util.Map;

public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(
            Double temperature,
            String responseMimeType,
            Map<String, Object> responseSchema
    ) {}
}
