package travel.service.impl.travel_recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AISmartItineraryService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AISmartItineraryServiceImpl implements AISmartItineraryService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String ITINERARY_PREFIX = "ai:itinerary:";

    @Override
    public Map<String, Object> generateItinerary(Map<String, Object> userPreferences, double budget, int days, Integer cityId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        // 生成缓存键
        String cacheKey = ITINERARY_PREFIX + "generate:" + userId + ":" + cityId + ":" + days;
        Object cachedObj = cacheUtil.get(cacheKey, Object.class);
        if (cachedObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) cachedObj;
            return cached;
        }

        // 模拟智能行程生成
        List<Map<String, Object>> dailyPlans = new ArrayList<>();

        for (int day = 1; day <= days; day++) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("day", day);
            dayPlan.put("title", "第" + day + "天行程");
            
            List<Map<String, Object>> activities = new ArrayList<>();
            
            // 上午活动
            Map<String, Object> morningActivity = new HashMap<>();
            morningActivity.put("time", "09:00-12:00");
            morningActivity.put("type", "attraction");
            morningActivity.put("name", "著名景点" + day);
            morningActivity.put("description", "上午游览当地著名景点");
            morningActivity.put("duration", 3);
            morningActivity.put("cost", 100.0);
            activities.add(morningActivity);

            // 午餐
            Map<String, Object> lunchActivity = new HashMap<>();
            lunchActivity.put("time", "12:00-13:30");
            lunchActivity.put("type", "restaurant");
            lunchActivity.put("name", "特色餐厅" + day);
            lunchActivity.put("description", "品尝当地特色美食");
            lunchActivity.put("duration", 1.5);
            lunchActivity.put("cost", 80.0);
            activities.add(lunchActivity);

            // 下午活动
            Map<String, Object> afternoonActivity = new HashMap<>();
            afternoonActivity.put("time", "14:00-17:00");
            afternoonActivity.put("type", "attraction");
            afternoonActivity.put("name", "文化景点" + day);
            afternoonActivity.put("description", "参观文化景点，了解当地历史");
            afternoonActivity.put("duration", 3);
            afternoonActivity.put("cost", 120.0);
            activities.add(afternoonActivity);

            // 晚餐
            Map<String, Object> dinnerActivity = new HashMap<>();
            dinnerActivity.put("time", "18:00-19:30");
            dinnerActivity.put("type", "restaurant");
            dinnerActivity.put("name", "晚餐餐厅" + day);
            dinnerActivity.put("description", "享用晚餐");
            dinnerActivity.put("duration", 1.5);
            dinnerActivity.put("cost", 100.0);
            activities.add(dinnerActivity);

            dayPlan.put("activities", activities);
            dayPlan.put("totalCost", 400.0);
            dayPlan.put("totalDuration", 9.0);

            dailyPlans.add(dayPlan);
        }

        // 生成行程摘要
        double totalCost = dailyPlans.stream().mapToDouble(plan -> (double) plan.get("totalCost")).sum();
        double totalDuration = dailyPlans.stream().mapToDouble(plan -> (double) plan.get("totalDuration")).sum();

        result.put("success", true);
        result.put("cityId", cityId);
        result.put("days", days);
        result.put("budget", budget);
        result.put("estimatedCost", totalCost);
        result.put("estimatedDuration", totalDuration);
        result.put("dailyPlans", dailyPlans);
        result.put("generatedAt", LocalDateTime.now());
        result.put("optimizationScore", 85);

        // 缓存结果
        cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);

        return result;
    }

    @Override
    public Map<String, Object> optimizeItinerary(Integer routeId, Map<String, Object> userPreferences) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        // 模拟优化过程
        List<Map<String, Object>> optimizations = new ArrayList<>();

        // 时间优化
        Map<String, Object> timeOptimization = new HashMap<>();
        timeOptimization.put("type", "time");
        timeOptimization.put("description", "优化行程时间安排，避开人流高峰");
        timeOptimization.put("benefit", "节省约2小时等待时间");
        timeOptimization.put("priority", "high");
        optimizations.add(timeOptimization);

        // 成本优化
        Map<String, Object> costOptimization = new HashMap<>();
        costOptimization.put("type", "cost");
        costOptimization.put("description", "优化交通和餐饮安排，降低旅行成本");
        costOptimization.put("benefit", "节省约15%的旅行费用");
        costOptimization.put("priority", "medium");
        optimizations.add(costOptimization);

        // 体验优化
        Map<String, Object> experienceOptimization = new HashMap<>();
        experienceOptimization.put("type", "experience");
        experienceOptimization.put("description", "根据用户偏好调整景点顺序和停留时间");
        experienceOptimization.put("benefit", "提升旅行体验满意度");
        experienceOptimization.put("priority", "high");
        optimizations.add(experienceOptimization);

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("optimizations", optimizations);
        result.put("optimizedScore", 92);
        result.put("estimatedSavings", 350.0);
        result.put("optimizedAt", LocalDateTime.now());

        return result;
    }

    @Override
    public Map<String, Object> adjustItinerary(Integer routeId, Map<String, Object> realTimeData) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        // 模拟实时调整
        List<Map<String, Object>> adjustments = new ArrayList<>();

        // 天气调整
        if (realTimeData.containsKey("weather") && "rainy".equals(realTimeData.get("weather"))) {
            Map<String, Object> weatherAdjustment = new HashMap<>();
            weatherAdjustment.put("type", "weather");
            weatherAdjustment.put("description", "因天气原因，调整户外活动为室内景点");
            weatherAdjustment.put("originalActivity", "户外活动");
            weatherAdjustment.put("adjustedActivity", "博物馆参观");
            adjustments.add(weatherAdjustment);
        }

        // 交通调整
        if (realTimeData.containsKey("traffic") && "heavy".equals(realTimeData.get("traffic"))) {
            Map<String, Object> trafficAdjustment = new HashMap<>();
            trafficAdjustment.put("type", "traffic");
            trafficAdjustment.put("description", "因交通拥堵，调整出行时间和路线");
            trafficAdjustment.put("delay", "30分钟");
            trafficAdjustment.put("alternativeRoute", "建议使用公共交通");
            adjustments.add(trafficAdjustment);
        }

        // 人流调整
        if (realTimeData.containsKey("crowd") && "crowded".equals(realTimeData.get("crowd"))) {
            Map<String, Object> crowdAdjustment = new HashMap<>();
            crowdAdjustment.put("type", "crowd");
            crowdAdjustment.put("description", "因景点拥挤，调整参观顺序");
            crowdAdjustment.put("recommendedTime", "下午15:00后");
            adjustments.add(crowdAdjustment);
        }

        result.put("success", true);
        result.put("routeId", routeId);
        result.put("adjustments", adjustments);
        result.put("adjustedAt", LocalDateTime.now());
        result.put("adjustmentScore", 88);

        return result;
    }

    @Override
    public List<Map<String, Object>> generateAlternatives(Integer routeId, int count) {
        List<Map<String, Object>> alternatives = new ArrayList<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            return alternatives;
        }

        // 生成备选行程
        for (int i = 1; i <= count; i++) {
            Map<String, Object> alternative = new HashMap<>();
            alternative.put("id", i);
            alternative.put("routeId", routeId);
            alternative.put("title", "备选行程 " + i);
            alternative.put("description", "基于原行程的第" + i + "种备选方案");
            alternative.put("focus", getAlternativeFocus(i));
            alternative.put("estimatedCost", 2000 + i * 200);
            alternative.put("duration", 3 + i);
            alternative.put("score", 85 + i * 2);

            alternatives.add(alternative);
        }

        return alternatives;
    }

    @Override
    public Map<String, Object> predictSatisfaction(Map<String, Object> itinerary, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        // 模拟满意度预测
        double baseScore = 80.0;
        
        // 根据行程特征调整分数
        if (itinerary.containsKey("dailyPlans")) {
            Object dailyPlansObj = itinerary.get("dailyPlans");
            if (dailyPlansObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dailyPlans = (List<Map<String, Object>>) dailyPlansObj;
                int days = dailyPlans.size();
                
                if (days >= 3 && days <= 5) {
                    baseScore += 5.0; // 合理的行程天数
                }
            }
        }

        if (itinerary.containsKey("estimatedCost")) {
            double cost = (double) itinerary.get("estimatedCost");
            if (cost < 3000) {
                baseScore += 3.0; // 合理的预算
            }
        }

        // 生成详细的满意度预测
        result.put("overallScore", baseScore);
        result.put("breakdown", Map.of(
                "attractions", baseScore + 2,
                "transportation", baseScore - 1,
                "accommodation", baseScore,
                "food", baseScore + 3,
                "overallExperience", baseScore
        ));

        result.put("recommendations", List.of(
                "建议增加一些自由活动时间",
                "考虑预订热门景点的优先入场权",
                "建议提前安排交通预订"
        ));

        result.put("confidence", 0.85);
        result.put("predictedAt", LocalDateTime.now());

        return result;
    }

    // 辅助方法：获取备选行程的侧重点
    private String getAlternativeFocus(int index) {
        String[] focuses = {
            "文化体验",
            "美食探索",
            "自然风光",
            "休闲度假",
            "历史遗迹"
        };
        return focuses[(index - 1) % focuses.length];
    }
}
