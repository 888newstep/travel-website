package travel.route.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.route.dto.route.RealTimeAdjustmentRequest;
import travel.route.dto.route.RealTimeAlternativeAttraction;
import travel.route.dto.route.RealTimeCrowdFactors;
import travel.route.dto.route.RealTimeFactors;
import travel.route.dto.route.RealTimeAdjustmentResult;
import travel.route.dto.route.RealTimeTrafficFactors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligentRouteRealtimeAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRouteRealtimeAdjustmentService.class);

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;
    private final RouteRealTimeAdjustmentService routeRealTimeAdjustmentService;

    public RealTimeAdjustmentResult getRealTimeAdjustment(Integer routeId, RealTimeAdjustmentRequest request) {
        log.info("Get realtime route adjustment: routeId={}", routeId);
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        List<Attraction> routeAttractions = getRouteAttractions(routeId.longValue());
        List<Long> attractionIds = routeAttractions.stream()
                .map(Attraction::getId)
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .toList();
        Map<String, Object> trafficInfo = routeRealTimeAdjustmentService
                .getRealTimeTrafficInfo(routeId.longValue());
        Map<Long, Map<String, Object>> attractionStatus = attractionIds.isEmpty()
                ? Map.of()
                : routeRealTimeAdjustmentService.getRealTimeAttractionStatus(attractionIds);

        List<String> congestedRoutes = trafficInfo.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("segment:"))
                .filter(entry -> "heavy".equals(entry.getValue()) || "severe".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        List<Integer> crowdedAttractions = attractionStatus.entrySet().stream()
                .filter(entry -> crowdLevel(entry.getValue()) >= 4)
                .map(entry -> Math.toIntExact(entry.getKey()))
                .toList();
        String weather = attractionStatus.values().stream()
                .map(status -> Objects.toString(status.get("weather"), null))
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);

        RealTimeTrafficFactors trafficFactors = new RealTimeTrafficFactors();
        trafficFactors.setCongestedRoutes(congestedRoutes);
        RealTimeCrowdFactors crowdFactors = new RealTimeCrowdFactors();
        crowdFactors.setCrowdedAttractions(crowdedAttractions);
        RealTimeFactors actualFactors = new RealTimeFactors();
        actualFactors.setWeather(weather);
        actualFactors.setTraffic(trafficFactors);
        actualFactors.setCrowd(crowdFactors);

        List<String> adjustments = new ArrayList<>();
        List<RealTimeAlternativeAttraction> alternativeAttractions = new ArrayList<>();
        if (isRainy(weather)) {
            adjustments.add("Prefer indoor attractions because of rainy weather.");
            alternativeAttractions.addAll(getAlternativeIndoorAttractions(resolveRouteCityId(route)));
        }
        if (!congestedRoutes.isEmpty()) {
            adjustments.add("Avoid congested segments: " + String.join(", ", congestedRoutes));
        }
        if (!crowdedAttractions.isEmpty()) {
            Map<Integer, String> attractionNames = new LinkedHashMap<>();
            routeAttractions.forEach(attraction -> attractionNames.put(
                    attraction.getId(), Objects.toString(attraction.getName(), attraction.getId().toString())));
            adjustments.add("Avoid crowded attractions: " + crowdedAttractions.stream()
                    .map(id -> attractionNames.getOrDefault(id, id.toString()))
                    .collect(Collectors.joining(", ")));
        }

        return RealTimeAdjustmentResult.builder()
                .routeId(routeId)
                .routeName(route.getTitle())
                .adjustments(adjustments)
                .alternativeAttractions(alternativeAttractions)
                .currentLocation(request == null ? null : request.getCurrentLocation())
                .realTimeFactors(actualFactors)
                .build();
    }

    private int crowdLevel(Map<String, Object> status) {
        Object value = status == null ? null : status.get("crowdLevel");
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean isRainy(String weather) {
        if (weather == null) {
            return false;
        }
        String normalized = weather.trim().toLowerCase();
        return normalized.contains("rain") || normalized.contains("雨");
    }

    private List<Attraction> getRouteAttractions(Long routeId) {
        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        return routeAttractions.stream()
                .map(ra -> attractionService.getById(ra.getAttractionId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<RealTimeAlternativeAttraction> getAlternativeIndoorAttractions(Integer cityId) {
        if (cityId == null) {
            return List.of();
        }
        List<Attraction> allAttractions = attractionService.getByCityId(cityId);
        if (allAttractions == null || allAttractions.isEmpty()) {
            return List.of();
        }
        return allAttractions.stream()
                .filter(attraction -> {
                    String description = attraction.getDescription();
                    return description != null && (
                            description.contains("博物馆") ||
                            description.contains("室内") ||
                            description.contains("文化") ||
                            description.contains("历史")
                    );
                })
                .limit(3)
                .map(attraction -> {
                    return new RealTimeAlternativeAttraction(
                            attraction.getId(), attraction.getName(), attraction.getDescription(), "indoor");
                })
                .collect(Collectors.toList());
    }

    private Integer resolveRouteCityId(Route route) {
        if (route == null) {
            return null;
        }
        if (route.getCity() != null && route.getCity().getId() != null) {
            return route.getCity().getId();
        }
        return route.getCityId();
    }
}
