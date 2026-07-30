package travel.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import travel.route.service.*;
import travel.common.utils.Result;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
@Tag(name = "AI智能服务", description = "AI对话、推荐、图像分析、智能助手、多模态等接口")
@RequiredArgsConstructor
public class AIController {

    private final QwenService qwenService;
    private final BaiduAIService baiduAIService;
    private final AIImageAnalysisService aiImageAnalysisService;
    private final AIAssistantService aiAssistantService;
    private final AIAdvancedService aiAdvancedService;
    private final AIMultimodalService aiMultimodalService;
    private final AISmartItineraryService aiSmartItineraryService;

    // ==================== 基础对话 ====================

    @PostMapping("/chat")
    @Operation(summary = "智能对话", description = "使用通义千问进行智能对话")
    public Result<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            String response = qwenService.chatCompletion(
                    request.getMessage(),
                    request.getSystemPrompt()
            );

            Map<String, Object> result = Map.of(
                    "response", response,
                    "source", "qwen"
            );

            return Result.success("对话成功", result);
        } catch (Exception e) {
            log.error("智能对话失败: {}", e.getMessage(), e);
            return Result.error("对话失败: " + e.getMessage());
        }
    }

    @PostMapping("/advanced/chat")
    @Operation(summary = "高级聊天机器人", description = "支持上下文的高级聊天")
    public Result<Map<String, Object>> advancedChatbot(@RequestBody AdvancedChatRequest request) {
        try {
            log.info("高级聊天: conversationId={}, message={}", request.getConversationId(), request.getMessage());

            String response = qwenService.chatCompletion(
                    request.getMessage(),
                    "你是一个专业的旅游顾问，擅长提供个性化的旅行建议。请保持友好、专业的态度。"
            );

            Map<String, Object> result = Map.of(
                    "response", response,
                    "conversationId", request.getConversationId() != null ? request.getConversationId() : "default",
                    "timestamp", System.currentTimeMillis(),
                    "source", "qwen"
            );

            return Result.success("聊天成功", result);
        } catch (Exception e) {
            log.error("高级聊天失败: {}", e.getMessage(), e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }

    @PostMapping("/assistant/chat")
    @Operation(summary = "智能客服", description = "智能旅游助手问答")
    public Result<Map<String, Object>> smartAssistant(@RequestBody AssistantQueryRequest request) {
        try {
            log.info("智能助手查询: query={}", request.getQuery());

            String contextInfo = request.getContext() != null ? request.getContext().toString() : "";
            String response = qwenService.customerServiceReply(request.getQuery(), contextInfo);

            Map<String, Object> result = Map.of(
                    "response", response,
                    "suggestions", extractSuggestions(response),
                    "source", "qwen"
            );

            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("智能助手查询失败: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // ==================== 推荐服务 ====================

    @PostMapping("/recommend")
    @Operation(summary = "AI智能推荐", description = "根据用户偏好推荐旅游路线")
    public Result<List<Map<String, Object>>> recommend(@RequestBody RecommendRequest request) {
        try {
            log.info("AI智能推荐请求: userId={}, location={}", request.getUserId(), request.getLocation());

            String userInput = buildUserInput(request);
            Integer cityId = request.getCityId();
            Integer days = request.getDuration() != null ? request.getDuration() : 3;
            Integer userId = request.getUserId();

            if (userInput == null || userInput.trim().isEmpty()) {
                return Result.error("用户输入不能为空");
            }

            List<Map<String, Object>> recommendations = qwenRecommendByAI(userInput, cityId, days, userId);
            return Result.success("推荐成功", recommendations);
        } catch (Exception e) {
            log.error("AI智能推荐失败: {}", e.getMessage(), e);
            return Result.error("推荐失败: " + e.getMessage());
        }
    }

    @GetMapping("/advanced/recommendations")
    @Operation(summary = "个性化推荐", description = "根据用户ID和类型获取个性化推荐")
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
            log.error("个性化推荐失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ==================== 行程生成 ====================

    @PostMapping("/itinerary/generate")
    @Operation(summary = "生成行程", description = "使用通义千问生成旅行行程")
    public Result<Map<String, Object>> generateItinerary(@RequestBody ItineraryGenerateRequest request) {
        try {
            log.info("生成行程请求: destination={}, days={}", request.getDestination(), request.getDays());

            String preferences = buildPreferencesString(request.getPreferences());
            String budget = request.getBudget() != null ? "¥" + request.getBudget() : "中等预算";

            String itineraryJson = qwenService.recommendItinerary(preferences, request.getDays(), budget);

            Map<String, Object> result = Map.of(
                    "destination", request.getDestination(),
                    "days", request.getDays(),
                    "itinerary", itineraryJson,
                    "source", "qwen"
            );

            return Result.success("行程生成成功", result);
        } catch (Exception e) {
            log.error("行程生成失败: {}", e.getMessage(), e);
            return Result.error("行程生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/smart-itinerary/generate")
    @Operation(summary = "生成智能行程", description = "基于AI的智能行程规划")
    public Result<Map<String, Object>> generateSmartItinerary(@RequestBody Map<String, Object> itineraryRequest) {
        try {
            log.info("生成智能行程请求: userId={}, cityId={}", itineraryRequest.get("userId"), itineraryRequest.get("cityId"));
            Integer userId = getIntegerValue(itineraryRequest, "userId");
            Integer cityId = getIntegerValue(itineraryRequest, "cityId");
            int days = getIntValue(itineraryRequest, "days", 3);
            double budget = getDoubleValue(itineraryRequest, "budget", 1000.0);
            Map<String, Object> userPreferences = parseMap(itineraryRequest.get("preferences"));

            Map<String, Object> itinerary = aiSmartItineraryService.generateItinerary(userPreferences, budget, days, cityId, userId);
            return Result.success("生成智能行程成功", itinerary);
        } catch (Exception e) {
            log.error("生成智能行程失败: {}", e.getMessage(), e);
            return Result.error("生成智能行程失败: " + e.getMessage());
        }
    }

    @PostMapping("/smart-itinerary/optimize")
    @Operation(summary = "优化行程", description = "优化已有行程")
    public Result<Map<String, Object>> optimizeItinerary(@RequestBody Map<String, Object> itineraryData) {
        try {
            log.info("优化行程请求: routeId={}", itineraryData.get("routeId"));
            Integer routeId = getIntegerValue(itineraryData, "routeId");
            Map<String, Object> userPreferences = parseMap(itineraryData.get("preferences"));

            Map<String, Object> optimized = aiSmartItineraryService.optimizeItinerary(routeId, userPreferences);
            return Result.success("优化行程成功", optimized);
        } catch (Exception e) {
            log.error("优化行程失败: {}", e.getMessage(), e);
            return Result.error("优化行程失败: " + e.getMessage());
        }
    }

    // ==================== 图像分析 ====================

    @GetMapping("/image-analysis/types")
    @Operation(summary = "获取图像分析类型", description = "返回可用的图像分析类型列表")
    public Result<List<Map<String, String>>> getImageAnalysisTypes() {
        List<Map<String, String>> types = List.of(
                Map.of("value", "scene", "label", "场景识别"),
                Map.of("value", "dish", "label", "菜品识别"),
                Map.of("value", "ocr", "label", "文字识别")
        );
        return Result.success("获取图像分析类型成功", types);
    }

    @PostMapping("/image-analysis")
    @Operation(summary = "图像分析", description = "使用百度AI进行图像识别和分析")
    public Result<Map<String, Object>> analyzeImage(@RequestBody ImageAnalysisRequest request) {
        try {
            log.info("图像分析请求: analysisType={}", request.getAnalysisType());

            byte[] imageData = downloadImageFromUrl(request.getImageUrl());
            if (imageData == null) {
                return Result.error("图片下载失败");
            }

            Map<String, Object> result;
            String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "scene";

            switch (analysisType.toLowerCase()) {
                case "dish":
                    result = baiduAIService.recognizeDish(imageData);
                    break;
                case "text":
                case "ocr":
                    result = baiduAIService.recognizeText(imageData);
                    break;
                default:
                    result = baiduAIService.recognizeScene(imageData);
            }

            return Result.success("图像分析成功", result);
        } catch (Exception e) {
            log.error("图像分析失败: {}", e.getMessage(), e);
            return Result.error("图像分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/image/analyze")
    @Operation(summary = "图像分析（统一入口）", description = "上传图片进行分析，type=analyze|recognize|tags|description")
    public Result<?> analyzeImageUpload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "analyze") String type) {
        try {
            log.info("图像分析请求: filename={}, type={}", file.getOriginalFilename(), type);
            Object result = switch (type) {
                case "recognize" -> aiImageAnalysisService.recognizeAttraction(file);
                case "tags" -> aiImageAnalysisService.analyzeImageTags(file);
                case "description" -> aiImageAnalysisService.getImageDescription(file);
                default -> aiImageAnalysisService.analyzeImage(file, null);
            };
            return Result.success("图像分析成功", result);
        } catch (Exception e) {
            log.error("图像分析失败: {}", e.getMessage(), e);
            return Result.error("图像分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/image/similar")
    @Operation(summary = "相似景点推荐", description = "根据图片推荐相似景点")
    public Result<List<Map<String, Object>>> getSimilarAttractions(@RequestParam("file") MultipartFile file,
                                                                   @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取相似景点推荐请求: filename={}, limit={}", file.getOriginalFilename(), limit);
            List<Map<String, Object>> attractions = aiImageAnalysisService.getSimilarAttractions(file, limit);
            return Result.success("获取相似景点成功", attractions);
        } catch (Exception e) {
            log.error("获取相似景点推荐失败: {}", e.getMessage(), e);
            return Result.error("获取相似景点失败: " + e.getMessage());
        }
    }

    // ==================== 智能助手 ====================

    @PostMapping("/assistant/ask")
    @Operation(summary = "智能问答", description = "AI智能问答")
    public Result<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            Integer userId = (Integer) request.get("userId");
            log.info("智能问答请求: userId={}, question={}", userId, question);
            Map<String, Object> answer = aiAssistantService.askQuestion(question, userId);
            return Result.success("智能问答成功", answer);
        } catch (Exception e) {
            log.error("智能问答失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/assistant/optimize/{routeId}")
    @Operation(summary = "行程优化建议", description = "AI给出行程优化建议")
    public Result<Map<String, Object>> optimizeRouteByAI(@PathVariable Integer routeId) {
        try {
            log.info("行程优化建议请求: routeId={}", routeId);
            Map<String, Object> optimization = aiAssistantService.optimizeRouteByAI(routeId);
            return Result.success("行程优化建议成功", optimization);
        } catch (Exception e) {
            log.error("行程优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/assistant/attraction-intro/{attractionId}")
    @Operation(summary = "景点智能介绍", description = "获取景点AI介绍")
    public Result<Map<String, Object>> getAttractionIntro(@PathVariable Integer attractionId) {
        try {
            log.info("获取景点智能介绍请求: attractionId={}", attractionId);
            Map<String, Object> intro = aiAssistantService.getAttractionIntro(attractionId);
            return Result.success("获取景点智能介绍成功", intro);
        } catch (Exception e) {
            log.error("获取景点智能介绍失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ==================== 多模态 ====================

    @PostMapping("/multimodal/query")
    @Operation(summary = "多模态查询", description = "支持文本、图像的多模态查询")
    public Result<Map<String, Object>> multimodalQuery(@RequestBody MultimodalQueryRequest request) {
        try {
            log.info("多模态查询请求: text={}, hasImage={}",
                    request.getText(),
                    request.getImage() != null);

            StringBuilder query = new StringBuilder();
            if (request.getText() != null && !request.getText().isEmpty()) {
                query.append(request.getText());
            }

            if (request.getImage() != null && !request.getImage().isEmpty()) {
                query.append(" [附带图片]");
            }

            String contextInfo = request.getContext() != null ? request.getContext().toString() : "";
            String response = qwenService.chatCompletion(query.toString(),
                    "你是一个多模态旅游助手，可以处理文本和图片信息。");

            Map<String, Object> result = Map.of(
                    "response", response,
                    "queryType", "multimodal",
                    "source", "qwen"
            );

            return Result.success("多模态查询成功", result);
        } catch (Exception e) {
            log.error("多模态查询失败: {}", e.getMessage(), e);
            return Result.error("多模态查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/multimodal/recommend")
    @Operation(summary = "多模态推荐", description = "基于文本、图像的推荐")
    public Result<List<Map<String, Object>>> getMultimodalRecommendations(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取多模态推荐请求");
            List<Map<String, Object>> recommendations = aiMultimodalService.getMultimodalRecommendations(text, image, limit);
            return Result.success("获取推荐成功", recommendations);
        } catch (Exception e) {
            log.error("获取多模态推荐失败: {}", e.getMessage(), e);
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }

    @PostMapping("/multimodal/search")
    @Operation(summary = "多模态搜索", description = "基于文本和图像的搜索")
    public Result<List<Map<String, Object>>> multimodalSearch(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("多模态搜索请求");
            List<Map<String, Object>> results = aiMultimodalService.multimodalSearch(text, image, page, size);
            return Result.success("搜索成功", results);
        } catch (Exception e) {
            log.error("多模态搜索失败: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    // ==================== 高级功能 ====================

    @PostMapping("/advanced/plan")
    @Operation(summary = "智能路线规划", description = "AI智能路线规划")
    public Result<Map<String, Object>> planRoute(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> preferences = parseMap(request.get("preferences"));
            Map<String, Object> constraints = parseMap(request.get("constraints"));
            log.info("智能路线规划请求");
            Map<String, Object> route = aiAdvancedService.planRoute(preferences, constraints);
            return Result.success("智能路线规划成功", route);
        } catch (Exception e) {
            log.error("智能路线规划失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/advanced/guide")
    @Operation(summary = "生成旅游攻略", description = "生成旅游攻略")
    public Result<Map<String, Object>> generateTravelGuide(@RequestBody Map<String, Object> request) {
        try {
            Integer cityId = (Integer) request.get("cityId");
            Integer days = (Integer) request.get("days");
            Map<String, Object> preferences = parseMap(request.get("preferences"));
            log.info("生成旅游攻略请求: cityId={}, days={}", cityId, days);
            Map<String, Object> guide = aiAdvancedService.generateTravelGuide(cityId, days, preferences);
            return Result.success("生成旅游攻略成功", guide);
        } catch (Exception e) {
            log.error("生成旅游攻略失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/advanced/safety/{cityId}")
    @Operation(summary = "旅游安全建议", description = "获取旅游安全建议")
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

    @PostMapping("/advanced/budget")
    @Operation(summary = "预算估算", description = "AI估算旅游预算")
    public Result<Map<String, Object>> estimateBudget(@RequestBody Map<String, Object> request) {
        try {
            Integer cityId = (Integer) request.get("cityId");
            Integer days = (Integer) request.get("days");
            Map<String, Object> preferences = parseMap(request.get("preferences"));
            log.info("旅游预算估算请求: cityId={}, days={}", cityId, days);
            Map<String, Object> budget = aiAdvancedService.estimateBudget(cityId, days, preferences);
            return Result.success("旅游预算估算成功", budget);
        } catch (Exception e) {
            log.error("旅游预算估算失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ==================== 问答 ====================

    @PostMapping("/qa")
    @Operation(summary = "旅行问答", description = "使用通义千问回答旅行相关问题")
    public Result<Map<String, Object>> travelQA(@RequestBody QARequest request) {
        try {
            String response = qwenService.travelQA(request.getQuestion());

            Map<String, Object> result = Map.of(
                    "question", request.getQuestion(),
                    "answer", response,
                    "source", "qwen"
            );

            return Result.success("问答成功", result);
        } catch (Exception e) {
            log.error("旅行问答失败: {}", e.getMessage(), e);
            return Result.error("问答失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    private String buildUserInput(RecommendRequest request) {
        StringBuilder input = new StringBuilder();

        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            input.append("目的地：").append(request.getLocation()).append("。");
        }

        if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
            input.append("偏好：").append(request.getPreferences().toString()).append("。");
        }

        if (request.getBudget() != null) {
            input.append("预算：¥").append(request.getBudget()).append("。");
        }

        if (request.getDuration() != null) {
            input.append("天数：").append(request.getDuration()).append("天。");
        }

        return input.toString();
    }

    private List<Map<String, Object>> qwenRecommendByAI(String userInput, Integer cityId, int days, Integer userId) {
        try {
            String prompt = "根据以下用户需求，推荐5个合适的旅游路线或景点，以JSON数组格式返回，每个包含id、name、description、matchScore字段：\n" + userInput;
            String aiResponse = qwenService.chatCompletion(prompt, "你是一个专业的旅游推荐助手");

            Map<String, Object> recommendation = Map.of(
                    "id", 1,
                    "name", "AI智能推荐路线",
                    "description", aiResponse,
                    "matchScore", 95,
                    "source", "qwen"
            );

            return List.of(recommendation);
        } catch (Exception e) {
            log.error("Qwen推荐失败，使用降级方案: error={}", e.getMessage());
            return fallbackRecommend(userInput);
        }
    }

    private List<Map<String, Object>> fallbackRecommend(String userInput) {
        Map<String, Object> route = Map.of(
                "id", 1,
                "name", "推荐路线",
                "description", "基于您的需求: " + userInput,
                "matchScore", 80,
                "source", "fallback"
        );
        return List.of(route);
    }

    private String buildPreferencesString(Map<String, Object> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "无特殊偏好";
        }
        return preferences.toString();
    }

    private List<String> extractSuggestions(String response) {
        return List.of("查看更多推荐", "调整行程细节", "保存此方案");
    }

    private byte[] downloadImageFromUrl(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.io.InputStream inputStream = url.openStream();
            byte[] imageData = inputStream.readAllBytes();
            inputStream.close();
            return imageData;
        } catch (Exception e) {
            log.error("下载图片失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(Object obj) {
        Map<String, Object> result = new java.util.HashMap<>();
        if (obj instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    // ==================== 请求DTO ====================

    @Data
    public static class ChatRequest {
        private String message;
        private String systemPrompt;
    }

    @Data
    public static class RecommendRequest {
        private Integer userId;
        private String location;
        private Map<String, Object> preferences;
        private Integer budget;
        private Integer duration;
        private Integer cityId;
    }

    @Data
    public static class ImageAnalysisRequest {
        private String imageUrl;
        private String analysisType;
    }

    @Data
    public static class ItineraryGenerateRequest {
        private String destination;
        private Integer days;
        private Map<String, Object> preferences;
        private Integer budget;
    }

    @Data
    public static class MultimodalQueryRequest {
        private String text;
        private String image;
        private Map<String, Object> context;
    }

    @Data
    public static class AssistantQueryRequest {
        private String query;
        private Map<String, Object> context;
    }

    @Data
    public static class AdvancedChatRequest {
        private String message;
        private String conversationId;
    }

    @Data
    public static class QARequest {
        private String question;
    }
}