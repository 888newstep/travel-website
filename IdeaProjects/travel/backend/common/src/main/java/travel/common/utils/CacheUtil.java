package travel.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class CacheUtil {

    private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheUtil(Optional<RedisTemplate<String, Object>> redisTemplate) {
        this.redisTemplate = redisTemplate.orElse(null);
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过缓存设置: key={}", key);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("设置缓存成功: key={}, timeout={}{}", key, timeout, unit.name());
        } catch (Exception e) {
            log.error("设置缓存失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 获取缓存
     */
    public <T> T get(String key, Class<T> clazz) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过缓存获取: key={}", key);
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("获取缓存成功: key={}", key);
                return clazz.cast(value);
            }
        } catch (Exception e) {
            log.error("获取缓存失败: key={}, error={}", key, e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过缓存删除: key={}", key);
            return;
        }
        try {
            redisTemplate.delete(key);
            log.debug("删除缓存成功: key={}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 缓存是否存在
     */
    public boolean exists(String key) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查缓存是否存在失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成缓存键
     */
    public static String generateKey(String prefix, Object... params) {
        StringBuilder key = new StringBuilder(prefix);
        for (Object param : params) {
            key.append(":").append(param);
        }
        return key.toString();
    }

    /**
     * 原子递增计数器
     */
    public Long increment(String key, long delta) {
        if (redisTemplate == null) {
            return 0L;
        }
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("递增计数器失败: key={}, error={}", key, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 原子递减计数器
     */
    public Long decrement(String key, long delta) {
        if (redisTemplate == null) {
            return 0L;
        }
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("递减计数器失败: key={}, error={}", key, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 获取计数器值
     */
    public Long getCount(String key) {
        if (redisTemplate == null) {
            return 0L;
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (Exception e) {
            log.error("获取计数器值失败: key={}, error={}", key, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 尝试获取分布式锁
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, requestId, expireTime, TimeUnit.MILLISECONDS);
            return result != null && result;
        } catch (Exception e) {
            log.error("获取分布式锁失败: lockKey={}, error={}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 释放分布式锁（使用Lua脚本保证原子性）
     */
    public boolean releaseLock(String lockKey, String requestId) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), requestId);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("释放分布式锁失败: lockKey={}, error={}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量删除缓存（支持通配符）
     */
    public void deleteByPattern(String pattern) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.keys(pattern).forEach(key -> {
                redisTemplate.delete(key);
                log.debug("删除缓存: key={}", key);
            });
        } catch (Exception e) {
            log.error("批量删除缓存失败: pattern={}, error={}", pattern, e.getMessage(), e);
        }
    }

    /**
     * 设置哈希缓存
     */
    public void hashSet(String key, String field, Object value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().put(key, field, value);
        } catch (Exception e) {
            log.error("设置哈希缓存失败: key={}, field={}, error={}", key, field, e.getMessage(), e);
        }
    }

    /**
     * 获取哈希缓存
     */
    public Object hashGet(String key, String field) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            log.error("获取哈希缓存失败: key={}, field={}, error={}", key, field, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 添加到有序集合
     */
    public void zAdd(String key, Object value, double score) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            log.error("添加到有序集合失败: key={}, error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 获取有序集合Top N
     */
    public java.util.Set<Object> zReverseRange(String key, long start, long end) {
        if (redisTemplate == null) {
            return java.util.Collections.emptySet();
        }
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.error("获取有序集合失败: key={}, error={}", key, e.getMessage(), e);
            return java.util.Collections.emptySet();
        }
    }

    // 缓存键前缀
    public static final String ATTRACTION_KEY_PREFIX = "attraction";
    public static final String ROUTE_KEY_PREFIX = "route";
    public static final String ROUTE_SHARE_KEY_PREFIX = "route_share";
    public static final String ROUTE_COMMENT_KEY_PREFIX = "route_comment";
    public static final String ROUTE_COLLECTION_KEY_PREFIX = "route_collection";
    public static final String USER_KEY_PREFIX = "user";
    public static final String TRANSPORT_KEY_PREFIX = "transport";
    public static final String CAPTCHA_KEY_PREFIX = "captcha";
    public static final String REALTIME_STATUS_KEY_PREFIX = "realtime_status";
    public static final String LOCK_KEY_PREFIX = "lock";
    public static final String COUNTER_KEY_PREFIX = "counter";
    public static final String HOT_KEY_PREFIX = "hot";
}
