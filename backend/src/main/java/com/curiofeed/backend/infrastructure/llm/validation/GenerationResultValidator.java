package com.curiofeed.backend.infrastructure.llm.validation;

import com.curiofeed.backend.domain.model.GenerationResult;
import com.curiofeed.backend.infrastructure.llm.QualityScorer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GenerationResultValidator {

    private final StructureValidator structureValidator;
    private final ContentValidator contentValidator;
    private final LearningValidator learningValidator;
    private final QualityScorer qualityScorer;

    public GenerationResultValidator(
            StructureValidator structureValidator,
            ContentValidator contentValidator,
            LearningValidator learningValidator,
            QualityScorer qualityScorer) {
        this.structureValidator = structureValidator;
        this.contentValidator = contentValidator;
        this.learningValidator = learningValidator;
        this.qualityScorer = qualityScorer;
    }

    public ValidationResult validate(GenerationResult result) {
        List<String> allErrors = new ArrayList<>();

        // Stage 1: Structure — hard gate. 실패 시 score 계산 없이 즉시 반환
        List<String> structureErrors = structureValidator.check(result);
        if (!structureErrors.isEmpty()) {
            allErrors.addAll(structureErrors);
            return ValidationResult.fail(allErrors, 0.0);
        }

        // Stage 2: Content — soft warnings
        allErrors.addAll(contentValidator.check(result));

        // Stage 3: Learning — hard(quiz type/MCQ choices) + soft(vocab, duplicates 등)
        List<String> learningErrors = learningValidator.check(result);
        allErrors.addAll(learningErrors);

        double score = qualityScorer.score(result);

        if (learningValidator.isHardFail(learningErrors)) {
            return ValidationResult.fail(allErrors, score);
        }

        return ValidationResult.pass(score);
    }
}
