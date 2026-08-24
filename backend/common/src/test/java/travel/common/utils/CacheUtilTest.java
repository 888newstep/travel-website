package travel.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheUtilTest {

    @Test
    void shouldConsumeMatchingValueOnlyOnce() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        CacheUtil cacheUtil = new CacheUtil(Optional.of(redisTemplate));
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(Collections.singletonList("captcha:13800138000")),
                eq("123456")))
                .thenReturn(1L, 0L);

        assertTrue(cacheUtil.consumeIfEquals("captcha:13800138000", "123456"));
        assertFalse(cacheUtil.consumeIfEquals("captcha:13800138000", "123456"));
    }

    @Test
    void shouldFailClosedWhenRedisIsUnavailable() {
        CacheUtil cacheUtil = new CacheUtil(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> cacheUtil.consumeIfEquals("captcha:13800138000", "123456"));
    }
}
