package travel.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import travel.common.utils.RateLimiter;

import java.nio.charset.StandardCharsets;

/**
 * API限流拦截器
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_SECONDS = 60;

    private final RateLimiter rateLimiter;

    @Value("${rate.limit.enabled:true}")
    private boolean enabled;

    @Value("${rate.limit.requests-per-minute:1000}")
    private int requestsPerMinute;

    @Value("${rate.limit.ai-requests-per-minute:30}")
    private int aiRequestsPerMinute;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
            return true;
        }
        String subject = resolveSubject(request);
        if (!rateLimiter.tryAcquire(
                "rate-limit:api:" + subject, requestsPerMinute, WINDOW_SECONDS)) {
            return reject(response);
        }
        if (isAiRequest(request) && !rateLimiter.tryAcquireStrict(
                "rate-limit:ai:" + subject, aiRequestsPerMinute, WINDOW_SECONDS)) {
            return reject(response);
        }
        return true;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String gatewayClientIp = request.getHeader("X-Client-IP");
        return gatewayClientIp == null || gatewayClientIp.isBlank()
                ? request.getRemoteAddr()
                : gatewayClientIp.trim();
    }

    private String resolveSubject(HttpServletRequest request) {
        return request.getUserPrincipal() == null
                ? "ip:" + getClientIp(request)
                : "user:" + request.getUserPrincipal().getName();
    }

    private boolean isAiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/ai") || uri.startsWith("/api/ai/")
                || uri.equals("/ai") || uri.startsWith("/ai/");
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
        response.getWriter().write(
                "{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\",\"code\":429}");
        return false;
    }
}
