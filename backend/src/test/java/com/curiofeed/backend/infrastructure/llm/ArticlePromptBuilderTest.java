package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticlePromptBuilderTest {

    private ArticlePromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ArticlePromptBuilder();
    }

    @Test
    @DisplayName("EASY 레벨로 build 시 'EASY' 문자열 및 원문이 포함된다")
    void build_easyLevel_containsEasyAndContent() {
        String content = "This is the original article content.";
        String prompt = builder.build(content, DifficultyLevel.EASY);

        assertThat(prompt).contains("EASY");
        assertThat(prompt).contains(content);
    }

    @Test
    @DisplayName("HARD 레벨로 build 시 'HARD'가 포함된다")
    void build_hardLevel_containsHard() {
        String prompt = builder.build("Some content", DifficultyLevel.HARD);
        assertThat(prompt).contains("HARD");
    }

    @Test
    @DisplayName("결과 프롬프트에 'Do NOT include' 문구가 포함된다")
    void build_containsDoNotInclude() {
        String prompt = builder.build("Some content", DifficultyLevel.MEDIUM);
        assertThat(prompt).contains("Do NOT include");
    }

    @Test
    @DisplayName("생성 프롬프트에 3~4 문단 지침이 포함된다")
    void build_containsParagraphGuideline() {
        String prompt = builder.build("Some content", DifficultyLevel.EASY);
        assertThat(prompt).contains("3 to 4 natural paragraphs");
    }

    @Test
    @DisplayName("생성 프롬프트에 빈 줄 구분 지침이 포함된다")
    void build_containsBlankLineSeparation() {
        String prompt = builder.build("Some content", DifficultyLevel.MEDIUM);
        assertThat(prompt).contains("Separate paragraphs with a blank line");
    }

    @Test
    @DisplayName("생성 프롬프트에 bullet list 금지 지침이 포함된다")
    void build_containsBulletListProhibition() {
        String prompt = builder.build("Some content", DifficultyLevel.HARD);
        assertThat(prompt).contains("Do not use bullet points, numbered lists");
    }
}
