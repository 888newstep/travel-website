package travel.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API限流拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // 存储每个IP的请求次数
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    // 存储每个IP的最后请求时间
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    
    // 每分钟最大请求数
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        long currentTime = System.currentTimeMillis();
        
        // 清理过期的请求记录
        cleanupExpiredRecords(clientIp, currentTime);
        
        // 检查请求次数是否超过限制
        AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429); // 429 Too Many Requests
            response.getWriter().write("{\"success\": false, \"message\": \"请求过于频繁，请稍后再试\", \"code\": 429}");
            return false;
        }
        
        // 更新最后请求时间
        lastRequestTimes.put(clientIp, currentTime);
        
        return true;
    }

    /**
     * 清理过期的请求记录
     */
    private void cleanupExpiredRecords(String clientIp, long currentTime) {
        Long lastTime = lastRequestTimes.get(clientIp);
        if (lastTime != null && (currentTime - lastTime) > 60000) { // 60秒
            requestCounts.remove(clientIp);
            lastRequestTimes.remove(clientIp);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
