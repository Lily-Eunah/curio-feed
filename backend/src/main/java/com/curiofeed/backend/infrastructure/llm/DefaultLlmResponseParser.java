package com.curiofeed.backend.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultLlmResponseParser implements LlmResponseParser {

    private final ObjectMapper objectMapper;

    public DefaultLlmResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T parse(String rawResponse, Class<T> targetType) {
        String cleaned = removeMarkdownCodeBlock(rawResponse);
        String json = extractJson(cleaned);
        try {
            return objectMapper.readValue(json, targetType);
        } catch (Exception e) {
            throw new LlmParseException("Failed to deserialize LLM response: " + e.getMessage(), e);
        }
    }

    /**
     * ```json ... ``` 마크다운 코드 블록 제거.
     */
    private String removeMarkdownCodeBlock(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline == -1) {
                return trimmed;
            }
            String withoutOpening = trimmed.substring(firstNewline + 1);
            if (withoutOpening.endsWith("```")) {
                return withoutOpening.substring(0, withoutOpening.length() - 3).trim();
            }
            return withoutOpening;
        }
        return raw;
    }

    /**
     * 첫 번째 '{' 위치에서 시작하여 balanced brace 추적으로 JSON 범위 추출.
     * 문자열 값 내부의 {} 는 따옴표 상태를 추적해 무시한다.
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        if (start == -1) {
            throw new LlmParseException("No JSON object found in LLM response");
        }

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        throw new LlmParseException("Unbalanced braces in LLM response — could not extract JSON");
    }
}
