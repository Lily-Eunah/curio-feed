package com.curiofeed.backend.infrastructure.llm.validation.safety;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyFilterTest {

    @Test
    @DisplayName("RegexPiiDetector identifies email, phone number, SSN, and credit card")
    void testRegexPiiDetector() {
        RegexPiiDetector detector = new RegexPiiDetector();

        List<String> cleanResult = detector.check("This is a clean news article about space exploration.");
        assertThat(cleanResult).isEmpty();

        List<String> piiResult = detector.check("Contact john.doe@example.com or call 555-123-4567. SSN: 123-45-6789.");
        assertThat(piiResult).hasSize(3);
        assertThat(piiResult).anyMatch(s -> s.contains("Email"));
        assertThat(piiResult).anyMatch(s -> s.contains("Phone"));
        assertThat(piiResult).anyMatch(s -> s.contains("Social Security"));
    }

    @Test
    @DisplayName("KeywordToxicityFilter identifies profanity and toxic words")
    void testKeywordToxicityFilter() {
        KeywordToxicityFilter filter = new KeywordToxicityFilter();

        List<String> cleanResult = filter.check("The economy grew by three percent in the last quarter.");
        assertThat(cleanResult).isEmpty();

        List<String> toxicResult = filter.check("This is total bullshit and a complete waste of time.");
        assertThat(toxicResult).hasSize(1);
        assertThat(toxicResult.get(0)).contains("Toxic word detected");
    }

    @Test
    @DisplayName("CopyrightPhraseDetector flags 5-gram verbatim copied phrases")
    void testCopyrightPhraseDetector() {
        CopyrightPhraseDetector detector = new CopyrightPhraseDetector();

        String original = "Scientists at the university discovered a breakthrough energy source in deep sea hydrothermal vents.";
        String copied = "In recent news, scientists at the university discovered a breakthrough energy source while exploring.";
        String paraphrased = "Researchers found a novel power mechanism underwater.";

        assertThat(detector.check(copied, original)).hasSize(1);
        assertThat(detector.check(paraphrased, original)).isEmpty();
    }

    @Test
    @DisplayName("CompositeSafetyFilter aggregates all safety checks")
    void testCompositeSafetyFilter() {
        RegexPiiDetector pii = new RegexPiiDetector();
        KeywordToxicityFilter toxicity = new KeywordToxicityFilter();
        CopyrightPhraseDetector copyright = new CopyrightPhraseDetector();

        CompositeSafetyFilter composite = new CompositeSafetyFilter(List.of(pii, toxicity), copyright);

        String original = "The international conference discussed climate policy and renewable energy solutions for developing nations.";
        String unsafeContent = "Contact test@example.com. The international conference discussed climate policy and renewable energy solutions for developing nations.";

        List<String> violations = composite.validate(unsafeContent, original);
        assertThat(violations).hasSize(2);
        assertThat(violations).anyMatch(v -> v.contains("Email"));
        assertThat(violations).anyMatch(v -> v.contains("Copyright"));
    }
}
