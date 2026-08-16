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
    @DisplayName("CopyrightPhraseDetector flags sustained verbatim reproduction")
    void testCopyrightPhraseDetector_flagsLongVerbatimRun() {
        CopyrightPhraseDetector detector = new CopyrightPhraseDetector();

        String original = "Researchers said the discovery could transform how coastal cities plan for rising sea levels.";
        String copied = "In the report, researchers said the discovery could transform how coastal cities plan for rising sea levels.";
        String paraphrased = "Scientists believe the finding may reshape flood planning along the coast.";

        assertThat(detector.check(copied, original)).hasSize(1);
        assertThat(detector.check(paraphrased, original)).isEmpty();
    }

    @Test
    @DisplayName("CopyrightPhraseDetector ignores runs made only of function words")
    void testCopyrightPhraseDetector_ignoresFunctionWordRuns() {
        CopyrightPhraseDetector detector = new CopyrightPhraseDetector();

        // "and there is more to it than that" is eight words of pure connective tissue.
        // Reusing it is not reproduction of protectable expression.
        String original = "The committee agreed and there is more to it than that in the final report.";
        String reusing = "Officials noted and there is more to it than that during the briefing.";

        assertThat(detector.check(reusing, original)).isEmpty();
    }

    @Test
    @DisplayName("CopyrightPhraseDetector tolerates an unavoidable proper-noun sequence")
    void testCopyrightPhraseDetector_tolerlatesShortUnavoidableOverlap() {
        CopyrightPhraseDetector detector = new CopyrightPhraseDetector();

        // An official body's name cannot be paraphrased away when reporting the same fact.
        String original = "The national institute for health and care excellence reviews new treatments every year.";
        String reporting = "Experts at the national institute for health and care excellence assess them carefully.";

        assertThat(detector.check(reporting, original)).isEmpty();
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
