package travel.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalCallRateLimiterTest {

    @Test
    void shouldRejectWhenWaitQueueExceedsConfiguredLimit() {
        ExternalCallRateLimiter limiter = new ExternalCallRateLimiter(1, 0);

        assertDoesNotThrow(limiter::acquire);
        assertThrows(IllegalStateException.class, limiter::acquire);
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalCallRateLimiter(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new ExternalCallRateLimiter(3, -1));
    }
}
