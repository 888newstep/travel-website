package travel.route.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.security.AuthenticatedUserSupport;

import java.time.Duration;
import java.time.LocalDate;

/**
 * AI 接口用户日配额：按 userId + 自然日计数，防止登录用户刷外部模型成本。
 * 幂等/网关限流防的是"重复与洪峰"，本拦截器防的是"单用户日累计超预算"。
 * 计数使用 Redis INCR（原子），key 在次日自动过期。
 */
@Component
public class AiQuotaInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AiQuotaInterceptor.class);
    static final String QUOTA_KEY_PREFIX = "ai:quota:";

    private final StringRedisTemplate redisTemplate;
    private final int dailyQuota;

    public AiQuotaInterceptor(StringRedisTemplate redisTemplate,
                              @Value("${travel.ai.daily-quota:50}") int dailyQuota) {
        this.redisTemplate = redisTemplate;
        this.dailyQuota = Math.max(1, dailyQuota);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true; // 仅统计真正触发模型调用的写操作
        }
        Integer userId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        if (userId == null) {
            return true; // 匿名请求由鉴权层拒绝，不进入配额
        }
        long used = incrementAndExpire(userId);
        if (used > dailyQuota) {
            log.warn("AI 日配额超限: userId={}, used={}, quota={}", userId, used, dailyQuota);
            throw new BusinessException(ErrorCodeEnum.AI_DAILY_QUOTA_EXCEEDED);
        }
        return true;
    }

    private long incrementAndExpire(Integer userId) {
        String key = QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now();
        Long used = redisTemplate.opsForValue().increment(key);
        if (used != null && used == 1L) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        return used == null ? 0L : used;
    }
}
