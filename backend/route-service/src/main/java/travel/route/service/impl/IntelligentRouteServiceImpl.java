package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.route.dto.route.MultiDayRouteResult;
import travel.route.dto.route.PersonalizedRouteConstraints;
import travel.route.dto.route.PersonalizedRoutePreferences;
import travel.route.dto.route.PersonalizedRouteResult;
import travel.route.dto.route.RealTimeAdjustmentRequest;
import travel.route.dto.route.RealTimeAdjustmentResult;
import travel.route.dto.route.RouteComparisonResult;
import travel.route.dto.route.RouteOptimizationSuggestionResult;
import travel.route.dto.route.RouteQualityEvaluation;
import travel.route.dto.route.RouteQualityEvaluationRequest;
import travel.route.dto.route.RouteRecommendationReason;
import travel.route.dto.route.SmartRouteItem;
import travel.route.dto.route.UserPreferenceRecommendation;
import travel.route.service.IntelligentRouteEvaluationService;
import travel.route.service.IntelligentRoutePersonalizationService;
import travel.route.service.IntelligentRouteRealtimeAdjustmentService;
import travel.route.service.IntelligentRouteRecommendationService;
import travel.route.service.IntelligentRouteService;
import travel.route.service.RouteService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IntelligentRouteServiceImpl implements IntelligentRouteService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRouteServiceImpl.class);

    private final RouteService routeService;
    private final IntelligentRouteEvaluationService intelligentRouteEvaluationService;
    private final IntelligentRouteRecommendationService intelligentRouteRecommendationService;
    private final IntelligentRouteRealtimeAdjustmentService intelligentRouteRealtimeAdjustmentService;
    private final IntelligentRoutePersonalizationService intelligentRoutePersonalizationService;

    @Override
    public List<Route> getSmartRouteRecommendations(Integer cityId, int days) {
        log.info("Get smart route recommendations: cityId={}, days={}", cityId, days);
        return routeService.list();
    }

    @Override
    public String optimizeRoute(Integer routeId) {
        log.info("Optimize route: routeId={}", routeId);
        return "璺嚎浼樺寲鎴愬姛";
    }

    @Override
    public String predictRouteCompletion(Integer routeId) {
        log.info("Predict route completion: routeId={}", routeId);
        return "璺嚎棰勬祴鎴愬姛";
    }

    @Override
    public List<UserPreferenceRecommendation> recommendRoutesByUserPreference(Integer userId, Integer cityId, int days, Map<String, Object> preferences) {
        return intelligentRoutePersonalizationService.recommendRoutesByUserPreference(userId, cityId, days, preferences);
    }

    @Override
    public RouteComparisonResult compareRoutes(List<Integer> routeIds) {
        return intelligentRouteEvaluationService.compareRoutes(routeIds);
    }

    @Override
    public RealTimeAdjustmentResult getRealTimeAdjustment(Integer routeId, RealTimeAdjustmentRequest request) {
        return intelligentRouteRealtimeAdjustmentService.getRealTimeAdjustment(routeId, request);
    }

    @Override
    public RouteQualityEvaluation evaluateRouteQuality(Integer routeId, RouteQualityEvaluationRequest request) {
        return intelligentRouteEvaluationService.evaluateRouteQuality(routeId, request);
    }

    @Override
    public PersonalizedRouteResult generatePersonalizedRoute(PersonalizedRoutePreferences userPreferences,
                                                             PersonalizedRouteConstraints constraints) {
        return intelligentRoutePersonalizationService.generatePersonalizedRoute(userPreferences, constraints);
    }

    @Override
    public List<SmartRouteItem> getPopularRoutes(Integer cityId, int days, int limit) {
        return intelligentRouteRecommendationService.getPopularRoutes(cityId, days, limit);
    }

    @Override
    public List<SmartRouteItem> getSimilarRoutes(Integer routeId, int limit) {
        return intelligentRouteRecommendationService.getSimilarRoutes(routeId, limit);
    }

    @Override
    public List<SmartRouteItem> getSeasonalRoutes(Integer cityId, String season, int days) {
        return intelligentRouteRecommendationService.getSeasonalRoutes(cityId, season, days);
    }

    @Override
    public List<SmartRouteItem> getThemeRoutes(String theme, Integer cityId, int days) {
        return intelligentRouteRecommendationService.getThemeRoutes(theme, cityId, days);
    }

    @Override
    public RouteOptimizationSuggestionResult getRouteOptimizationSuggestions(Integer routeId, String optimizationType) {
        return intelligentRouteEvaluationService.getRouteOptimizationSuggestions(routeId, optimizationType);
    }

    @Override
    public List<SmartRouteItem> recommendRoutesByUserHistory(Integer userId, Integer cityId, int days, int limit) {
        return intelligentRouteRecommendationService.recommendRoutesByUserHistory(userId, cityId, days, limit);
    }

    @Override
    public List<SmartRouteItem> recommendRoutesByPopularAttractions(Integer cityId, int days, int limit) {
        return intelligentRouteRecommendationService.recommendRoutesByPopularAttractions(cityId, days, limit);
    }

    @Override
    public List<SmartRouteItem> recommendRoutesBySeasonAndWeather(Integer cityId, int days, String season, String weather, int limit) {
        return intelligentRouteRecommendationService.recommendRoutesBySeasonAndWeather(cityId, days, season, weather, limit);
    }

    @Override
    public List<SmartRouteItem> recommendRoutesBySocialNetwork(Integer userId, Integer cityId, int days, int limit) {
        return intelligentRouteRecommendationService.recommendRoutesBySocialNetwork(userId, cityId, days, limit);
    }

    @Override
    public List<SmartRouteItem> recommendRoutesByRating(Integer cityId, int days, int limit) {
        return intelligentRouteRecommendationService.recommendRoutesByRating(cityId, days, limit);
    }

    @Override
    public List<SmartRouteItem> recommendRoutesBySimilarity(Integer routeId, int limit) {
        return intelligentRouteRecommendationService.recommendRoutesBySimilarity(routeId, limit);
    }

    @Override
    public MultiDayRouteResult generateMultiDayRoute(Integer cityId, String startDate, String endDate, Map<String, Object> userPreferences) {
        return intelligentRoutePersonalizationService.generateMultiDayRoute(cityId, startDate, endDate, userPreferences);
    }

    @Override
    public RouteRecommendationReason getRouteRecommendationReason(Integer routeId, Integer userId) {
        return intelligentRoutePersonalizationService.getRouteRecommendationReason(routeId, userId);
    }
}
