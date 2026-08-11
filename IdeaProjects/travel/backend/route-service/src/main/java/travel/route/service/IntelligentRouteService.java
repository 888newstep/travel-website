package travel.route.service;

import travel.common.entity.route_planning.Route;
import travel.route.dto.route.*;

import java.util.List;
import java.util.Map;

public interface IntelligentRouteService {

    List<Route> getSmartRouteRecommendations(Integer cityId, int days);

    String optimizeRoute(Integer routeId);

    String predictRouteCompletion(Integer routeId);

    List<UserPreferenceRecommendation> recommendRoutesByUserPreference(Integer userId, Integer cityId, int days, Map<String, Object> preferences);

    RouteComparisonResult compareRoutes(List<Integer> routeIds);

    RealTimeAdjustmentResult getRealTimeAdjustment(Integer routeId, RealTimeAdjustmentRequest request);

    RouteQualityEvaluation evaluateRouteQuality(Integer routeId, RouteQualityEvaluationRequest request);

    PersonalizedRouteResult generatePersonalizedRoute(PersonalizedRoutePreferences userPreferences,
                                                       PersonalizedRouteConstraints constraints);

    List<SmartRouteItem> getPopularRoutes(Integer cityId, int days, int limit);

    List<SmartRouteItem> getSimilarRoutes(Integer routeId, int limit);

    List<SmartRouteItem> getSeasonalRoutes(Integer cityId, String season, int days);

    List<SmartRouteItem> getThemeRoutes(String theme, Integer cityId, int days);

    RouteOptimizationSuggestionResult getRouteOptimizationSuggestions(Integer routeId, String optimizationType);

    List<SmartRouteItem> recommendRoutesByUserHistory(Integer userId, Integer cityId, int days, int limit);

    List<SmartRouteItem> recommendRoutesByPopularAttractions(Integer cityId, int days, int limit);

    List<SmartRouteItem> recommendRoutesBySeasonAndWeather(Integer cityId, int days, String season, String weather, int limit);

    List<SmartRouteItem> recommendRoutesBySocialNetwork(Integer userId, Integer cityId, int days, int limit);

    List<SmartRouteItem> recommendRoutesByRating(Integer cityId, int days, int limit);

    List<SmartRouteItem> recommendRoutesBySimilarity(Integer routeId, int limit);

    MultiDayRouteResult generateMultiDayRoute(Integer cityId, String startDate, String endDate, Map<String, Object> userPreferences);

    RouteRecommendationReason getRouteRecommendationReason(Integer routeId, Integer userId);
}
