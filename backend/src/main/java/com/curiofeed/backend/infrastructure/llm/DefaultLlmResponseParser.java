package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.model.GenerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultLlmResponseParser implements LlmResponseParser {

    private static final Logger log = LoggerFactory.getLogger(DefaultLlmResponseParser.class);

    private final ObjectMapper objectMapper;

    public DefaultLlmResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // "options": {content-without-colons} → e.g. {"A","B","C","D"} 같은 비정상 포맷 처리
    private static final Pattern MALFORMED_OPTIONS_PATTERN =
            Pattern.compile("\"options\"\\s*:\\s*\\{([^{}]*)\\}", Pattern.DOTALL);

    @Override
    public <T> T parse(String rawResponse, Class<T> targetType) {
        String cleaned = removeMarkdownCodeBlock(rawResponse);
        String json = extractJson(cleaned);
        String fixed = fixMalformedOptions(json);
        try {
            T result = objectMapper.readValue(fixed, targetType);
            if (result instanceof GenerationResult gr) {
                return targetType.cast(sanitize(gr));
            }
            return result;
        } catch (Exception e) {
            throw new LlmParseException("Failed to deserialize LLM response: " + e.getMessage(), e);
        }
    }

    /**
     * LLM이 options를 {"A","B","C","D"} 처럼 콜론 없는 객체로 출력하는 경우
     * 파싱 전에 {} 로 교체해 Jackson 오류를 방지한다.
     * 정상적인 {"choices":[...]} 형태는 콜론이 있으므로 건드리지 않는다.
     */
    private String fixMalformedOptions(String json) {
        Matcher m = MALFORMED_OPTIONS_PATTERN.matcher(json);
        return m.replaceAll(mr -> mr.group(1).contains(":") ? mr.group() : "\"options\": {}");
    }

    /**
     * type 또는 question이 null인 불완전한 퀴즈 항목을 제거한다.
     * LLM이 하나의 퀴즈 객체를 두 개로 쪼갠 경우에 발생한다.
     */
    private GenerationResult sanitize(GenerationResult result) {
        if (result.quizzes() == null) return result;
        var valid = result.quizzes().stream()
                .filter(q -> q.type() != null && q.question() != null)
                .toList();
        if (valid.size() != result.quizzes().size()) {
            log.warn("Filtered {} malformed quiz entries (null type/question)", result.quizzes().size() - valid.size());
        }
        return new GenerationResult(result.content(), result.candidates(), result.vocabularies(), valid, result.sourceDigest());
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
     * 첫 번째 '{' 위치에서 시작하여 JSON 범위를 추출한다.
     * { [ 를 스택으로 추적하고, 문자열 내부의 괄호는 무시한다.
     * LLM이 응답을 중간에 잘랐을 경우 닫히지 않은 괄호를 자동으로 닫아 best-effort 복구를 시도한다.
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        if (start == -1) {
            throw new LlmParseException("No JSON object found in LLM response");
        }

        Deque<Character> stack = new ArrayDeque<>();
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

            if (c == '{' || c == '[') {
                stack.push(c);
            } else if (c == '}' || c == ']') {
                if (!stack.isEmpty()) stack.pop();
                if (stack.isEmpty()) {
                    return text.substring(start, i + 1);
                }
            }
        }

        // LLM이 컨텍스트 한계로 응답을 잘라낸 경우 — 닫히지 않은 괄호를 채워 복구 시도
        log.warn("LLM response appears truncated (unclosed depth={}). Attempting best-effort JSON repair.", stack.size());
        StringBuilder repaired = new StringBuilder(text.substring(start));
        if (inString) {
            repaired.append('"');
        }
        while (!stack.isEmpty()) {
            repaired.append(stack.pop() == '{' ? '}' : ']');
        }
        return repaired.toString();
    }
}
