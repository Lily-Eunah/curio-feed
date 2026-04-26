package com.curiofeed.backend.infrastructure.llm;

public interface LlmClient {
    String generate(String prompt);
}
