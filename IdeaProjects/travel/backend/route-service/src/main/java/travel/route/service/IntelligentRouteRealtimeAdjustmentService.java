package travel.route.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.ExceptionUtil;
import travel.route.dto.route.RealTimeAdjustmentRequest;
import travel.route.dto.route.RealTimeAlternativeAttraction;
import travel.route.dto.route.RealTimeCrowdFactors;
import travel.route.dto.route.RealTimeFactors;
import travel.route.dto.route.RealTimeAdjustmentResult;
import travel.route.dto.route.RealTimeTrafficFactors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligentRouteRealtimeAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRouteRealtimeAdjustmentService.class);

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;

    public RealTimeAdjustmentResult getRealTimeAdjustment(Integer routeId, RealTimeAdjustmentRequest request) {
        try {
            log.info("Get realtime route adjustment: routeId={}, request={}", routeId, request);

            RealTimeFactors safeFactors = request == null || request.getRealTimeFactors() == null
                    ? new RealTimeFactors() : request.getRealTimeFactors();
            Route route = routeService.getById(routeId);
            ExceptionUtil.checkNotNull(route, "Route not found");

            getRouteAttractions(routeId.longValue());
            String weather = Objects.toString(safeFactors.getWeather(), "sunny");
            RealTimeTrafficFactors traffic = safeFactors.getTraffic();
            RealTimeCrowdFactors crowd = safeFactors.getCrowd();

            List<String> adjustments = new ArrayList<>();
            List<RealTimeAlternativeAttraction> alternativeAttractions = new ArrayList<>();

            if ("rainy".equalsIgnoreCase(weather)) {
                adjustments.add("Prefer indoor attractions because of rainy weather.");
                alternativeAttractions.addAll(getAlternativeIndoorAttractions(resolveRouteCityId(route)));
            }

            List<String> congestedRoutes = traffic == null || traffic.getCongestedRoutes() == null
                    ? List.of() : traffic.getCongestedRoutes();
            if (!congestedRoutes.isEmpty()) {
                adjustments.add("Avoid congested segments: " + String.join(", ", congestedRoutes));
            }

            List<Integer> crowdedAttractions = crowd == null || crowd.getCrowdedAttractions() == null
                    ? List.of() : crowd.getCrowdedAttractions();
            if (!crowdedAttractions.isEmpty()) {
                adjustments.add("Avoid crowded attractions: " + crowdedAttractions.stream()
                        .map(id -> {
                            Attraction attraction = attractionService.getById(id);
                            return attraction != null ? attraction.getName() : String.valueOf(id);
                        })
                        .collect(Collectors.joining(", ")));
            }

            return RealTimeAdjustmentResult.builder()
                    .routeId(routeId)
                    .routeName(route.getTitle())
                    .adjustments(adjustments)
                    .alternativeAttractions(alternativeAttractions)
                    .currentLocation(request == null ? null : request.getCurrentLocation())
                    .realTimeFactors(safeFactors)
                    .build();
        } catch (Exception e) {
            log.error("Get realtime route adjustment failed: routeId={}", routeId, e);
            throw new RuntimeException("Get realtime route adjustment failed: " + e.getMessage(), e);
        }
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
