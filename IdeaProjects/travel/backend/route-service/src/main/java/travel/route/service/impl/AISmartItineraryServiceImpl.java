package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.route_planning.Route;
import travel.route.service.RouteService;
import travel.route.service.AISmartItineraryService;
import travel.common.utils.CacheUtil;
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

    private final RouteService routeService;
    private final CacheUtil cacheUtil;

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
}
