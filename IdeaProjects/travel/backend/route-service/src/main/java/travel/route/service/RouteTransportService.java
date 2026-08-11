package travel.route.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteTransportService {

    private static final Logger log = LoggerFactory.getLogger(RouteTransportService.class);

    public List<Map<String, Object>> getTransportOptions(Integer fromCity, Integer toCity) {
        try {
            List<Map<String, Object>> options = new ArrayList<>();
            options.add(buildOption("public", "Public Transport", 50, 120));
            options.add(buildOption("taxi", "Taxi", 200, 60));
            options.add(buildOption("private", "Private Car", 150, 80));
            return options;
        } catch (Exception e) {
            return RouteFallbackSupport.emptyList(log, "Failed to get transport options", e);
        }
    }

    public Map<String, Object> calculateTransportCost(Map<String, Object> params) {
        try {
            String transportType = resolveString(params, "transportType", "public");
            int distance = resolveInteger(params, "distance", 100);

            double costPerKm = switch (transportType) {
                case "public" -> 0.5;
                case "taxi" -> 2.0;
                case "private" -> 1.5;
                default -> 0.5;
            };

            Map<String, Object> result = new HashMap<>();
            result.put("transportType", transportType);
            result.put("distance", distance);
            result.put("costPerKm", costPerKm);
            result.put("totalCost", distance * costPerKm);
            return result;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to calculate transport cost", e);
        }
    }

    public Map<String, Object> calculateTransportTime(Map<String, Object> params) {
        try {
            String transportType = resolveString(params, "transportType", "public");
            int distance = resolveInteger(params, "distance", 100);

            double speedKmPerHour = switch (transportType) {
                case "public" -> 30.0;
                case "taxi" -> 40.0;
                case "private" -> 50.0;
                default -> 30.0;
            };

            Map<String, Object> result = new HashMap<>();
            result.put("transportType", transportType);
            result.put("distance", distance);
            result.put("speedKmPerHour", speedKmPerHour);
            result.put("totalTime", distance / speedKmPerHour * 60);
            return result;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to calculate transport time", e);
        }
    }

    public List<Map<String, Object>> getTransportRecommendations(Map<String, Object> params) {
        try {
            int budget = resolveInteger(params, "budget", 200);
            int timeConstraint = resolveInteger(params, "timeConstraint", 120);

            List<Map<String, Object>> recommendations = new ArrayList<>();
            for (Map<String, Object> option : getTransportOptions(1, 2)) {
                Number cost = (Number) option.get("cost");
                Number time = (Number) option.get("time");
                if (cost.doubleValue() <= budget && time.doubleValue() <= timeConstraint) {
                    recommendations.add(option);
                }
            }
            return recommendations;
        } catch (Exception e) {
            return RouteFallbackSupport.emptyList(log, "Failed to get transport recommendations", e);
        }
    }

    private Map<String, Object> buildOption(String type, String name, int cost, int time) {
        Map<String, Object> option = new HashMap<>();
        option.put("type", type);
        option.put("name", name);
        option.put("cost", cost);
        option.put("time", time);
        return option;
    }

    private String resolveString(Map<String, Object> params, String key, String defaultValue) {
        if (params == null) {
            return defaultValue;
        }
        Object value = params.get(key);
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : defaultValue;
    }

    private int resolveInteger(Map<String, Object> params, String key, int defaultValue) {
        if (params == null) {
            return defaultValue;
        }
        Object value = params.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }
}