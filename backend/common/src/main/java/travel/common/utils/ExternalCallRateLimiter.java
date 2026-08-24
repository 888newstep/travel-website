package travel.common.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 进程内平滑限流器，用于保护有明确 QPS 配额的第三方接口。
 */
public final class ExternalCallRateLimiter {

    private final long intervalNanos;
    private final long maxWaitNanos;
    private long nextPermitNanos;

    public ExternalCallRateLimiter(int permitsPerSecond, long maxWaitMillis) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive");
        }
        if (maxWaitMillis < 0) {
            throw new IllegalArgumentException("maxWaitMillis must not be negative");
        }
        this.intervalNanos = TimeUnit.SECONDS.toNanos(1) / permitsPerSecond;
        this.maxWaitNanos = TimeUnit.MILLISECONDS.toNanos(maxWaitMillis);
    }

    public void acquire() {
        long waitNanos;
        synchronized (this) {
            long now = System.nanoTime();
            long permitAt = Math.max(now, nextPermitNanos);
            waitNanos = permitAt - now;
            if (waitNanos > maxWaitNanos) {
                throw new IllegalStateException("External API rate limit queue is full");
            }
            nextPermitNanos = permitAt + intervalNanos;
        }
        if (waitNanos > 0) {
            LockSupport.parkNanos(waitNanos);
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Interrupted while waiting for external API rate limit");
            }
        }
    }
}
