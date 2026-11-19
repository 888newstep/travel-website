package travel.route.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiQuotaInterceptorTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private AiQuotaInterceptor interceptor;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        interceptor = new AiQuotaInterceptor(redisTemplate, 3);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                42L, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest aiPost() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ai/chat");
        return request;
    }

    @Test
    void shouldConsumeQuotaAndPassWhenBelowLimit() {
        when(valueOps.increment(anyString())).thenReturn(1L, 2L, 3L);

        for (int i = 0; i < 3; i++) {
            assertTrue(interceptor.preHandle(aiPost(), response, new Object()));
        }
        verify(redisTemplate).expire(org.mockito.ArgumentMatchers.startsWith(AiQuotaInterceptor.QUOTA_KEY_PREFIX), eq(Duration.ofDays(1)));
    }

    @Test
    void shouldRejectWhenQuotaExceeded() {
        when(valueOps.increment(anyString())).thenReturn(4L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(aiPost(), response, new Object()));

        assertEquals(ErrorCodeEnum.AI_DAILY_QUOTA_EXCEEDED.getCode(), ex.getCode());
    }

    @Test
    void shouldSkipAnonymousAndNonPostRequests() {
        SecurityContextHolder.clearContext();
        assertTrue(interceptor.preHandle(aiPost(), response, new Object()));
        verify(redisTemplate, never()).opsForValue();

        assertTrue(interceptor.preHandle(new MockHttpServletRequest("GET", "/ai/assistant/attraction-intro/1"), response, new Object()));
        verify(redisTemplate, never()).opsForValue();
    }
}
