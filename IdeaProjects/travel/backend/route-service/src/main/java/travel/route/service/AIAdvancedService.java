package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import travel.route.dto.ai.AIPersonalizedRecommendationItem;
import travel.route.dto.ai.AIBudgetDetails;
import travel.route.dto.ai.AIPlanRouteConstraints;
import travel.route.dto.ai.AIPlanRoutePreferences;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AISafetyAdviceResponse;
import travel.route.dto.ai.AITravelGuideContent;

import java.util.List;
import java.util.Map;

public interface AIAdvancedService {

    List<AIPersonalizedRecommendationItem> getPersonalizedRecommendations(Integer userId, String recommendationType, int limit);

    AIPlanRouteResponse planRoute(AIPlanRoutePreferences preferences, AIPlanRouteConstraints constraints);

    AITravelGuideContent generateTravelGuide(Integer cityId, int days, Map<String, JsonNode> preferences);

    AIBudgetDetails estimateBudget(Integer cityId, int days, Map<String, JsonNode> preferences);

    AISafetyAdviceResponse getSafetyAdvice(Integer cityId);
}
