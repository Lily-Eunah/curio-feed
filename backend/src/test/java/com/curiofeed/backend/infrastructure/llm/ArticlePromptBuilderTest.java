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
        String prompt = builder.buildContentPrompt(content, DifficultyLevel.EASY);

        assertThat(prompt).contains("EASY");
        assertThat(prompt).contains(content);
    }

    @Test
    @DisplayName("HARD 레벨로 build 시 'HARD'가 포함된다")
    void build_hardLevel_containsHard() {
        String prompt = builder.buildContentPrompt("Some content", DifficultyLevel.HARD);
        assertThat(prompt).contains("HARD");
    }

    @Test
    @DisplayName("결과 프롬프트에 출처 외 정보 금지 지침이 포함된다")
    void build_containsNoExtraInfoConstraint() {
        String prompt = builder.buildContentPrompt("Some content", DifficultyLevel.MEDIUM);
        assertThat(prompt).contains("Do not add information not present in the source");
    }

    @Test
    @DisplayName("생성 프롬프트에 문단 수 지침이 포함된다")
    void build_containsParagraphGuideline() {
        String prompt = builder.buildContentPrompt("Some content", DifficultyLevel.EASY);
        assertThat(prompt).contains("paragraphs");
    }

    @Test
    @DisplayName("생성 프롬프트에 문단 구분 지침이 포함된다")
    void build_containsParagraphSeparation() {
        String prompt = builder.buildContentPrompt("Some content", DifficultyLevel.MEDIUM);
        assertThat(prompt).contains("Separate paragraphs with");
    }

    @Test
    @DisplayName("생성 프롬프트에 bullet list 금지 지침이 포함된다")
    void build_containsBulletListProhibition() {
        String prompt = builder.buildContentPrompt("Some content", DifficultyLevel.HARD);
        assertThat(prompt).contains("bullets");
    }
}
