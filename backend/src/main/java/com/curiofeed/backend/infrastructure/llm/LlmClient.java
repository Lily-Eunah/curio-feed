package com.curiofeed.backend.infrastructure.llm;

import java.util.Map;

public interface LlmClient {
    String generate(String prompt);

    /** Generate with a custom JSON output schema (for step-specific output). */
    default String generate(String prompt, Map<String, Object> schema) {
        return generate(prompt);
    }
}
