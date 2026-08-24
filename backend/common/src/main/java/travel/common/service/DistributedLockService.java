package travel.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import travel.common.utils.CacheUtil;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    @Value("${redisson.lock.wait-time-seconds:3}")
    private long waitTimeSeconds;

    @Value("${redisson.lock.lease-time-seconds:15}")
    private long leaseTimeSeconds;

    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        String fullLockKey = CacheUtil.generateKey(CacheUtil.LOCK_KEY_PREFIX, lockKey);
        RLock lock = redissonClient.getLock(fullLockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);

            if (!locked) {
                log.warn("Failed to acquire distributed lock: lockKey={}", lockKey);
                throw new RuntimeException("Failed to acquire distributed lock");
            }

            log.debug("Acquired distributed lock: lockKey={}", lockKey);
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring distributed lock", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released distributed lock: lockKey={}, success=true", lockKey);
            }
        }
    }

    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }
}


