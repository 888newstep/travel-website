package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.service.travel_recommendation.AIAdvancedService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 高级AI功能控制器
 * 提供高级AI功能，如聊天、个性化推荐、语音处理、图像分析等
 */
@RestController
@RequestMapping("/ai-advanced")
@Slf4j
@RequiredArgsConstructor
public class AIAdvancedController {

    private final AIAdvancedService aiAdvancedService;

    /**
     * AI聊天
     * POST /api/ai-advanced/chat
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chatWithAI(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            String sessionId = (String) request.get("sessionId");
            log.info("AI聊天请求: sessionId={}, message={}", sessionId, message);
            Map<String, Object> response = aiAdvancedService.chatWithAI(message, sessionId);
            return Result.success("AI聊天成功", response);
        } catch (Exception e) {
            log.error("AI聊天失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 个性化推荐
     * GET /api/ai-advanced/recommendations
     */
    @GetMapping("/recommendations")
    public Result<List<Map<String, Object>>> getPersonalizedRecommendations(
            @RequestParam Integer userId,
            @RequestParam String recommendationType,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("个性化推荐请求: userId={}, type={}, limit={}", userId, recommendationType, limit);
            List<Map<String, Object>> recommendations = aiAdvancedService.getPersonalizedRecommendations(
                    userId, recommendationType, limit);
            return Result.success("个性化推荐成功", recommendations);
        } catch (Exception e) {
            log.error("个性化推荐失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 处理语音请求
     * POST /api/ai-advanced/voice
     */
    @PostMapping("/voice")
    public Result<Map<String, Object>> processVoiceRequest(@RequestBody Map<String, Object> request) {
        try {
            byte[] audioData = (byte[]) request.get("audioData");
            log.info("处理语音请求: audioData length={}", audioData != null ? audioData.length : 0);
            Map<String, Object> result = aiAdvancedService.processVoiceRequest(audioData);
            return Result.success("语音处理成功", result);
        } catch (Exception e) {
            log.error("语音处理失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分析图像
     * POST /api/ai-advanced/image
     */
    @PostMapping("/image")
    public Result<Map<String, Object>> analyzeImage(@RequestBody Map<String, Object> request) {
        try {
            byte[] imageData = (byte[]) request.get("imageData");
            String analysisType = (String) request.get("analysisType");
            log.info("分析图像请求: analysisType={}", analysisType);
            Map<String, Object> result = aiAdvancedService.analyzeImage(imageData, analysisType);
            return Result.success("图像分析成功", result);
        } catch (Exception e) {
            log.error("图像分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能路线规划
     * POST /api/ai-advanced/plan
     */
    @PostMapping("/plan")
    public Result<Map<String, Object>> planRoute(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = (Map<String, Object>) request.get("preferences");
            @SuppressWarnings("unchecked")
            Map<String, Object> constraints = (Map<String, Object>) request.get("constraints");
            log.info("智能路线规划请求");
            Map<String, Object> route = aiAdvancedService.planRoute(preferences, constraints);
            return Result.success("智能路线规划成功", route);
        } catch (Exception e) {
            log.error("智能路线规划失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成旅游攻略
     * POST /api/ai-advanced/guide
     */
    @PostMapping("/guide")
    public Result<Map<String, Object>> generateTravelGuide(@RequestBody Map<String, Object> request) {
        try {
            Integer cityId = (Integer) request.get("cityId");
            Integer days = (Integer) request.get("days");
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = (Map<String, Object>) request.get("preferences");
            log.info("生成旅游攻略请求: cityId={}, days={}", cityId, days);
            Map<String, Object> guide = aiAdvancedService.generateTravelGuide(cityId, days, preferences);
            return Result.success("生成旅游攻略成功", guide);
        } catch (Exception e) {
            log.error("生成旅游攻略失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 多语言翻译
     * POST /api/ai-advanced/translate
     */
    @PostMapping("/translate")
    public Result<Map<String, Object>> translate(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            String sourceLanguage = (String) request.get("sourceLanguage");
            String targetLanguage = (String) request.get("targetLanguage");
            log.info("多语言翻译请求: source={}, target={}", sourceLanguage, targetLanguage);
            Map<String, Object> result = aiAdvancedService.translate(text, sourceLanguage, targetLanguage);
            return Result.success("多语言翻译成功", result);
        } catch (Exception e) {
            log.error("多语言翻译失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 情感分析
     * POST /api/ai-advanced/sentiment
     */
    @PostMapping("/sentiment")
    public Result<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            log.info("情感分析请求: text length={}", text != null ? text.length() : 0);
            Map<String, Object> result = aiAdvancedService.analyzeSentiment(text);
            return Result.success("情感分析成功", result);
        } catch (Exception e) {
            log.error("情感分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 旅游预算估算
     * POST /api/ai-advanced/budget
     */
    @PostMapping("/budget")
    public Result<Map<String, Object>> estimateBudget(@RequestBody Map<String, Object> request) {
        try {
            Integer cityId = (Integer) request.get("cityId");
            Integer days = (Integer) request.get("days");
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = (Map<String, Object>) request.get("preferences");
            log.info("旅游预算估算请求: cityId={}, days={}", cityId, days);
            Map<String, Object> budget = aiAdvancedService.estimateBudget(cityId, days, preferences);
            return Result.success("旅游预算估算成功", budget);
        } catch (Exception e) {
            log.error("旅游预算估算失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取旅游安全建议
     * GET /api/ai-advanced/safety/{cityId}
     */
    @GetMapping("/safety/{cityId}")
    public Result<Map<String, Object>> getSafetyAdvice(@PathVariable Integer cityId) {
        try {
            log.info("获取旅游安全建议请求: cityId={}", cityId);
            Map<String, Object> advice = aiAdvancedService.getSafetyAdvice(cityId);
            return Result.success("获取旅游安全建议成功", advice);
        } catch (Exception e) {
            log.error("获取旅游安全建议失败: cityId={}, error={}", cityId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 回答旅游相关问题
     * POST /api/ai-advanced/answer
     */
    @PostMapping("/answer")
    public Result<Map<String, Object>> answerQuestion(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            log.info("回答旅游相关问题请求: question={}", question);
            Map<String, Object> answer = aiAdvancedService.answerQuestion(question);
            return Result.success("回答旅游相关问题成功", answer);
        } catch (Exception e) {
            log.error("回答旅游相关问题失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 增强景点识别
     * POST /api/ai-advanced/recognize
     */
    @PostMapping("/recognize")
    public Result<Map<String, Object>> enhancedAttractionRecognition(@RequestBody Map<String, Object> request) {
        try {
            byte[] imageData = (byte[]) request.get("imageData");
            @SuppressWarnings("unchecked")
            Map<String, Double> location = (Map<String, Double>) request.get("location");
            log.info("增强景点识别请求");
            Map<String, Object> result = aiAdvancedService.enhancedAttractionRecognition(imageData, location);
            return Result.success("增强景点识别成功", result);
        } catch (Exception e) {
            log.error("增强景点识别失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能行程优化
     * POST /api/ai-advanced/optimize-itinerary
     */
    @PostMapping("/optimize-itinerary")
    public Result<Map<String, Object>> optimizeItinerary(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> itinerary = (Map<String, Object>) request.get("itinerary");
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = (Map<String, Object>) request.get("preferences");
            @SuppressWarnings("unchecked")
            Map<String, Object> constraints = (Map<String, Object>) request.get("constraints");
            log.info("智能行程优化请求");
            Map<String, Object> result = aiAdvancedService.optimizeItinerary(itinerary, preferences, constraints);
            return Result.success("智能行程优化成功", result);
        } catch (Exception e) {
            log.error("智能行程优化失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 增强智能问答
     * POST /api/ai-advanced/enhanced-qa
     */
    @PostMapping("/enhanced-qa")
    public Result<Map<String, Object>> enhancedQuestionAnswering(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>) request.get("context");
            Integer userId = (Integer) request.get("userId");
            log.info("增强智能问答请求: question={}", question);
            Map<String, Object> result = aiAdvancedService.enhancedQuestionAnswering(question, context, userId);
            return Result.success("增强智能问答成功", result);
        } catch (Exception e) {
            log.error("增强智能问答失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 个性化旅游建议
     * POST /api/ai-advanced/travel-advice
     */
    @PostMapping("/travel-advice")
    public Result<Map<String, Object>> getPersonalizedTravelAdvice(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = (Integer) request.get("userId");
            String tripType = (String) request.get("tripType");
            Integer duration = (Integer) request.get("duration");
            Double budget = (Double) request.get("budget");
            log.info("个性化旅游建议请求: userId={}, tripType={}", userId, tripType);
            Map<String, Object> advice = aiAdvancedService.getPersonalizedTravelAdvice(userId, tripType, duration, budget);
            return Result.success("个性化旅游建议成功", advice);
        } catch (Exception e) {
            log.error("个性化旅游建议失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 旅游热点分析
     * POST /api/ai-advanced/hotspots
     */
    @PostMapping("/hotspots")
    public Result<Map<String, Object>> analyzeTravelHotspots(@RequestBody Map<String, Object> request) {
        try {
            String region = (String) request.get("region");
            @SuppressWarnings("unchecked")
            Map<String, String> timeRange = (Map<String, String>) request.get("timeRange");
            log.info("旅游热点分析请求: region={}", region);
            Map<String, Object> result = aiAdvancedService.analyzeTravelHotspots(region, timeRange);
            return Result.success("旅游热点分析成功", result);
        } catch (Exception e) {
            log.error("旅游热点分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 多模态交互
     * POST /api/ai-advanced/multimodal
     */
    @PostMapping("/multimodal")
    public Result<Map<String, Object>> multimodalInteraction(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) request.get("requestData");
            String sessionId = (String) request.get("sessionId");
            log.info("多模态交互请求: sessionId={}", sessionId);
            Map<String, Object> result = aiAdvancedService.multimodalInteraction(requestData, sessionId);
            return Result.success("多模态交互成功", result);
        } catch (Exception e) {
            log.error("多模态交互失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
