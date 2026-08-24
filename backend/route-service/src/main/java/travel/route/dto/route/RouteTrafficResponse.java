package travel.route.dto.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record RouteTrafficResponse(
        Integer routeId,
        boolean dataAvailable,
        String source,
        long totalDistanceMeters,
        long totalDurationSeconds,
        List<RouteTrafficSegmentResponse> segments,
        String message) {

    public static RouteTrafficResponse from(Integer routeId, Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return new RouteTrafficResponse(routeId, false, "amap", 0, 0, List.of(), "实时路况暂不可用");
        }
        boolean available = Boolean.TRUE.equals(raw.get("dataAvailable"));
        return new RouteTrafficResponse(
                routeId,
                available,
                text(raw.get("source"), "amap"),
                number(raw.get("totalDistanceMeters")),
                number(raw.get("totalDurationSeconds")),
                parseSegments(raw.get("routeDetails")),
                text(raw.get("message"), available ? null : "实时路况暂不可用"));
    }

    private static List<RouteTrafficSegmentResponse> parseSegments(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<RouteTrafficSegmentResponse> segments = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            segments.add(new RouteTrafficSegmentResponse(
                    text(map.get("segmentId"), null),
                    integer(map.get("fromAttractionId")),
                    integer(map.get("toAttractionId")),
                    text(map.get("status"), "unknown"),
                    number(map.get("distanceMeters")),
                    number(map.get("durationSeconds"))));
        }
        return List.copyOf(segments);
    }

    private static Integer integer(Object value) {
        long number = number(value);
        return number > Integer.MAX_VALUE || number < Integer.MIN_VALUE ? null : (int) number;
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? 0 : Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String text(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString();
    }
}
