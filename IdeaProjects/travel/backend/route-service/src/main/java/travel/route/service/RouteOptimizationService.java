package travel.route.service;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;
import travel.route.dto.optimization.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RouteOptimizationService {

    OptimalRoute planOptimalRoute(List<Integer> attractionIds, int maxDays, BigDecimal budget, String preference);

    AdjustRouteResult adjustRoute(Integer routeId, String adjustmentType, Map<String, Object> adjustmentParams);

    List<RouteRecommendationItem> getRouteRecommendations(Integer cityId, int days, List<String> interests, BigDecimal budget);

    double calculateRouteSimilarity(Integer routeId1, Integer routeId2);

    RouteQualityEvaluationResult evaluateRouteQuality(Integer routeId);

    List<RouteAlternative> generateRouteAlternatives(Integer routeId, int alternativeCount);

    RouteCrowdPrediction predictRouteCrowd(Integer routeId, String date);

    RouteAnalysisResult getRouteAnalysis(Integer routeId);

    boolean saveUserRoutePreferences(Integer userId, Map<String, Object> preferences);

    List<RouteRecommendationItem> getPersonalizedRouteRecommendations(Integer userId, Integer cityId, int days);

    List<OptimizationSuggestion> getOptimizationSuggestions(Integer routeId);

    boolean applyOptimization(ApplyOptimizationRequest request);

    List<OptimizationHistoryItem> getOptimizationHistory(Integer routeId);
}
