package travel.route.service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteFallbackSupportTest {

    private static final Logger log = LoggerFactory.getLogger(RouteFallbackSupportTest.class);

    @Test
    void shouldReturnEmptyListFallback() {
        List<Map<String, Object>> result = RouteFallbackSupport.emptyList(log, "fallback list", new RuntimeException("db error"));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnErrorResultFallback() {
        Map<String, Object> result = RouteFallbackSupport.errorResult(log, "fallback map", new RuntimeException("timeout"));

        assertEquals(false, result.get("success"));
        assertEquals("fallback map: timeout", result.get("message"));
    }
}