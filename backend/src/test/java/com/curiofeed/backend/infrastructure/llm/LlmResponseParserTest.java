package com.curiofeed.backend.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmResponseParserTest {

    private LlmResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultLlmResponseParser(new ObjectMapper());
    }

    record SimpleDto(String name, int value) {}

    @Test
    @DisplayName("정상 JSON 응답을 파싱한다")
    void parse_cleanJson_success() {
        String raw = """
                {"name": "test", "value": 42}
                """;
        SimpleDto result = parser.parse(raw, SimpleDto.class);
        assertThat(result.name()).isEqualTo("test");
        assertThat(result.value()).isEqualTo(42);
    }

    @Test
    @DisplayName("```json 마크다운 블록이 포함된 응답을 파싱한다")
    void parse_markdownCodeBlock_success() {
        String raw = """
                ```json
                {"name": "markdown", "value": 7}
                ```
                """;
        SimpleDto result = parser.parse(raw, SimpleDto.class);
        assertThat(result.name()).isEqualTo("markdown");
        assertThat(result.value()).isEqualTo(7);
    }

    @Test
    @DisplayName("JSON 앞뒤에 불필요한 텍스트가 있어도 파싱한다")
    void parse_jsonWithSurroundingText_success() {
        String raw = "Here is the JSON: {\"name\": \"surrounded\", \"value\": 99} That's all.";
        SimpleDto result = parser.parse(raw, SimpleDto.class);
        assertThat(result.name()).isEqualTo("surrounded");
        assertThat(result.value()).isEqualTo(99);
    }

    @Test
    @DisplayName("완전히 깨진 JSON은 LlmParseException을 발생시킨다")
    void parse_brokenJson_throwsLlmParseException() {
        assertThatThrownBy(() -> parser.parse("no json here at all", SimpleDto.class))
                .isInstanceOf(LlmParseException.class);
    }

    @Test
    @DisplayName("JSON 문자열 값 안에 중첩 {} 포함 시 올바르게 파싱한다")
    void parse_nestedBracesInStringValue_success() {
        String raw = "{\"name\": \"value with {nested} braces\", \"value\": 1}";
        SimpleDto result = parser.parse(raw, SimpleDto.class);
        assertThat(result.name()).isEqualTo("value with {nested} braces");
        assertThat(result.value()).isEqualTo(1);
    }
}
