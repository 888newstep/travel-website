package travel.route.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteTransportServiceTest {

    private final RouteTransportService routeTransportService = new RouteTransportService();

    @Test
    void shouldReturnDefaultTransportOptions() {
        List<Map<String, Object>> options = routeTransportService.getTransportOptions(1, 2);

        assertEquals(3, options.size());
        assertEquals("public", options.get(0).get("type"));
        assertEquals(50, options.get(0).get("cost"));
        assertEquals(120, options.get(0).get("time"));
    }

    @Test
    void shouldCalculateTransportCost() {
        Map<String, Object> result = routeTransportService.calculateTransportCost(Map.of(
                "transportType", "taxi",
                "distance", 80
        ));

        assertEquals("taxi", result.get("transportType"));
        assertEquals(80, result.get("distance"));
        assertEquals(2.0, result.get("costPerKm"));
        assertEquals(160.0, result.get("totalCost"));
    }

    @Test
    void shouldUseDefaultValuesWhenParamsMissing() {
        Map<String, Object> result = routeTransportService.calculateTransportTime(null);

        assertEquals("public", result.get("transportType"));
        assertEquals(100, result.get("distance"));
        assertEquals(30.0, result.get("speedKmPerHour"));
        assertEquals(200.0, result.get("totalTime"));
    }

    @Test
    void shouldFilterTransportRecommendationsByBudgetAndTimeConstraint() {
        List<Map<String, Object>> recommendations = routeTransportService.getTransportRecommendations(Map.of(
                "budget", 160,
                "timeConstraint", 100
        ));

        assertEquals(1, recommendations.size());
        assertEquals("private", recommendations.get(0).get("type"));
    }

    @Test
    void shouldFallbackToDefaultWhenParamTypeIsUnexpected() {
        Map<String, Object> result = routeTransportService.calculateTransportCost(Map.of(
                "transportType", 123,
                "distance", "bad"
        ));

        assertEquals("public", result.get("transportType"));
        assertEquals(100, result.get("distance"));
        assertEquals(50.0, result.get("totalCost"));
    }
}