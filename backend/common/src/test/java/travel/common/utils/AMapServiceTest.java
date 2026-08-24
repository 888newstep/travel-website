package travel.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AMapServiceTest {

    @Test
    void shouldRemoveApiKeyFromExceptionMessage() {
        String apiKey = "local-test-key";
        AMapService service = new AMapService(
                new AICacheManager(new CacheUtil(Optional.empty())),
                new ExternalCallBulkheadRegistry(50, 4, 1, 1));
        ReflectionTestUtils.setField(service, "apiKey", apiKey);

        String message = ReflectionTestUtils.invokeMethod(
                service,
                "sanitizeExceptionMessage",
                new IOException("request failed: https://restapi.amap.com/v3/direction/driving?key="
                        + apiKey + "&origin=116.3,39.9"));

        assertFalse(message.contains(apiKey));
        assertEquals(
                "request failed: https://restapi.amap.com/v3/direction/driving?key=***&origin=116.3,39.9",
                message);
    }
}
