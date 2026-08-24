package travel.common.performance;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PerformanceStageRecorderTest {

    @Test
    void shouldRecordStageWithOutcomeTagsWhenEnabled() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PerformanceStageRecorder recorder = new PerformanceStageRecorder(meterRegistry, true);

        long startedAtNanos = recorder.start();
        recorder.record("collection.lookup", startedAtNanos, "missing");

        Timer timer = meterRegistry.find(PerformanceStageRecorder.METRIC_NAME)
                .tag("stage", "collection.lookup")
                .tag("outcome", "missing")
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }
}
