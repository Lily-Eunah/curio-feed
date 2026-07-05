package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.GenerationStepType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer & In-process counters and latency for the 3-step generation pipeline.
 * Publishes Prometheus metrics via Micrometer and provides periodic summary logs.
 */
@Component
public class ThreeStepPipelineMetrics {

    private static final Logger log = LoggerFactory.getLogger(ThreeStepPipelineMetrics.class);

    private final MeterRegistry meterRegistry;

    private final Map<GenerationStepType, ByLevel> stepAttempts  = enumMap();
    private final Map<GenerationStepType, ByLevel> stepSuccesses = enumMap();
    private final Map<GenerationStepType, ByLevel> stepFails     = enumMap();
    private final Map<GenerationStepType, ByLevel> stepRetries   = enumMap();
    private final Map<GenerationStepType, ByLevel> stepLatencyMs = enumMap();

    private final ByLevel subJobCompletes  = new ByLevel();
    private final ByLevel subJobFails      = new ByLevel();
    private final ByLevel subJobLatencyMs  = new ByLevel();

    public ThreeStepPipelineMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ── Public recording API ──────────────────────────────────────────────────

    public void recordStepAttempt(GenerationStepType step, DifficultyLevel level) {
        stepAttempts.get(step).inc(level);
        Counter.builder("curiofeed.pipeline.step.attempts")
                .tag("step", step.name())
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .increment();
    }

    public void recordStepSuccess(GenerationStepType step, DifficultyLevel level, long durationMs) {
        stepSuccesses.get(step).inc(level);
        stepLatencyMs.get(step).add(level, durationMs);
        Counter.builder("curiofeed.pipeline.step.successes")
                .tag("step", step.name())
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .increment();
        Timer.builder("curiofeed.pipeline.step.duration")
                .tag("step", step.name())
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordStepFail(GenerationStepType step, DifficultyLevel level) {
        stepFails.get(step).inc(level);
        Counter.builder("curiofeed.pipeline.step.fails")
                .tag("step", step.name())
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .increment();
    }

    /** Corrective within-step LLM retry — not the same as an admin-triggered retry. */
    public void recordStepRetry(GenerationStepType step, DifficultyLevel level) {
        stepRetries.get(step).inc(level);
        Counter.builder("curiofeed.pipeline.step.retries")
                .tag("step", step.name())
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .increment();
    }

    public void recordSubJobComplete(DifficultyLevel level, long durationMs) {
        subJobCompletes.inc(level);
        subJobLatencyMs.add(level, durationMs);
        Counter.builder("curiofeed.pipeline.subjob.completes")
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .increment();
        Timer.builder("curiofeed.pipeline.subjob.duration")
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordSubJobFail(DifficultyLevel level) {
        subJobFails.inc(level);
        Counter.builder("curiofeed.pipeline.subjob.fails")
                .tag("difficulty", level.name())
                .register(meterRegistry)
                .increment();
    }

    // ── Periodic summary log ──────────────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    public void logSummary() {
        boolean any = false;
        for (DifficultyLevel l : DifficultyLevel.values()) {
            if (subJobCompletes.get(l) + subJobFails.get(l) > 0) { any = true; break; }
        }
        if (!any) return;

        for (DifficultyLevel level : DifficultyLevel.values()) {
            long complete = subJobCompletes.get(level);
            long fail     = subJobFails.get(level);
            long total    = complete + fail;
            if (total == 0) continue;

            double sr    = (double) complete / total * 100;
            long avgMs   = complete > 0 ? subJobLatencyMs.get(level) / complete : 0;
            log.info("[3StepMetrics] level={} subJobs={} successRate={}% avgMs={}",
                    level, total, String.format("%.1f", sr), avgMs);

            for (GenerationStepType step : GenerationStepType.values()) {
                long att = stepAttempts.get(step).get(level);
                long suc = stepSuccesses.get(step).get(level);
                long fai = stepFails.get(step).get(level);
                long ret = stepRetries.get(step).get(level);
                if (att == 0) continue;
                long avgStepMs = suc > 0 ? stepLatencyMs.get(step).get(level) / suc : 0;
                log.info("[3StepMetrics]   step={} level={} attempts={} success={} fails={} intraRetries={} avgMs={}",
                        step, level, att, suc, fai, ret, avgStepMs);
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static Map<GenerationStepType, ByLevel> enumMap() {
        Map<GenerationStepType, ByLevel> m = new EnumMap<>(GenerationStepType.class);
        for (GenerationStepType s : GenerationStepType.values()) m.put(s, new ByLevel());
        return m;
    }

    private static final class ByLevel {
        private final Map<DifficultyLevel, AtomicLong> counters = new EnumMap<>(DifficultyLevel.class);

        ByLevel() {
            for (DifficultyLevel l : DifficultyLevel.values()) counters.put(l, new AtomicLong());
        }

        void inc(DifficultyLevel level)             { counters.get(level).incrementAndGet(); }
        void add(DifficultyLevel level, long delta) { counters.get(level).addAndGet(delta); }
        long get(DifficultyLevel level)             { return counters.get(level).get(); }
    }
}
