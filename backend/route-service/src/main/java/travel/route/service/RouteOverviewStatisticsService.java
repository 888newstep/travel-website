package travel.route.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.repository.RouteRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteOverviewStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(RouteOverviewStatisticsService.class);

    private final RouteRepository routeRepository;

    public Map<String, Object> getRouteStatistics() {
        try {
            List<Route> allRoutes = routeRepository.findAll();
            Map<String, Object> statistics = new HashMap<>();

            statistics.put("totalRoutes", allRoutes.size());
            statistics.put("publicRoutes", allRoutes.stream().filter(route -> Boolean.TRUE.equals(route.getIsPublic())).count());
            statistics.put("privateRoutes", allRoutes.stream().filter(route -> Boolean.FALSE.equals(route.getIsPublic())).count());
            statistics.put("averageDays", allRoutes.stream()
                    .map(Route::getDurationDays)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0));
            statistics.put("totalViews", allRoutes.stream()
                    .map(Route::getViewCount)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum());
            statistics.put("totalLikes", allRoutes.stream()
                    .map(Route::getLikeCount)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum());
            return statistics;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to get route statistics", e);
        }
    }

    public List<Map<String, Object>> getRouteStatisticsByCity() {
        try {
            Map<Integer, List<Route>> routesByCity = routeRepository.findAll().stream()
                    .collect(Collectors.groupingBy(Route::getCityId));

            List<Map<String, Object>> cityStats = new ArrayList<>();
            routesByCity.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Integer::compareTo)))
                    .forEach(entry -> {
                        Map<String, Object> stat = new HashMap<>();
                        stat.put("cityId", entry.getKey());
                        stat.put("routeCount", entry.getValue().size());
                        stat.put("averageRating", 0.0);
                        cityStats.add(stat);
                    });
            return cityStats;
        } catch (Exception e) {
            return RouteFallbackSupport.emptyList(log, "Failed to get route statistics by city", e);
        }
    }

    public Map<String, Object> getRouteCompletionRate() {
        try {
            List<Route> allRoutes = routeRepository.findAll();
            int totalRoutes = allRoutes.size();
            int completedRoutes = (int) allRoutes.stream()
                    .filter(route -> route.getViewCount() != null && route.getViewCount() > 0)
                    .count();
            double completionRate = totalRoutes > 0 ? (double) completedRoutes / totalRoutes : 0.0;

            Map<String, Object> result = new HashMap<>();
            result.put("totalRoutes", totalRoutes);
            result.put("completedRoutes", completedRoutes);
            result.put("rate", completionRate);
            return result;
        } catch (Exception e) {
            return RouteFallbackSupport.errorResult(log, "Failed to get route completion rate", e);
        }
    }

    public List<Map<String, Object>> getRouteDurationDistribution() {
        try {
            Map<Integer, Long> durationDistribution = routeRepository.findAll().stream()
                    .filter(route -> route.getDurationDays() != null)
                    .collect(Collectors.groupingBy(Route::getDurationDays, Collectors.counting()));

            List<Map<String, Object>> distribution = new ArrayList<>();
            durationDistribution.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("days", entry.getKey());
                        item.put("count", entry.getValue());
                        distribution.add(item);
                    });
            return distribution;
        } catch (Exception e) {
            return RouteFallbackSupport.emptyList(log, "Failed to get route duration distribution", e);
        }
    }
}