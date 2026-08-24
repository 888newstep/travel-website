package travel.common.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PerformanceStageRecorder {

    public static final String METRIC_NAME = "travel.performance.stage";

    private final MeterRegistry meterRegistry;
    private final boolean enabled;

    public PerformanceStageRecorder(
            MeterRegistry meterRegistry,
            @Value("${travel.performance.stage-timing-enabled:false}") boolean enabled) {
        this.meterRegistry = meterRegistry;
        this.enabled = enabled;
        log.info("Performance stage recorder initialized: enabled={}", enabled);
    }

    public static PerformanceStageRecorder disabled() {
        return new PerformanceStageRecorder(null, false);
    }

    public long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    public void record(String stage, long startedAtNanos, String outcome) {
        if (!enabled || meterRegistry == null || startedAtNanos <= 0L) {
            return;
        }
        long durationNanos = Math.max(0L, System.nanoTime() - startedAtNanos);
        Timer.builder(METRIC_NAME)
                .description("Latency of explicitly instrumented travel service stages")
                .tag("stage", normalize(stage, "unknown"))
                .tag("outcome", normalize(outcome, "unknown"))
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
