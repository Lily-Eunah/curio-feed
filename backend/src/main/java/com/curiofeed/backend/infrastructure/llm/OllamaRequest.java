package com.curiofeed.backend.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaRequest(String model, String prompt, boolean stream, Options options, Map<String, Object> format) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Options(@JsonProperty("num_ctx") int numCtx, @JsonProperty("temperature") Double temperature) {
        public Options(int numCtx) { this(numCtx, null); }
    }
}
