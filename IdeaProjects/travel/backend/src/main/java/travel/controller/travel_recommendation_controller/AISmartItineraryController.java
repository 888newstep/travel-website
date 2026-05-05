package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.service.travel_recommendation.AISmartItineraryService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI智能行程控制器
 * 处理基于AI的智能行程规划和推荐
 */
@RestController
@RequestMapping("/ai-smart-itinerary")
@RequiredArgsConstructor
public class AISmartItineraryController {

    private static final Logger log = LoggerFactory.getLogger(AISmartItineraryController.class);

    private final AISmartItineraryService aiSmartItineraryService;

    /**
     * 生成智能行程
     * POST /api/ai-smart-itinerary/generate
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateSmartItinerary(@RequestBody Map<String, Object> itineraryRequest) {
        try {
            log.info("生成智能行程请求: userId={}, cityId={}", itineraryRequest.get("userId"), itineraryRequest.get("cityId"));
            // 从请求中提取参数
            Integer userId = (Integer) itineraryRequest.get("userId");
            Integer cityId = (Integer) itineraryRequest.get("cityId");
            int days = (int) itineraryRequest.getOrDefault("days", 3);
            double budget = (double) itineraryRequest.getOrDefault("budget", 1000.0);
            Map<String, Object> userPreferences = new java.util.HashMap<>();
            Object preferencesObj = itineraryRequest.get("preferences");
            if (preferencesObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) preferencesObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        userPreferences.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            
            Map<String, Object> itinerary = aiSmartItineraryService.generateItinerary(userPreferences, budget, days, cityId, userId);
            return Result.success("生成智能行程成功", itinerary);
        } catch (Exception e) {
            log.error("生成智能行程失败: error={}", e.getMessage());
            return Result.error("生成智能行程失败: " + e.getMessage());
        }
    }

    /**
     * 优化行程
     * POST /api/ai-smart-itinerary/optimize
     */
    @PostMapping("/optimize")
    public Result<Map<String, Object>> optimizeItinerary(@RequestBody Map<String, Object> itineraryData) {
        try {
            log.info("优化行程请求: routeId={}", itineraryData.get("routeId"));
            Integer routeId = (Integer) itineraryData.get("routeId");
            Map<String, Object> userPreferences = new java.util.HashMap<>();
            Object preferencesObj = itineraryData.get("preferences");
            if (preferencesObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) preferencesObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        userPreferences.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            
            Map<String, Object> optimized = aiSmartItineraryService.optimizeItinerary(routeId, userPreferences);
            return Result.success("优化行程成功", optimized);
        } catch (Exception e) {
            log.error("优化行程失败: error={}", e.getMessage());
            return Result.error("优化行程失败: " + e.getMessage());
        }
    }

    /**
     * 调整行程
     * POST /api/ai-smart-itinerary/adjust
     */
    @PostMapping("/adjust")
    public Result<Map<String, Object>> adjustItinerary(@RequestBody Map<String, Object> itineraryData) {
        try {
            log.info("调整行程请求: routeId={}", itineraryData.get("routeId"));
            Integer routeId = (Integer) itineraryData.get("routeId");
            Map<String, Object> realTimeData = new java.util.HashMap<>();
            Object realTimeDataObj = itineraryData.get("realTimeData");
            if (realTimeDataObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) realTimeDataObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        realTimeData.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            
            Map<String, Object> adjusted = aiSmartItineraryService.adjustItinerary(routeId, realTimeData);
            return Result.success("调整行程成功", adjusted);
        } catch (Exception e) {
            log.error("调整行程失败: error={}", e.getMessage());
            return Result.error("调整行程失败: " + e.getMessage());
        }
    }

    /**
     * 生成备选行程
     * POST /api/ai-smart-itinerary/alternatives
     */
    @PostMapping("/alternatives")
    public Result<List<Map<String, Object>>> generateAlternatives(@RequestBody Map<String, Object> request) {
        try {
            log.info("生成备选行程请求: routeId={}, count={}", request.get("routeId"), request.get("count"));
            Integer routeId = (Integer) request.get("routeId");
            int count = (int) request.getOrDefault("count", 5);
            
            List<Map<String, Object>> alternatives = aiSmartItineraryService.generateAlternatives(routeId, count);
            return Result.success("生成备选行程成功", alternatives);
        } catch (Exception e) {
            log.error("生成备选行程失败: error={}", e.getMessage());
            return Result.error("生成备选行程失败: " + e.getMessage());
        }
    }

    /**
     * 预测行程满意度
     * POST /api/ai-smart-itinerary/predict-satisfaction
     */
    @PostMapping("/predict-satisfaction")
    public Result<Map<String, Object>> predictSatisfaction(@RequestBody Map<String, Object> request) {
        try {
            log.info("预测行程满意度请求: userId={}", request.get("userId"));
            Integer userId = (Integer) request.get("userId");
            Map<String, Object> itinerary = new java.util.HashMap<>();
            Object itineraryObj = request.get("itinerary");
            if (itineraryObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) itineraryObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        itinerary.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            
            Map<String, Object> prediction = aiSmartItineraryService.predictSatisfaction(itinerary, userId);
            return Result.success("预测行程满意度成功", prediction);
        } catch (Exception e) {
            log.error("预测行程满意度失败: error={}", e.getMessage());
            return Result.error("预测行程满意度失败: " + e.getMessage());
        }
    }
}
