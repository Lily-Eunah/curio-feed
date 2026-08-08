package com.curiofeed.backend.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MistralRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {
    public record Message(String role, String content) {}

    public record ResponseFormat(String type) {
        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }
}
