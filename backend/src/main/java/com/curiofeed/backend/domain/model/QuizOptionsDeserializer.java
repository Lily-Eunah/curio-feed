package com.curiofeed.backend.domain.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuizOptionsDeserializer extends StdDeserializer<QuizOptions> {

    public QuizOptionsDeserializer() {
        super(QuizOptions.class);
    }

    @Override
    public QuizOptions deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            return new QuizOptions(readChoicesArray(p, ctxt), Map.of());
        }
        List<QuizChoice> choices = null;
        Map<String, String> explanations = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            switch (field) {
                case "choices" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        choices = readChoicesArray(p, ctxt);
                    }
                }
                case "explanations" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        JavaType mapType = ctxt.getTypeFactory().constructMapType(Map.class, String.class, String.class);
                        explanations = ctxt.readValue(p, mapType);
                    }
                }
                default -> p.skipChildren();
            }
        }
        return new QuizOptions(choices, explanations);
    }

    /**
     * choices 배열을 읽는다. 각 요소가 객체({key, text, explanation})일 수도 있고
     * 단순 문자열("A")일 수도 있다 — LLM이 포맷을 혼용하는 경우를 모두 처리한다.
     */
    private List<QuizChoice> readChoicesArray(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<QuizChoice> choices = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                choices.add(new QuizChoice(p.getText(), null, null));
            } else {
                choices.add(ctxt.readValue(p, QuizChoice.class));
            }
        }
        return choices;
    }
}
