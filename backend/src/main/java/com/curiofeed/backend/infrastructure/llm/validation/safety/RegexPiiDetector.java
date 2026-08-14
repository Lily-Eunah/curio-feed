package com.curiofeed.backend.infrastructure.llm.validation.safety;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RegexPiiDetector implements SafetyFilter {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\+?1[-.\\s]?)?(?:\\(\\d{3}\\)|\\d{3})[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"
    );

    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35\\d{3})\\d{11})\\b"
    );

    @Override
    public List<String> check(String content) {
        List<String> violations = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return violations;
        }

        if (EMAIL_PATTERN.matcher(content).find()) {
            violations.add("PII Violation: Email address detected");
        }
        if (PHONE_PATTERN.matcher(content).find()) {
            violations.add("PII Violation: Phone number detected");
        }
        if (SSN_PATTERN.matcher(content).find()) {
            violations.add("PII Violation: Social Security Number (SSN) detected");
        }
        if (CREDIT_CARD_PATTERN.matcher(content).find()) {
            violations.add("PII Violation: Credit Card Number detected");
        }

        return violations;
    }
}
