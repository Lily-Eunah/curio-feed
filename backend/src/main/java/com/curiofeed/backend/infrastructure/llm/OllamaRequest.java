package com.curiofeed.backend.infrastructure.llm;

public record OllamaRequest(String model, String prompt, boolean stream) {
}
