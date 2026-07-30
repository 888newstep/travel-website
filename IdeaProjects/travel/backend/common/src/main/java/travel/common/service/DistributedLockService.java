package travel.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.utils.CacheUtil;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final CacheUtil cacheUtil;
    private static final long DEFAULT_LOCK_TIME = 5000;

    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        String requestId = UUID.randomUUID().toString();
        boolean locked = false;

        try {
            locked = cacheUtil.tryLock(
                    CacheUtil.generateKey(CacheUtil.LOCK_KEY_PREFIX, lockKey),
                    requestId,
                    DEFAULT_LOCK_TIME
            );

            if (!locked) {
                log.warn("获取分布式锁失败: lockKey={}", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            log.debug("获取分布式锁成功: lockKey={}", lockKey);
            return action.get();

        } finally {
            if (locked) {
                boolean released = cacheUtil.releaseLock(
                        CacheUtil.generateKey(CacheUtil.LOCK_KEY_PREFIX, lockKey),
                        requestId
                );
                log.debug("释放分布式锁: lockKey={}, success={}", lockKey, released);
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
