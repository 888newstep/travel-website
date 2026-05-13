package travel.controller.travel_recommendation_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import travel.service.travel_recommendation.BaiduAIService;
import travel.service.travel_recommendation.QwenService;
import travel.utils.Result;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
@Tag(name = "AI智能服务", description = "通义千问、百度AI和高德地图相关接口")
@RequiredArgsConstructor
public class AIController {

    private final QwenService qwenService;
    private final BaiduAIService baiduAIService;

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

    @PostMapping("/multimodal/query")
    @Operation(summary = "多模态查询", description = "支持文本、图像、音频的多模态查询")
    public Result<Map<String, Object>> multimodalQuery(@RequestBody MultimodalQueryRequest request) {
        try {
            log.info("多模态查询请求: text={}, hasImage={}, hasAudio={}",
                    request.getText(),
                    request.getImage() != null,
                    request.getAudio() != null);

            StringBuilder query = new StringBuilder();
            if (request.getText() != null && !request.getText().isEmpty()) {
                query.append(request.getText());
            }

            if (request.getImage() != null && !request.getImage().isEmpty()) {
                query.append(" [附带图片]");
            }

            if (request.getAudio() != null && !request.getAudio().isEmpty()) {
                query.append(" [附带音频]");
            }

            String contextInfo = request.getContext() != null ? request.getContext().toString() : "";
            String response = qwenService.chatCompletion(query.toString(),
                    "你是一个多模态旅游助手，可以处理文本、图片和语音信息。");

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

    @PostMapping("/assistant/query")
    @Operation(summary = "智能助手查询", description = "智能旅游助手问答")
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

    @PostMapping("/advanced/chatbot")
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

    @PostMapping("/recognize-scene")
    @Operation(summary = "场景识别", description = "使用百度AI识别图片场景")
    public Result<Map<String, Object>> recognizeScene(@RequestParam("image") MultipartFile image) {
        try {
            byte[] imageData = image.getBytes();
            Map<String, Object> result = baiduAIService.recognizeScene(imageData);
            return Result.success("场景识别成功", result);
        } catch (Exception e) {
            log.error("场景识别失败: {}", e.getMessage(), e);
            return Result.error("场景识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/recognize-dish")
    @Operation(summary = "菜品识别", description = "使用百度AI识别菜品")
    public Result<Map<String, Object>> recognizeDish(@RequestParam("image") MultipartFile image) {
        try {
            byte[] imageData = image.getBytes();
            Map<String, Object> result = baiduAIService.recognizeDish(imageData);
            return Result.success("菜品识别成功", result);
        } catch (Exception e) {
            log.error("菜品识别失败: {}", e.getMessage(), e);
            return Result.error("菜品识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/ocr")
    @Operation(summary = "文字识别", description = "使用百度OCR识别图片文字")
    public Result<Map<String, Object>> recognizeText(@RequestParam("image") MultipartFile image) {
        try {
            byte[] imageData = image.getBytes();
            Map<String, Object> result = baiduAIService.recognizeText(imageData);
            return Result.success("文字识别成功", result);
        } catch (Exception e) {
            log.error("文字识别失败: {}", e.getMessage(), e);
            return Result.error("文字识别失败: " + e.getMessage());
        }
    }

    @PostMapping("/travel-qa")
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
        List<String> suggestions = List.of(
                "查看更多推荐",
                "调整行程细节",
                "保存此方案"
        );
        return suggestions;
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
        private String audio;
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
