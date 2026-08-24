package travel.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private HttpIdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new HttpIdempotencyService(redisTemplate, new ObjectMapper(), 300, 3600);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldDeleteCorruptedCompletedRecordAndClaimAgain() {
        HttpIdempotencyService.RequestMetadata metadata =
                new HttpIdempotencyService.RequestMetadata("42", "POST", "/items", "fingerprint");
        String redisKey = service.keyFor("42", "request-1");
        when(valueOperations.get(redisKey)).thenReturn("corrupted-json");
        doReturn(0L, 1L, 1L).when(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                any(Object[].class));

        HttpIdempotencyService.ClaimResult result = service.tryClaim("42", "request-1", metadata);

        assertEquals(HttpIdempotencyService.ClaimStatus.CLAIMED, result.status());
        assertEquals(metadata, result.request());
        verify(valueOperations).get(redisKey);
    }

    @Test
    void shouldKeepValidProcessingRecordLocked() {
        HttpIdempotencyService.RequestMetadata metadata =
                new HttpIdempotencyService.RequestMetadata("42", "POST", "/items", "fingerprint");
        String redisKey = service.keyFor("42", "request-2");
        when(valueOperations.get(redisKey)).thenReturn(HttpIdempotencyService.PROCESSING + ":another-token");
        doReturn(0L).when(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                any(Object[].class));

        HttpIdempotencyService.ClaimResult result = service.tryClaim("42", "request-2", metadata);

        assertEquals(HttpIdempotencyService.ClaimStatus.IN_PROGRESS, result.status());
        assertNull(result.existing());
    }
}
