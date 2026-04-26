package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Component
public class PipelineMetrics {

    private static final Logger log = LoggerFactory.getLogger(PipelineMetrics.class);

    private final AtomicLong totalAttempts = new AtomicLong();
    private final AtomicLong fallbackCount = new AtomicLong();
    private final AtomicLong qwenCount = new AtomicLong();
    private final Map<DifficultyLevel, AtomicLong> totalByLevel = new EnumMap<>(DifficultyLevel.class);
    private final Map<DifficultyLevel, AtomicLong> fallbackByLevel = new EnumMap<>(DifficultyLevel.class);
    private final DoubleAdder scoreSum = new DoubleAdder();
    private final AtomicLong scoreCount = new AtomicLong();

    public PipelineMetrics() {
        for (DifficultyLevel level : DifficultyLevel.values()) {
            totalByLevel.put(level, new AtomicLong());
            fallbackByLevel.put(level, new AtomicLong());
        }
    }

    public void recordAttempt(DifficultyLevel level, double score) {
        totalAttempts.incrementAndGet();
        totalByLevel.get(level).incrementAndGet();
        scoreSum.add(score);
        scoreCount.incrementAndGet();
    }

    public void recordFallback(DifficultyLevel level) {
        fallbackCount.incrementAndGet();
        fallbackByLevel.get(level).incrementAndGet();
    }

    public void recordQwenUsage() {
        qwenCount.incrementAndGet();
    }

    public double getFallbackRate() {
        long total = totalAttempts.get();
        return total == 0 ? 0.0 : (double) fallbackCount.get() / total;
    }

    public double getAverageScore() {
        long count = scoreCount.get();
        return count == 0 ? 0.0 : scoreSum.sum() / count;
    }

    @Scheduled(fixedDelay = 60_000)
    public void logSummary() {
        long total = totalAttempts.get();
        if (total == 0) return;

        log.info("[PipelineMetrics] total={} fallbackRate={}% avgScore={} qwenUsage={}%",
                total,
                String.format("%.1f", getFallbackRate() * 100),
                String.format("%.3f", getAverageScore()),
                String.format("%.1f", (double) qwenCount.get() / total * 100));

        for (DifficultyLevel level : DifficultyLevel.values()) {
            long levelTotal = totalByLevel.get(level).get();
            long levelFallback = fallbackByLevel.get(level).get();
            if (levelTotal > 0) {
                log.info("[PipelineMetrics] level={} total={} fallbackRate={}%",
                        level, levelTotal,
                        String.format("%.1f", (double) levelFallback / levelTotal * 100));
            }
        }
    }
}
