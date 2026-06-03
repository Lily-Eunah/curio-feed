package com.curiofeed.backend.domain.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuizOptionsDeserializer extends StdDeserializer<QuizOptions> {

    private static final String CHOICE_KEYS = "ABCD";

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
        // Gemini sometimes responds with {"A": "text", "B": "text", ...} instead of {"choices": [...]}
        Map<String, String> abcdFallback = null;

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
                default -> {
                    // Fallback: handle {"A": "...", "B": "...", "C": "...", "D": "..."} format
                    if (field.length() == 1 && CHOICE_KEYS.indexOf(field.charAt(0)) >= 0
                            && p.currentToken() == JsonToken.VALUE_STRING) {
                        if (abcdFallback == null) abcdFallback = new LinkedHashMap<>();
                        abcdFallback.put(field, p.getText());
                    } else {
                        p.skipChildren();
                    }
                }
            }
        }

        // Convert A/B/C/D map to choices list if no structured choices were found
        if (choices == null && abcdFallback != null && !abcdFallback.isEmpty()) {
            choices = new ArrayList<>();
            for (Map.Entry<String, String> entry : abcdFallback.entrySet()) {
                choices.add(new QuizChoice(entry.getKey(), entry.getValue(), null));
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
