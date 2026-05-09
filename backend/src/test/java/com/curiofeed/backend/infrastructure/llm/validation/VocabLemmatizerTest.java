package com.curiofeed.backend.infrastructure.llm.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VocabLemmatizerTest {

    private VocabLemmatizer lemmatizer;

    @BeforeEach
    void setUp() {
        lemmatizer = new VocabLemmatizer();
    }

    // ── Exact match ────────────────────────────────────────────────────────────

    @Test
    void exactBaseForm_found() {
        assertThat(lemmatizer.appearsInContent("surge", "the surge in prices")).isTrue();
    }

    @Test
    void notInContent_returnsFalse() {
        assertThat(lemmatizer.appearsInContent("escalate", "the steady decline in wages")).isFalse();
    }

    // ── Common inflections (experiment-observed failures) ────────────────────

    @ParameterizedTest(name = "base={0} content={1}")
    @CsvSource({
        "target,      'the vessel was targeted by gunfire'",
        "surge,       'oil prices surged above $100'",
        "restrict,    'the channel was restricted'",
        "escalate,    'tensions escalated sharply'",
        "constitute,  'this constituted a violation'",
        "depreciate,  'the yen depreciation continued'",
        "propel,      'growth was propelled by investment'",
        "announce,    'the minister announced the reopening'",
        "blockade,    'Iran enforced a blockade of ports'",
        "significant, 'a significantly higher price'",
    })
    void inflectedForm_matchesBase(String baseWord, String content) {
        // Remove surrounding quotes added by CsvSource for strings with commas
        String cleanContent = content.trim().replaceAll("^'|'$", "");
        assertThat(lemmatizer.appearsInContent(baseWord, cleanContent))
                .as("base='%s' should match in '%s'", baseWord, cleanContent)
                .isTrue();
    }

    // ── Noun derivations ──────────────────────────────────────────────────────

    @Test
    void depreciate_matchesDepreciation() {
        assertThat(lemmatizer.appearsInContent("depreciate",
                "the pronounced depreciation of the yen over two years")).isTrue();
    }

    @Test
    void escalate_matchesEscalation() {
        assertThat(lemmatizer.appearsInContent("escalate",
                "escalation of the conflict led to higher prices")).isTrue();
    }

    @Test
    void restrict_matchesRestriction() {
        assertThat(lemmatizer.appearsInContent("restrict",
                "the restriction on commercial vessels continued")).isTrue();
    }

    // ── Adverb form ──────────────────────────────────────────────────────────

    @Test
    void significant_matchesSignificantly() {
        assertThat(lemmatizer.appearsInContent("significant",
                "traffic has significantly decreased")).isTrue();
    }

    // ── Word boundary — must not match partial words ────────────────────────

    @Test
    void wordBoundary_doesNotMatchPartial() {
        // "target" should NOT match "restaurant" or "targeted" as a prefix of "targetedly"
        assertThat(lemmatizer.appearsInContent("target", "the restaurant was busy")).isFalse();
    }

    @Test
    void wordBoundary_matchesAtSentenceStart() {
        assertThat(lemmatizer.appearsInContent("surge", "Surge pricing applied")).isTrue();
    }

    @Test
    void wordBoundary_matchesAtSentenceEnd() {
        assertThat(lemmatizer.appearsInContent("surge", "prices continue to surge")).isTrue();
    }

    // ── Null / blank safety ──────────────────────────────────────────────────

    @Test
    void nullWord_returnsFalse() {
        assertThat(lemmatizer.appearsInContent(null, "some content")).isFalse();
    }

    @Test
    void blankWord_returnsFalse() {
        assertThat(lemmatizer.appearsInContent("  ", "some content")).isFalse();
    }

    // ── Normalization tests ───────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "targeted, target",
        "surged, surge",
        "restricted, restrict",
        "escalated, escalate",
        "constituted, constitute",
        "propelled, propel",
        "asserting, assert"
    })
    void normalizeDisplayWord_safelyTransformsSpecificWords(String input, String expected) {
        assertThat(lemmatizer.normalizeDisplayWord(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "depreciation, depreciation",
        "significantly, significantly",
        "inflation, inflation",
        "justification, justification"
    })
    void normalizeDisplayWord_preservesSpecificWords(String input, String expected) {
        assertThat(lemmatizer.normalizeDisplayWord(input)).isEqualTo(expected);
    }

    @Test
    void normalizeDisplayWord_returnsOriginalForOtherWords() {
        assertThat(lemmatizer.normalizeDisplayWord("unknown")).isEqualTo("unknown");
        assertThat(lemmatizer.normalizeDisplayWord("running")).isEqualTo("running"); // not in safe list
    }
}
