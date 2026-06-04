package com.curiofeed.backend.infrastructure.llm.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TitleSimilarityValidatorTest {

    private TitleSimilarityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TitleSimilarityValidator();
    }

    @Test
    void isTooSimilar_whenTitleSharesSignificantPhrases_returnsTrue() {
        // "1,000 Times More Reliable" is shared almost verbatim
        String original  = "Microsoft says new quantum chip 1,000 times more reliable than predecessor";
        String generated = "Microsoft's Quantum Chip Breakthrough: 1,000 Times More Reliable";

        assertThat(validator.isTooSimilar(generated, original)).isTrue();
    }

    @Test
    void isTooSimilar_whenTitleUsesCompleteDifferentStructure_returnsFalse() {
        // Different framing, no shared bigrams
        String original  = "Martin Scorsese gets backlash after endorsing 'creatively freeing' AI";
        String generated = "Scorsese Embraces AI for Filmmaking Despite Industry Backlash";

        assertThat(validator.isTooSimilar(generated, original)).isFalse();
    }

    @Test
    void isTooSimilar_whenTitleChangesFromQuestionToStatement_returnsFalse() {
        String original  = "Can two hours of strength training a week reduce the risk of dying early?";
        String generated = "Weight Training for 90 Minutes Weekly Cuts Early Death Risk by 13%";

        assertThat(validator.isTooSimilar(generated, original)).isFalse();
    }

    @Test
    void bigramJaccard_identicalTitles_returnsOne() {
        assertThat(validator.bigramJaccard("same title here", "same title here")).isEqualTo(1.0);
    }

    @Test
    void bigramJaccard_completelyDifferentTitles_returnsZero() {
        assertThat(validator.bigramJaccard("hello world", "foo bar baz")).isEqualTo(0.0);
    }

    @Test
    void bigramJaccard_nullInputs_returnsZero() {
        assertThat(validator.bigramJaccard(null, "some title")).isEqualTo(0.0);
        assertThat(validator.bigramJaccard("some title", null)).isEqualTo(0.0);
    }

    @Test
    void bigramJaccard_singleWordTitles_returnsZero() {
        assertThat(validator.bigramJaccard("Microsoft", "Microsoft")).isEqualTo(0.0);
    }

    @Test
    void isTooSimilar_belowThreshold_returnsFalse() {
        // Shares one bigram out of many -> well below 0.30
        String original  = "Long original title with many unique words about technology and science";
        String generated = "technology and innovation reshape the modern world in surprising ways";

        assertThat(validator.isTooSimilar(generated, original)).isFalse();
    }
}
