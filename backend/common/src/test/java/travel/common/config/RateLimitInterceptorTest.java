package travel.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import travel.common.utils.RateLimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    private RateLimiter rateLimiter;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(RateLimiter.class);
        interceptor = new RateLimitInterceptor(rateLimiter);
        ReflectionTestUtils.setField(interceptor, "enabled", true);
        ReflectionTestUtils.setField(interceptor, "requestsPerMinute", 1000);
        ReflectionTestUtils.setField(interceptor, "aiRequestsPerMinute", 30);
    }

    @Test
    void shouldFailClosedWhenAiQuotaCannotBeAcquired() throws Exception {
        when(rateLimiter.tryAcquire(contains("rate-limit:api"), eq(1000), anyLong()))
                .thenReturn(true);
        when(rateLimiter.tryAcquireStrict(contains("rate-limit:ai"), eq(30), anyLong()))
                .thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/assistant/chat");
        request.addHeader("X-Client-IP", "198.51.100.7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void shouldNotConsumeAiQuotaForOrdinaryApi() throws Exception {
        when(rateLimiter.tryAcquire(contains("rate-limit:api"), eq(1000), anyLong()))
                .thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/routes/8");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(rateLimiter, never()).tryAcquireStrict(contains("rate-limit:ai"), eq(30), anyLong());
    }
}
