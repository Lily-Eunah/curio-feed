package com.curiofeed.backend.infrastructure.llm;

public record OllamaResponse(String model, String response, boolean done) {
}
