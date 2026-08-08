package com.curiofeed.backend.infrastructure.llm.validation.safety;

import java.util.List;

public interface SafetyFilter {
    /**
     * Checks content against safety rules.
     * Returns a list of error/warning messages. Empty list means passed.
     */
    List<String> check(String content);
}
