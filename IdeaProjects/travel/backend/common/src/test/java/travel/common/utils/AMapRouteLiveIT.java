package travel.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AMapRouteLiveIT {

    @Test
    void shouldFetchTwoPointTrafficAndMultiPointRouteFromAmap() {
        String apiKey = requiredEnvironment("AMAP_API_KEY");
        String apiUrl = optionalEnvironment("AMAP_API_URL", "https://restapi.amap.com/v3");
        Scenario twoPoint = scenario("AMAP_LIVE_TWO", 2, 2);
        Scenario multiPoint = scenario("AMAP_LIVE_MULTI", 3, Integer.MAX_VALUE);
        ExternalCallBulkheadRegistry registry = new ExternalCallBulkheadRegistry(100, 4, 1, 1);

        AMapService trafficService = new AMapService(
                new AICacheManager(new CacheUtil(Optional.empty())),
                registry);
        ReflectionTestUtils.setField(trafficService, "apiKey", apiKey);
        ReflectionTestUtils.setField(trafficService, "apiUrl", apiUrl);
        ReflectionTestUtils.setField(trafficService, "maxResponseBytes", 1_048_576L);

        double[] origin = twoPoint.points().get(0);
        double[] destination = twoPoint.points().get(1);
        Map<String, Object> traffic = trafficService.drivingRoute(
                origin[0], origin[1], destination[0], destination[1]);
        assertNotNull(traffic);
        assertEquals("amap", traffic.get("source"));
        assertTrue(number(traffic.get("distance")) > 0);
        assertTrue(number(traffic.get("duration")) > 0);
        JsonNode steps = (JsonNode) traffic.get("steps");
        assertNotNull(steps);
        long trafficSegmentCount = countTrafficSegments(steps);
        assertTrue(trafficSegmentCount > 0, "extensions=all should return tmcs traffic segments");
        printTrafficResult(twoPoint, traffic, trafficSegmentCount);

        AMapRouteService routeService = new AMapRouteService(3000, 8000, 1_048_576L, registry);
        ReflectionTestUtils.setField(routeService, "apiKey", apiKey);
        ReflectionTestUtils.setField(routeService, "apiUrl", apiUrl);
        AMapRouteService.RouteInfo route = routeService.calculateMultiPointRoute(multiPoint.points());
        assertNotNull(route);
        assertTrue(route.getDistance() > 0);
        assertTrue(route.getDuration() > 0);
        assertNotNull(route.getSteps());
        assertFalse(route.getSteps().isEmpty());
        printMultiPointResult(multiPoint, route);
    }

    private Scenario scenario(String prefix, int minimumPoints, int maximumPoints) {
        long routeId = Long.parseLong(requiredEnvironment(prefix + "_ROUTE_ID"));
        List<double[]> points = Arrays.stream(requiredEnvironment(prefix + "_POINTS").split("\\|"))
                .map(this::parsePoint)
                .toList();
        assertTrue(points.size() >= minimumPoints);
        assertTrue(points.size() <= maximumPoints);
        return new Scenario(routeId, points);
    }

    private double[] parsePoint(String value) {
        String[] coordinates = value.split(",", 2);
        if (coordinates.length != 2) {
            throw new IllegalArgumentException("Invalid live coordinate");
        }
        return new double[]{Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1])};
    }

    private long countTrafficSegments(JsonNode steps) {
        long count = 0;
        for (JsonNode step : steps) {
            JsonNode tmcs = step.path("tmcs");
            if (tmcs.isArray()) {
                count += tmcs.size();
            }
        }
        return count;
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private void printTrafficResult(Scenario scenario, Map<String, Object> traffic, long trafficSegmentCount) {
        System.out.printf(Locale.ROOT,
                "AMAP_LIVE_RESULT scenario=two-point routeId=%d pointCount=%d distanceMeters=%d "
                        + "durationSeconds=%d trafficSegmentCount=%d%n",
                scenario.routeId(),
                scenario.points().size(),
                number(traffic.get("distance")),
                number(traffic.get("duration")),
                trafficSegmentCount);
    }

    private void printMultiPointResult(Scenario scenario, AMapRouteService.RouteInfo route) {
        System.out.printf(Locale.ROOT,
                "AMAP_LIVE_RESULT scenario=multi-point routeId=%d pointCount=%d distanceKm=%.3f "
                        + "durationMinutes=%.3f tolls=%.2f stepCount=%d%n",
                scenario.routeId(),
                scenario.points().size(),
                route.getDistance(),
                route.getDuration(),
                route.getCost(),
                route.getSteps().size());
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private String optionalEnvironment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record Scenario(long routeId, List<double[]> points) {
    }
}
