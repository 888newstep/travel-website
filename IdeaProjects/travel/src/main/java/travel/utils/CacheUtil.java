package travel.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CacheUtil {

    private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

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
}
