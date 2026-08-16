package travel.common.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * RabbitMQ 消费幂等的 Redis 快速路径。
 *
 * <p>PROCESSING 只保留较短租约，进程崩溃后允许消息重新处理；COMPLETED 保留
 * 三天用于过滤重复投递。业务库仍应使用 source_message_id 唯一约束作为最终兜底。</p>
 */
@Service
public class RedisMessageIdempotencyService {

    public static final String COMPLETED = "COMPLETED";
    private static final String PROCESSING_PREFIX = "PROCESSING:";
    private static final String KEY_PREFIX = "travel:mq:idempotency:notification:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration processingTtl;
    private final Duration completedTtl;

    @Autowired
    public RedisMessageIdempotencyService(
            StringRedisTemplate redisTemplate,
            @Value("${mq.reliable-notification.idempotency.processing-ttl-seconds:300}")
            long processingTtlSeconds,
            @Value("${mq.reliable-notification.idempotency.completed-ttl-seconds:259200}")
            long completedTtlSeconds) {
        this.redisTemplate = redisTemplate;
        if (processingTtlSeconds <= 0 || completedTtlSeconds <= 0) {
            throw new IllegalArgumentException("idempotency TTL must be positive");
        }
        this.processingTtl = Duration.ofSeconds(processingTtlSeconds);
        this.completedTtl = Duration.ofSeconds(completedTtlSeconds);
    }

    public ClaimResult tryClaim(String messageId) {
        String key = keyFor(messageId);
        String token = UUID.randomUUID().toString();
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(
                key,
                PROCESSING_PREFIX + token,
                processingTtl);
        if (Boolean.TRUE.equals(claimed)) {
            return new ClaimResult(ClaimStatus.CLAIMED, key, token);
        }

        String existing = redisTemplate.opsForValue().get(key);
        if (COMPLETED.equals(existing)) {
            return new ClaimResult(ClaimStatus.COMPLETED, key, null);
        }
        return new ClaimResult(ClaimStatus.IN_PROGRESS, key, null);
    }

    public void markCompleted(ClaimResult claim) {
        requireClaimed(claim);
        redisTemplate.opsForValue().set(claim.key(), COMPLETED, completedTtl);
    }

    public void release(ClaimResult claim) {
        if (claim == null || claim.status() != ClaimStatus.CLAIMED || claim.token() == null) {
            return;
        }
        redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(claim.key()),
                PROCESSING_PREFIX + claim.token());
    }

    public String keyFor(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId cannot be blank");
        }
        return KEY_PREFIX + messageId;
    }

    private void requireClaimed(ClaimResult claim) {
        if (claim == null || claim.status() != ClaimStatus.CLAIMED || claim.token() == null) {
            throw new IllegalArgumentException("only a claimed message can be completed");
        }
    }

    public enum ClaimStatus {
        CLAIMED,
        COMPLETED,
        IN_PROGRESS
    }

    public record ClaimResult(ClaimStatus status, String key, String token) {
    }
}
