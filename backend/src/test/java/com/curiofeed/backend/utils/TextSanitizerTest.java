package com.curiofeed.backend.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextSanitizerTest {

    @Test
    void removesVocabMarkersAndLiteralNewlines() {
        String input = "Quantum uses {{qubits}}.\\n\\nIt solves {{ complex }} problems.\r\nNext line.";
        String expected = "Quantum uses qubits. It solves complex problems. Next line.";
        assertEquals(expected, TextSanitizer.sanitizeForPreview(input, 200));
    }

    @Test
    void removesSimpleVocabMarker() {
        assertEquals("qubits", TextSanitizer.sanitizeForPreview("{{qubits}}", 100));
    }

    @Test
    void removesMarkerWithInternalSpaces() {
        assertEquals("complex", TextSanitizer.sanitizeForPreview("{{ complex }}", 100));
    }

    @Test
    void replacesLiteralBackslashN() {
        String input = "Line one.\\n\\nLine two.";
        String result = TextSanitizer.sanitizeForPreview(input, 100);
        assertFalse(result.contains("\\n"), "Literal \\n must be removed");
        assertTrue(result.contains("Line one."), "First line must remain");
        assertTrue(result.contains("Line two."), "Second line must remain");
    }

    @Test
    void replacesActualNewline() {
        String input = "Line one.\n\nLine two.";
        String result = TextSanitizer.sanitizeForPreview(input, 100);
        assertFalse(result.contains("\n"), "Actual newline must be removed");
    }

    @Test
    void collapsesMultipleSpaces() {
        assertEquals("a b c", TextSanitizer.sanitizeForPreview("a  b   c", 100));
    }

    @Test
    void truncatesAtWordBoundary() {
        String input = "This is a very long sentence that should be truncated eventually at some point.";
        String result = TextSanitizer.sanitizeForPreview(input, 20);
        assertTrue(result.endsWith("..."), "Truncated result must end with ...");
        assertTrue(result.length() <= 23, "Result must not exceed maxLength + 3");
    }

    @Test
    void handlesPlainTextWithNoMarkers() {
        assertEquals("Just normal text.", TextSanitizer.sanitizeForPreview("Just normal text.", 100));
    }

    @Test
    void handlesNullInput() {
        assertEquals("", TextSanitizer.sanitizeForPreview(null, 100));
    }

    @Test
    void handlesBlankInput() {
        assertEquals("", TextSanitizer.sanitizeForPreview("   ", 100));
    }
}
