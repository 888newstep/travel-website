package travel.route.service;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RouteFallbackSupport {

    private RouteFallbackSupport() {
    }

    public static <T> List<T> emptyList(Logger log, String operation, Exception e) {
        log.error(operation, e);
        return List.of();
    }

    public static Map<String, Object> errorResult(Logger log, String operation, Exception e) {
        log.error(operation, e);
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("success", false);
        errorResult.put("message", operation + ": " + e.getMessage());
        return errorResult;
    }
}