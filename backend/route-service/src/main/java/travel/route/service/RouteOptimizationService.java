package travel.route.service;

import travel.route.algorithm.RoutePlanAlgorithm.OptimalRoute;
import travel.route.dto.optimization.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RouteOptimizationService {

    OptimalRoute planOptimalRoute(List<Integer> attractionIds, int maxDays, BigDecimal budget, String preference);

    RouteQualityEvaluationResult evaluateRouteQuality(Integer routeId);

    List<OptimizationSuggestion> getOptimizationSuggestions(Integer routeId);

    boolean applyOptimization(ApplyOptimizationRequest request);

    List<OptimizationHistoryItem> getOptimizationHistory(Integer routeId);
}
