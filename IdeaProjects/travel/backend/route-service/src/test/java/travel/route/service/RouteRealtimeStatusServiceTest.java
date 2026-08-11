package travel.route.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteRealtimeStatusServiceTest {

    private final RouteRealtimeStatusService routeRealtimeStatusService = new RouteRealtimeStatusService();

    @Test
    void shouldReturnRoutesNeedingSyncWithDefaultMinutes() {
        List<Map<String, Object>> routes = routeRealtimeStatusService.getRoutesNeedingSync(null);

        assertEquals(1, routes.size());
        assertEquals(1, routes.get(0).get("routeId"));
        assertEquals(30, routes.get(0).get("minutes"));
        assertEquals("2026-04-22T10:00:00", routes.get(0).get("lastUpdated"));
    }

    @Test
    void shouldKeepPositiveMinutes() {
        List<Map<String, Object>> routes = routeRealtimeStatusService.getRoutesNeedingSync(45);

        assertEquals(45, routes.get(0).get("minutes"));
    }

    @Test
    void shouldSyncRouteStatusWithNullableRouteIds() {
        Map<String, Object> emptyResult = routeRealtimeStatusService.syncRouteStatus(null);
        Map<String, Object> filledResult = routeRealtimeStatusService.syncRouteStatus(List.of(1, 2, 3));

        assertEquals(0, emptyResult.get("successCount"));
        assertEquals(0, emptyResult.get("failedCount"));
        assertEquals(3, filledResult.get("successCount"));
        assertEquals(0, filledResult.get("failedCount"));
    }

    @Test
    void shouldReturnRealtimeStatus() {
        Map<String, Object> status = routeRealtimeStatusService.getRouteRealtimeStatus(88);

        assertEquals(88, status.get("routeId"));
        assertEquals("active", status.get("status"));
        assertNotNull(status.get("lastUpdated"));
        assertFalse(((String) status.get("lastUpdated")).isBlank());
    }

    @Test
    void shouldUpdateRealtimeStatus() {
        Map<String, Object> result = routeRealtimeStatusService.updateRouteRealtimeStatus(12, Map.of("status", "paused"));

        assertEquals(true, result.get("success"));
        assertEquals(12, result.get("routeId"));
        assertEquals("paused", result.get("status"));
    }

    @Test
    void shouldAllowNullParamsWhenUpdatingRealtimeStatus() {
        Map<String, Object> result = routeRealtimeStatusService.updateRouteRealtimeStatus(7, null);

        assertEquals(true, result.get("success"));
        assertEquals(7, result.get("routeId"));
        assertTrue(!result.containsKey("status"));
    }
}