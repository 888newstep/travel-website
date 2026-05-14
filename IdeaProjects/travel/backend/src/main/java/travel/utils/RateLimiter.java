package travel.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final CacheUtil cacheUtil;

    /**
     * 滑动窗口限流
     *
     * @param key 限流key
     * @param limit 限制次数
     * @param windowSize 时间窗口（秒）
     * @return 是否允许通过
     */
    public boolean tryAcquire(String key, int limit, long windowSize) {
        try {
            long currentTime = System.currentTimeMillis();
            String windowKey = key + ":" + (currentTime / 1000 / windowSize);

            Long count = cacheUtil.increment(windowKey, 1);

            if (count == 1) {
                cacheUtil.set(windowKey, count, windowSize, TimeUnit.SECONDS);
            }

            boolean allowed = count <= limit;

            if (!allowed) {
                log.warn("限流触发: key={}, count={}, limit={}", key, count, limit);
            }

            return allowed;
        } catch (Exception e) {
            log.error("限流检查失败: key={}, error={}", key, e.getMessage(), e);
            return true;
        }
    }

    /**
     * 令牌桶限流
     */
    public boolean tryAcquireToken(String key, int maxTokens, long refillTime) {
        try {
            String tokenKey = key + ":tokens";
            String lastRefillKey = key + ":lastRefill";

            Long tokens = cacheUtil.getCount(tokenKey);
            Long lastRefill = cacheUtil.getCount(lastRefillKey);
            long currentTime = System.currentTimeMillis();

            if (lastRefill == null || (currentTime - lastRefill) > refillTime * 1000) {
                cacheUtil.set(tokenKey, maxTokens, refillTime * 2, TimeUnit.SECONDS);
                cacheUtil.set(lastRefillKey, currentTime, refillTime * 2, TimeUnit.SECONDS);
                tokens = (long) maxTokens;
            }

            if (tokens > 0) {
                cacheUtil.decrement(tokenKey, 1);
                return true;
            }

            log.warn("令牌桶限流触发: key={}, tokens={}", key, tokens);
            return false;
        } catch (Exception e) {
            log.error("令牌桶限流检查失败: key={}, error={}", key, e.getMessage(), e);
            return true;
        }
    }
}
