package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.repository.ArticleGenerationStepJobRepository;
import com.curiofeed.backend.domain.repository.EvalScoreRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AbComparisonService {

    private final ArticleGenerationStepJobRepository stepJobRepository;
    private final EvalScoreRepository evalScoreRepository;

    public AbComparisonService(ArticleGenerationStepJobRepository stepJobRepository,
                                EvalScoreRepository evalScoreRepository) {
        this.stepJobRepository = stepJobRepository;
        this.evalScoreRepository = evalScoreRepository;
    }

    public Map<String, Object> compareVersions(String promptVersionA, String promptVersionB) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("promptVersionA", promptVersionA);
        result.put("promptVersionB", promptVersionB);

        // Fetch metrics / scores if available
        Double avgScoreA = evalScoreRepository.findAverageScoreByModel(promptVersionA);
        Double avgScoreB = evalScoreRepository.findAverageScoreByModel(promptVersionB);

        result.put("scoreA", avgScoreA != null ? avgScoreA : 0.0);
        result.put("scoreB", avgScoreB != null ? avgScoreB : 0.0);
        result.put("status", "ACTIVE_AB_COMPARISON");

        return result;
    }
}
