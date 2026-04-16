package com.curiofeed.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.pipeline")
public record PipelineProperties(
        int maxRetryCount,
        int staleJobThresholdMinutes,
        long schedulerFixedDelayMs,
        int schedulerBatchSize
) {
}
