package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.model.GenerationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StructureValidator {

    private static final int REQUIRED_VOCAB_COUNT = 5;
    private static final int REQUIRED_QUIZ_COUNT = 3;

    public List<String> check(GenerationResult result) {
        List<String> errors = new ArrayList<>();

        if (result == null) {
            errors.add("result is null");
            return errors;
        }

        if (!result.hasContent()) {
            errors.add("content is blank or missing");
        }

        if (result.vocabularies() == null) {
            errors.add("vocabularies is null");
        } else if (result.vocabularies().size() != REQUIRED_VOCAB_COUNT) {
            errors.add("expected " + REQUIRED_VOCAB_COUNT + " vocabularies, got " + result.vocabularies().size());
        } else {
            for (int i = 0; i < result.vocabularies().size(); i++) {
                var v = result.vocabularies().get(i);
                if (isBlank(v.word())) errors.add("vocab[" + i + "].word is blank");
                if (isBlank(v.definition())) errors.add("vocab[" + i + "].definition is blank");
                if (isBlank(v.exampleSentence())) errors.add("vocab[" + i + "].exampleSentence is blank");
            }
        }

        if (result.quizzes() == null) {
            errors.add("quizzes is null");
        } else if (result.quizzes().size() != REQUIRED_QUIZ_COUNT) {
            errors.add("expected " + REQUIRED_QUIZ_COUNT + " quizzes, got " + result.quizzes().size());
        } else {
            for (int i = 0; i < result.quizzes().size(); i++) {
                var q = result.quizzes().get(i);
                if (q.type() == null) errors.add("quiz[" + i + "].type is null");
                if (isBlank(q.question())) errors.add("quiz[" + i + "].question is blank");
                if (isBlank(q.correctAnswer())) errors.add("quiz[" + i + "].correctAnswer is blank");
            }
        }

        return errors;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
