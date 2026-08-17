package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AISmartItineraryOptimizeResponse;
import travel.route.dto.ai.AISmartItineraryActivity;
import travel.route.dto.ai.AISmartItineraryDayPlan;
import travel.route.dto.ai.AISmartItineraryOptimization;
import travel.route.dto.ai.AISmartItineraryOptimizationItem;
import travel.route.dto.ai.AISmartItineraryPlan;
import travel.route.dto.ai.AISmartItineraryResponse;
import travel.route.service.AISmartItineraryService;
import travel.route.service.RouteService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AISmartItineraryServiceImpl implements AISmartItineraryService {

    private static final String ITINERARY_PREFIX = "ai:itinerary:v2:";

    private final RouteService routeService;
    private final CacheUtil cacheUtil;

    @Override
    public AISmartItineraryResponse generateItinerary(Map<String, JsonNode> userPreferences, double budget, int days, Integer cityId, Integer userId) {
        AISmartItineraryPlan itinerary = buildItinerary(userPreferences, budget, days, cityId, userId);
        return buildGenerateResponse(userId, cityId, days, budget, itinerary);
    }

    @Override
    public AISmartItineraryOptimizeResponse optimizeItinerary(Integer routeId, Map<String, JsonNode> userPreferences) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new IllegalArgumentException("路线不存在");
        }

        AISmartItineraryOptimization optimized = buildOptimization(routeId, route, userPreferences);
        return AISmartItineraryOptimizeResponse.builder()
                .routeId(routeId)
                .optimized(optimized)
                .source("ai-smart-itinerary")
                .build();
    }

    private AISmartItineraryPlan buildItinerary(Map<String, JsonNode> userPreferences,
                                                double budget, int days, Integer cityId, Integer userId) {
        String preferenceKey = userPreferences == null || userPreferences.isEmpty()
                ? "none"
                : Integer.toHexString(userPreferences.hashCode());
        String cacheKey = CacheUtil.generateKey(
                ITINERARY_PREFIX + "generate", userId, cityId, days, budget, preferenceKey);
        AISmartItineraryPlan cachedPlan = cacheUtil.get(cacheKey, AISmartItineraryPlan.class);
        if (cachedPlan != null) {
            return cachedPlan;
        }

        List<AISmartItineraryDayPlan> dailyPlans = new ArrayList<>(days);
        for (int day = 1; day <= days; day++) {
            List<AISmartItineraryActivity> activities = List.of(
                    activity("09:00-12:00", "attraction", "著名景点" + day,
                            "上午游览当地著名景点", 3, 100.0),
                    activity("12:00-13:30", "restaurant", "特色餐厅" + day,
                            "品尝当地特色美食", 1.5, 80.0),
                    activity("14:00-17:00", "attraction", "文化景点" + day,
                            "参观文化景点，了解当地历史", 3, 120.0),
                    activity("18:00-19:30", "restaurant", "晚餐餐厅" + day,
                            "享用晚餐", 1.5, 100.0));
            dailyPlans.add(AISmartItineraryDayPlan.builder()
                    .day(day)
                    .title("第" + day + "天行程")
                    .activities(activities)
                    .totalCost(400.0)
                    .totalDuration(9.0)
                    .build());
        }

        double totalCost = dailyPlans.stream()
                .mapToDouble(plan -> plan.getTotalCost())
                .sum();
        double totalDuration = dailyPlans.stream()
                .mapToDouble(plan -> plan.getTotalDuration())
                .sum();

        AISmartItineraryPlan result = AISmartItineraryPlan.builder()
                .success(true)
                .userId(userId)
                .cityId(cityId)
                .days(days)
                .budget(budget)
                .preferences(copyPreferences(userPreferences))
                .estimatedCost(totalCost)
                .estimatedDuration(totalDuration)
                .dailyPlans(dailyPlans)
                .generatedAt(LocalDateTime.now())
                .optimizationScore(85)
                .build();

        cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);
        return result;
    }

    private AISmartItineraryOptimization buildOptimization(Integer routeId, Route route,
                                                           Map<String, JsonNode> userPreferences) {
        List<AISmartItineraryOptimizationItem> optimizations = List.of(
                optimization("time", "优化行程时间安排，避开人流高峰", "节省约2小时等待时间", "high"),
                optimization("cost", "优化交通和餐饮安排，降低旅行成本", "节省约15%的旅行费用", "medium"),
                optimization("experience", "根据用户偏好调整景点顺序和停留时间", "提升旅行体验满意度", "high"));

        return AISmartItineraryOptimization.builder()
                .success(true)
                .routeId(routeId)
                .routeTitle(route.getTitle())
                .cityName(route.getCity() != null ? route.getCity().getName() : null)
                .preferences(copyPreferences(userPreferences))
                .optimizations(optimizations)
                .optimizedScore(92)
                .estimatedSavings(350.0)
                .optimizedAt(LocalDateTime.now())
                .build();
    }

    private AISmartItineraryResponse buildGenerateResponse(Integer userId, Integer cityId, int days,
                                                           double budget, AISmartItineraryPlan itinerary) {
        return AISmartItineraryResponse.builder()
                .userId(userId)
                .cityId(cityId)
                .days(days)
                .budget(budget)
                .itinerary(itinerary)
                .source("ai-smart-itinerary")
                .build();
    }

    private AISmartItineraryActivity activity(String time, String type, String name, String description,
                                              double duration, double cost) {
        return AISmartItineraryActivity.builder()
                .time(time)
                .type(type)
                .name(name)
                .description(description)
                .duration(duration)
                .cost(cost)
                .build();
    }

    private AISmartItineraryOptimizationItem optimization(String type, String description,
                                                           String benefit, String priority) {
        return AISmartItineraryOptimizationItem.builder()
                .type(type)
                .description(description)
                .benefit(benefit)
                .priority(priority)
                .build();
    }

    private Map<String, JsonNode> copyPreferences(Map<String, JsonNode> userPreferences) {
        return userPreferences == null || userPreferences.isEmpty()
                ? Map.of()
                : new LinkedHashMap<>(userPreferences);
    }
}
