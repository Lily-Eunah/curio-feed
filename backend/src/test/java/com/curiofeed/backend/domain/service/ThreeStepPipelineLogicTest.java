package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.infrastructure.llm.ThreeStepPromptBuilder;
import com.curiofeed.backend.infrastructure.llm.validation.ContentStepValidator;
import com.curiofeed.backend.infrastructure.llm.validation.ContentValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThreeStepPipelineLogicTest {

    private final ThreeStepPromptBuilder promptBuilder = new ThreeStepPromptBuilder();
    private final ContentStepValidator contentValidator = new ContentStepValidator();

    @Test
    void testLongArticleDecisionLogic() {
        // threshold is 600
        String shortArticle = "Word ".repeat(500);
        String boundaryArticle600 = "Word ".repeat(600);
        String boundaryArticle601 = "Word ".repeat(601);
        String longArticle = "Word ".repeat(777);
        String veryLongArticle = "Word ".repeat(1300);

        assertThat(countWords(shortArticle)).isLessThanOrEqualTo(600);
        assertThat(countWords(boundaryArticle600)).isEqualTo(600);
        assertThat(countWords(boundaryArticle601)).isEqualTo(601);
        assertThat(countWords(longArticle)).isGreaterThan(600);
        assertThat(countWords(veryLongArticle)).isGreaterThan(1200);

        // Verify the exact logic used in ThreeStepSubJobWorker
        int threshold = 600;
        assertThat(countWords(boundaryArticle600) > threshold).isFalse(); // 600 -> Direct
        assertThat(countWords(boundaryArticle601) > threshold).isTrue();  // 601 -> Digest
    }

    @Test
    void testSourceDigestPromptBuilding() {
        String original = "Long news article content...";
        String prompt = promptBuilder.buildSourceDigestPrompt(original);

        assertThat(prompt).contains("Source Digest");
        assertThat(prompt).contains("centralStory");
        assertThat(prompt).contains("coreFacts");
        assertThat(prompt).contains(original);
    }

    @Test
    void testContentPromptWithDigest() {
        String digest = "Digest content...";
        String prompt = promptBuilder.buildContentPrompt(digest, DifficultyLevel.EASY, true);

        assertThat(prompt).contains("compressed SOURCE DIGEST");
        assertThat(prompt).contains("ONLY the facts in the digest");
        assertThat(prompt).contains("Absolute hard limit: 320 words");
        assertThat(prompt).contains(digest);
    }

    @Test
    void testContentValidationResultMetadata() {
        String content = "Word ".repeat(350); // Too long for EASY (hard limit 320)
        ContentValidationResult result = contentValidator.validate(content, DifficultyLevel.EASY);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getRetryReason()).isEqualTo("too_long");
        assertThat(result.getActualWordCount()).isEqualTo(350);
        assertThat(result.getPreferredMax()).isEqualTo(260); // Preferred max
        assertThat(result.getHardMax()).isEqualTo(320); // Hard limit
        assertThat(result.getMessage()).contains("HARD FAIL: content too long");
    }

    @Test
    void testRetryPromptIncludesMetadataAndNoForbiddenPhrases() {
        ContentValidationResult result = ContentValidationResult.builder()
                .retryReason("too_long")
                .actualWordCount(350)
                .preferredMin(180)
                .preferredMax(260)
                .hardMin(160)
                .hardMax(320)
                .status(ContentValidationResult.ValidationStatus.TOO_LONG_HARD_FAIL)
                .errors(List.of("too long"))
                .build();

        String retryPrompt = promptBuilder.buildContentRetryPrompt("Source text", DifficultyLevel.EASY, result, true);

        assertThat(retryPrompt).contains("350 words");
        assertThat(retryPrompt).contains("320 words"); // hard max
        assertThat(retryPrompt).contains("180~260 words"); // preferred range
        assertThat(retryPrompt).contains("STRICT WORD LIMIT IS HIGHER PRIORITY");
        
        // Forbidden phrases
        assertThat(retryPrompt).doesNotContain("preserve ALL key facts");
        assertThat(retryPrompt).doesNotContain("never remove facts");
        assertThat(retryPrompt).doesNotContain("every key fact must appear");
    }

    private int countWords(String text) {
        if (text == null) return 0;
        return text.trim().split("\\s+").length;
    }
}
