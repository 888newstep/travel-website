package travel.route.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteRealtimeStatusService {

    private static final Logger log = LoggerFactory.getLogger(RouteRealtimeStatusService.class);

    public List<Map<String, Object>> getRoutesNeedingSync(Integer minutes) {
        try {
            List<Map<String, Object>> routes = new ArrayList<>();
            Map<String, Object> route = new HashMap<>();
            route.put("routeId", 1);
            route.put("minutes", normalizeMinutes(minutes));
            route.put("lastUpdated", "2026-04-22T10:00:00");
            routes.add(route);
            return routes;
        } catch (Exception e) {
            return RouteFallbackSupport.emptyList(log, "Failed to get routes needing sync", e);
        }
    }

    public Map<String, Object> syncRouteStatus(List<Integer> routeIds) {
        try {
            int successCount = routeIds == null ? 0 : routeIds.size();
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failedCount", 0);
            return result;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to sync route status", e);
        }
    }

    public Map<String, Object> getRouteRealtimeStatus(Integer routeId) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("routeId", routeId);
            status.put("status", "active");
            status.put("lastUpdated", Instant.now().toString());
            return status;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to get route realtime status", e);
        }
    }

    public Map<String, Object> updateRouteRealtimeStatus(Integer routeId, Map<String, Object> params) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("routeId", routeId);
            if (params != null && params.containsKey("status")) {
                result.put("status", params.get("status"));
            }
            return result;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to update route realtime status", e);
        }
    }

    private int normalizeMinutes(Integer minutes) {
        return minutes == null || minutes <= 0 ? 30 : minutes;
    }
}