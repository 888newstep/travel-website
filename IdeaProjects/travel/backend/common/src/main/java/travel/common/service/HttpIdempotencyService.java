package travel.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/** Redis-backed storage for authenticated HTTP idempotency records. */
@Service
public class HttpIdempotencyService {

    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    private static final String PROCESSING_PREFIX = PROCESSING + ":";
    private static final String KEY_PREFIX = "travel:http:idempotency:";

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 0 then "
                    + "redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]) "
                    + "return 1 else return 0 end", Long.class);
    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "redis.call('set', KEYS[1], ARGV[2], 'PX', ARGV[3]) "
                    + "return 1 else return 0 end", Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration processingTtl;
    private final Duration completedTtl;

    @Autowired
    public HttpIdempotencyService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${travel.http.idempotency.processing-ttl-seconds:300}") long processingTtlSeconds,
            @Value("${travel.http.idempotency.completed-ttl-seconds:259200}") long completedTtlSeconds) {
        if (processingTtlSeconds <= 0 || completedTtlSeconds <= 0) {
            throw new IllegalArgumentException("HTTP idempotency TTL must be positive");
        }
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processingTtl = Duration.ofSeconds(processingTtlSeconds);
        this.completedTtl = Duration.ofSeconds(completedTtlSeconds);
    }

    public ClaimResult tryClaim(String scope, String idempotencyKey, RequestMetadata request) {
        requireText(scope, "scope");
        requireText(idempotencyKey, "idempotencyKey");
        if (request == null) {
            throw new IllegalArgumentException("request metadata cannot be null");
        }

        String key = keyFor(scope, idempotencyKey);
        String token = UUID.randomUUID().toString();
        String processingValue = PROCESSING_PREFIX + token;
        Long claimed = claim(key, processingValue);
        if (Long.valueOf(1L).equals(claimed)) {
            return new ClaimResult(ClaimStatus.CLAIMED, key, token, request, null);
        }

        String existing = redisTemplate.opsForValue().get(key);
        if (existing == null) {
            claimed = claim(key, processingValue);
            if (Long.valueOf(1L).equals(claimed)) {
                return new ClaimResult(ClaimStatus.CLAIMED, key, token, request, null);
            }
            existing = redisTemplate.opsForValue().get(key);
        }
        if (existing != null && !existing.startsWith(PROCESSING_PREFIX)) {
            StoredRecord record = readRecord(existing);
            if (record != null && COMPLETED.equals(record.state())) {
                return new ClaimResult(ClaimStatus.COMPLETED, key, null, request, record);
            }
        }
        return new ClaimResult(ClaimStatus.IN_PROGRESS, key, null, request, null);
    }

    private Long claim(String key, String processingValue) {
        return redisTemplate.execute(
                CLAIM_SCRIPT,
                Collections.singletonList(key),
                processingValue,
                String.valueOf(processingTtl.toMillis()));
    }

    public boolean complete(ClaimResult claim, StoredResponse response) {
        requireClaimed(claim);
        if (response == null) {
            throw new IllegalArgumentException("stored response cannot be null");
        }
        StoredRecord record = new StoredRecord(
                COMPLETED,
                claim.request().scope(),
                claim.request().method(),
                claim.request().path(),
                claim.request().fingerprint(),
                response);
        final String serialized;
        try {
            serialized = objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize HTTP idempotency response", e);
        }
        Long completed = redisTemplate.execute(
                COMPLETE_SCRIPT,
                Collections.singletonList(claim.key()),
                PROCESSING_PREFIX + claim.token(),
                serialized,
                String.valueOf(completedTtl.toMillis()));
        return Long.valueOf(1L).equals(completed);
    }

    public boolean release(ClaimResult claim) {
        if (claim == null || claim.status() != ClaimStatus.CLAIMED || claim.token() == null) {
            return false;
        }
        Long released = redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(claim.key()),
                PROCESSING_PREFIX + claim.token());
        return Long.valueOf(1L).equals(released);
    }

    public String keyFor(String scope, String idempotencyKey) {
        requireText(scope, "scope");
        requireText(idempotencyKey, "idempotencyKey");
        return KEY_PREFIX + sha256(scope + '\u0000' + idempotencyKey);
    }

    private StoredRecord readRecord(String value) {
        try {
            return objectMapper.readValue(value, StoredRecord.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void requireClaimed(ClaimResult claim) {
        if (claim == null || claim.status() != ClaimStatus.CLAIMED
                || claim.token() == null || claim.request() == null) {
            throw new IllegalArgumentException("only a claimed request can be completed");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public enum ClaimStatus {
        CLAIMED,
        COMPLETED,
        IN_PROGRESS
    }

    public record RequestMetadata(String scope, String method, String path, String fingerprint) {
    }

    public record StoredResponse(int status, String contentType, String bodyBase64) {
    }

    public record StoredRecord(
            String state,
            String scope,
            String method,
            String path,
            String fingerprint,
            StoredResponse response) {
    }

    public record ClaimResult(
            ClaimStatus status,
            String key,
            String token,
            RequestMetadata request,
            StoredRecord existing) {
    }
}
