package travel.route.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.common.utils.CommonUtil;
import travel.common.utils.ExceptionUtil;
import travel.route.dto.optimization.RouteQualityEvaluationResult;
import travel.route.dto.route.RouteComparisonDetail;
import travel.route.dto.route.RouteComparisonResult;
import travel.route.dto.route.RouteOptimizationSuggestionResult;
import travel.route.dto.route.RouteQualityEvaluation;
import travel.route.dto.route.RouteQualityEvaluationRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligentRouteEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRouteEvaluationService.class);
    private static final String ROUTE_OPTIMIZATION_PREFIX = "route:optimization:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final RouteAttractionService routeAttractionService;
    private final RouteOptimizationService routeOptimizationService;
    private final CacheUtil cacheUtil;

    public RouteComparisonResult compareRoutes(List<Integer> routeIds) {
        try {
            log.info("Compare routes: routeIds={}", routeIds);

            List<Route> routes = routeIds.stream()
                    .map(routeService::getById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            ExceptionUtil.checkCondition(!routes.isEmpty(), "No valid routes found");

            List<RouteComparisonDetail> routeDetails = routes.stream()
                    .map(this::buildComparisonDetail)
                    .collect(Collectors.toList());

            RouteComparisonDetail bestRoute = routeDetails.stream()
                    .max(Comparator.comparingDouble(detail -> {
                        double rating = detail.getAverageRating() != null ? detail.getAverageRating() : 0.0;
                        int likeCount = detail.getLikeCount() != null ? detail.getLikeCount() : 0;
                        return rating * 0.6 + likeCount * 0.4;
                    }))
                    .orElse(null);

            return RouteComparisonResult.builder()
                    .routes(routeDetails)
                    .bestRoute(bestRoute)
                    .totalRoutes(routeDetails.size())
                    .build();
        } catch (Exception e) {
            log.error("Compare routes failed", e);
            throw new RuntimeException("Compare routes failed: " + e.getMessage(), e);
        }
    }

    public RouteQualityEvaluation evaluateRouteQuality(Integer routeId, RouteQualityEvaluationRequest request) {
        try {
            log.info("Evaluate route quality: routeId={}, extensionCount={}", routeId,
                    request == null || request.getExtensions() == null ? 0 : request.getExtensions().size());

            RouteQualityEvaluationResult baseEvaluation = routeOptimizationService.evaluateRouteQuality(routeId);
            double qualityScore = baseEvaluation.getQualityScore() != null ? baseEvaluation.getQualityScore() : 0.0;

            Route route = routeService.getById(routeId);
            double diversityScore = 0.0;
            double reasonablenessScore = 0.0;
            double costPerformanceScore = 0.0;
            double overallScore = qualityScore;

            if (route != null) {
                List<Attraction> attractions = getRouteAttractions(routeId.longValue());
                diversityScore = calculateRouteDiversity(attractions);
                reasonablenessScore = calculateRouteReasonableness(attractions, route.getDurationDays());
                costPerformanceScore = calculateCostPerformance(attractions, route.getDurationDays());
                overallScore = qualityScore * 0.5 + diversityScore * 0.2 + reasonablenessScore * 0.2 + costPerformanceScore * 0.1;
            }

            return RouteQualityEvaluation.builder()
                    .routeId(routeId)
                    .qualityScore(qualityScore)
                    .diversityScore(diversityScore)
                    .reasonablenessScore(reasonablenessScore)
                    .costPerformanceScore(costPerformanceScore)
                    .overallScore(overallScore)
                    .build();
        } catch (Exception e) {
            log.error("Evaluate route quality failed: routeId={}", routeId, e);
            throw new RuntimeException("Evaluate route quality failed: " + e.getMessage(), e);
        }
    }

    public RouteOptimizationSuggestionResult getRouteOptimizationSuggestions(Integer routeId, String optimizationType) {
        try {
            String cacheKey = ROUTE_OPTIMIZATION_PREFIX + routeId + ":" + optimizationType;
            RouteOptimizationSuggestionResult cachedSuggestions = cacheUtil.get(cacheKey, RouteOptimizationSuggestionResult.class);
            if (cachedSuggestions != null) {
                log.info("Get route optimization suggestions from cache: routeId={}", routeId);
                return cachedSuggestions;
            }

            Route route = routeService.getById(routeId);
            ExceptionUtil.checkNotNull(route, "Route not found");
            List<Attraction> attractions = getRouteAttractions(routeId.longValue());
            List<String> suggestionList = new ArrayList<>();

            switch (optimizationType) {
                case "time" -> buildTimeSuggestions(route, attractions, suggestionList);
                case "cost" -> buildCostSuggestions(attractions, suggestionList);
                case "experience" -> buildExperienceSuggestions(attractions, suggestionList);
                default -> buildDefaultSuggestions(suggestionList);
            }

            RouteOptimizationSuggestionResult result = RouteOptimizationSuggestionResult.builder()
                    .routeId(routeId)
                    .routeName(route.getTitle())
                    .optimizationType(optimizationType)
                    .suggestions(suggestionList)
                    .attractionCount(attractions.size())
                    .durationDays(route.getDurationDays())
                    .build();

            cacheUtil.set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return result;
        } catch (Exception e) {
            log.error("Get route optimization suggestions failed: routeId={}", routeId, e);
            throw new RuntimeException("Get route optimization suggestions failed: " + e.getMessage(), e);
        }
    }

    private RouteComparisonDetail buildComparisonDetail(Route route) {
        List<Attraction> attractions = getRouteAttractions(route.getId().longValue());
        double totalDistance = calculateTotalDistance(attractions);
        double estimatedCost = calculateEstimatedCost(attractions);
        double estimatedTime = calculateEstimatedTime(attractions);
        double averageRating = attractions.stream().mapToDouble(a -> a.getRating().doubleValue()).average().orElse(0.0);

        return RouteComparisonDetail.builder()
                .routeId(route.getId())
                .routeName(route.getTitle())
                .durationDays(route.getDurationDays())
                .totalAttractions(attractions.size())
                .totalDistance(totalDistance)
                .estimatedCost(estimatedCost)
                .estimatedTime(estimatedTime)
                .averageRating(averageRating)
                .viewCount(route.getViewCount())
                .likeCount(route.getLikeCount())
                .build();
    }

    private List<Attraction> getRouteAttractions(Long routeId) {
        List<RouteAttraction> routeAttractions = routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId);
        return routeAttractions.stream()
                .map(ra -> attractionService.getById(ra.getAttractionId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void buildTimeSuggestions(Route route, List<Attraction> attractions, List<String> suggestionList) {
        if (attractions.size() > route.getDurationDays() * 4) {
            suggestionList.add("Attraction count is too high for the available days.");
        }
        suggestionList.add("Reorder attractions to reduce travel time.");
        suggestionList.add("Check opening hours to avoid closed attractions.");
    }

    private void buildCostSuggestions(List<Attraction> attractions, List<String> suggestionList) {
        long highCostAttractions = attractions.stream()
                .filter(a -> a.getTicketPrice().compareTo(BigDecimal.valueOf(50)) > 0)
                .count();
        if (highCostAttractions > attractions.size() / 2) {
            suggestionList.add("Add more free or low-cost attractions.");
        }
        suggestionList.add("Prefer public transport to reduce travel cost.");
        suggestionList.add("Consider combo tickets or bundled discounts.");
    }

    private void buildExperienceSuggestions(List<Attraction> attractions, List<String> suggestionList) {
        double diversityScore = calculateRouteDiversity(attractions);
        if (diversityScore < 0.5) {
            suggestionList.add("Add more diverse attraction types to improve experience.");
        }
        suggestionList.add("Optimize visit order to avoid repetitive route segments.");
        suggestionList.add("Avoid peak periods at crowded attractions.");
    }

    private void buildDefaultSuggestions(List<String> suggestionList) {
        suggestionList.add("Balance the number of attractions across each day.");
        suggestionList.add("Optimize transport mode and transit time.");
        suggestionList.add("Adjust the route according to season and weather.");
        suggestionList.add("Incorporate feedback from other travelers.");
    }

    private double calculateRouteDiversity(List<Attraction> attractions) {
        Set<String> attractionTypes = new HashSet<>();
        attractions.forEach(attraction -> {
            String description = attraction.getDescription();
            if (description != null) {
                if (description.contains("博物馆") || description.contains("文化") || description.contains("历史")) {
                    attractionTypes.add("culture");
                }
                if (description.contains("公园") || description.contains("山水") || description.contains("自然") || description.contains("风景")) {
                    attractionTypes.add("nature");
                }
                if (description.contains("美食") || description.contains("餐厅") || description.contains("小吃") || description.contains("饮食")) {
                    attractionTypes.add("food");
                }
                if (description.contains("乐园") || description.contains("儿童") || description.contains("互动") || description.contains("教育")) {
                    attractionTypes.add("family");
                }
            }
        });
        return attractionTypes.isEmpty() ? 0.0 : attractionTypes.size() / 4.0;
    }

    private double calculateRouteReasonableness(List<Attraction> attractions, int days) {
        int safeDays = Math.max(days, 1);
        int idealAttractionsPerDay = 4;
        int actualAttractionsPerDay = attractions.size() / safeDays;
        double deviation = Math.abs(actualAttractionsPerDay - idealAttractionsPerDay) / (double) idealAttractionsPerDay;
        return Math.max(0, 1 - deviation);
    }

    private double calculateCostPerformance(List<Attraction> attractions, int days) {
        int safeDays = Math.max(days, 1);
        double totalCost = attractions.stream().mapToDouble(a -> a.getTicketPrice().doubleValue()).sum();
        double averageRating = attractions.stream().mapToDouble(a -> a.getRating().doubleValue()).average().orElse(0.0);
        double costPerDay = totalCost / safeDays;
        return averageRating / (costPerDay / 100 + 1);
    }

    private double calculateTotalDistance(List<Attraction> attractions) {
        if (attractions == null || attractions.size() < 2) {
            return 0.0;
        }
        double totalDistance = 0.0;
        for (int i = 0; i < attractions.size() - 1; i++) {
            Attraction current = attractions.get(i);
            Attraction next = attractions.get(i + 1);
            totalDistance += CommonUtil.calculateDistance(
                    current.getLatitude().doubleValue(), current.getLongitude().doubleValue(),
                    next.getLatitude().doubleValue(), next.getLongitude().doubleValue());
        }
        return totalDistance;
    }

    private double calculateEstimatedCost(List<Attraction> attractions) {
        return attractions.stream().mapToDouble(a -> a.getTicketPrice().doubleValue()).sum() + attractions.size() * 20.0;
    }

    private double calculateEstimatedTime(List<Attraction> attractions) {
        return attractions.size() * 2.0;
    }
}
