package com.curiofeed.backend.utils;

import java.util.regex.Pattern;

public class TextSanitizer {

    private static final Pattern VOCAB_MARKER = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern NEWLINE_OR_ESCAPE = Pattern.compile("\\\\n|\\n");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    /**
     * Sanitizes and truncates text for previews.
     * 1. Removes {{word}} markers -> word
     * 2. Replaces newlines/escaped newlines with space
     * 3. Collapses multiple spaces
     * 4. Trims
     * 5. Truncates to maxLength with "..."
     */
    public static String sanitizeForPreview(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1. {{word}} -> word
        String result = VOCAB_MARKER.matcher(text).replaceAll("$1");

        // 2. \n or \\n -> space
        result = NEWLINE_OR_ESCAPE.matcher(result).replaceAll(" ");

        // 3. Multi-space collapse
        result = MULTI_SPACE.matcher(result).replaceAll(" ");

        // 4. Trim
        result = result.trim();

        // 5. Truncate
        if (result.length() <= maxLength) {
            return result;
        }

        // Try to truncate at word boundary
        int end = maxLength;
        int lastSpace = result.lastIndexOf(' ', maxLength);
        if (lastSpace > maxLength * 0.8) {
            end = lastSpace;
        }
        
        return result.substring(0, end).trim() + "...";
    }
}
