package com.curiofeed.backend.config;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "ai.pipeline")
public record PipelineProperties(
        int maxRetryCount,
        int staleJobThresholdMinutes,
        long schedulerFixedDelayMs,
        int schedulerBatchSize,
        Map<String, Double> scoreThresholds
) {
    public double thresholdFor(DifficultyLevel level) {
        if (scoreThresholds == null) return 0.7;
        return scoreThresholds.getOrDefault(level.name(), 0.7);
    }
}
