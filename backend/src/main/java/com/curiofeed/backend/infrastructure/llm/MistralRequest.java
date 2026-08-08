package com.curiofeed.backend.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MistralRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {
    public record Message(String role, String content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResponseFormat(
            String type,
            @JsonProperty("json_schema") JsonSchema jsonSchema
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record JsonSchema(
                String name,
                Boolean strict,
                Map<String, Object> schema
        ) {}

        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object", null);
        }

        public static ResponseFormat jsonSchema(Map<String, Object> schema) {
            return new ResponseFormat("json_schema", new JsonSchema("step_response_schema", true, schema));
        }
    }
}
