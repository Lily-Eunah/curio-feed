package com.curiofeed.backend.infrastructure.llm;

public interface LlmResponseParser {
    <T> T parse(String rawResponse, Class<T> targetType);
}
