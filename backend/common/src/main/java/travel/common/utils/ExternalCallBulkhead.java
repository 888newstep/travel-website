package travel.common.utils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 第三方同步调用并发隔离（舱壁），通过信号量限制对同一供应商的最大并发调用数。
 */
public class ExternalCallBulkhead {

    private final String name;
    private final Semaphore semaphore;
    private final long acquireTimeoutMillis;

    public ExternalCallBulkhead(String name, int maxConcurrent, long acquireTimeoutMillis) {
        this.name = name;
        this.semaphore = new Semaphore(maxConcurrent, true);
        this.acquireTimeoutMillis = acquireTimeoutMillis;
    }

    /**
     * 在 acquireTimeoutMillis 内获取一个调用许可，返回 AutoCloseable 许可，
     * 需配合 try-with-resources 释放。
     */
    public Permit acquire() {
        try {
            if (!semaphore.tryAcquire(acquireTimeoutMillis, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(
                        "Bulkhead [" + name + "] busy, no permit acquired within "
                                + acquireTimeoutMillis + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while acquiring bulkhead [" + name + "] permit", e);
        }
        return new Permit(semaphore);
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public String getName() {
        return name;
    }

    /**
     * 调用许可，关闭时释放信号量。
     */
    public static final class Permit implements AutoCloseable {

        private final Semaphore semaphore;
        private volatile boolean released;

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                semaphore.release();
            }
        }
    }
}
