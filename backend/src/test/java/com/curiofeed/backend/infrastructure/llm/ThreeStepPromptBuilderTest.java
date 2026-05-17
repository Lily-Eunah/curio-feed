package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThreeStepPromptBuilderTest {

    private ThreeStepPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ThreeStepPromptBuilder();
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 3~4 문단 지침이 포함된다")
    void buildContentPrompt_containsParagraphGuideline(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("3 to 4 natural paragraphs");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 빈 줄 구분 지침이 포함된다")
    void buildContentPrompt_containsBlankLineSeparation(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("Separate paragraphs with a blank line");
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("buildContentPrompt: 모든 난이도에 bullet list 금지 지침이 포함된다")
    void buildContentPrompt_containsBulletListProhibition(DifficultyLevel level) {
        String prompt = builder.buildContentPrompt("source text", level, false);
        assertThat(prompt).contains("Do not use bullet points, numbered lists");
    }

    @Test
    @DisplayName("buildContentPrompt: EASY 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_easyWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, false);
        assertThat(prompt).contains("180~260 words");
        assertThat(prompt).contains("320 words");
    }

    @Test
    @DisplayName("buildContentPrompt: MEDIUM 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_mediumWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.MEDIUM, false);
        assertThat(prompt).contains("220~320 words");
        assertThat(prompt).contains("380 words");
    }

    @Test
    @DisplayName("buildContentPrompt: HARD 난이도 word count 범위가 변경되지 않았다")
    void buildContentPrompt_hardWordCountUnchanged() {
        String prompt = builder.buildContentPrompt("source text", DifficultyLevel.HARD, false);
        assertThat(prompt).contains("280~420 words");
        assertThat(prompt).contains("500 words");
    }

    @Test
    @DisplayName("buildContentPrompt: digest 기반 플래그가 소스 컨텍스트 문구에 반영된다")
    void buildContentPrompt_digestBasedFlag() {
        String digestPrompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, true);
        String originalPrompt = builder.buildContentPrompt("source text", DifficultyLevel.EASY, false);

        assertThat(digestPrompt).contains("SOURCE DIGEST");
        assertThat(originalPrompt).contains("ORIGINAL ARTICLE");
    }
}
