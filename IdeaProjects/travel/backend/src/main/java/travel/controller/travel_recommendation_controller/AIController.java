package travel.controller.travel_recommendation_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import travel.service.travel_recommendation.BaiduAIService;
import travel.service.travel_recommendation.OpenAIService;
import travel.utils.Result;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
@Tag(name = "AI智能服务", description = "OpenAI和百度AI相关接口")
@RequiredArgsConstructor
public class AIController {

    private final OpenAIService openAIService;
    private final BaiduAIService baiduAIService;

    @PostMapping("/chat")
    @Operation(summary = "智能对话")
    public Result<String> chat(@RequestBody ChatRequest request) {
        try {
            String response = openAIService.chatCompletion(
                    request.getMessage(),
                    request.getSystemPrompt()
            );
            return Result.success("对话成功", response);
        } catch (Exception e) {
            log.error("智能对话失败: {}", e.getMessage(), e);
            return Result.error("对话失败: " + e.getMessage());
        }
    }

    @PostMapping("/recommend-itinerary")
    @Operation(summary = "行程推荐")
    public Result<String> recommendItinerary(@RequestBody ItineraryRequest request) {
        try {
            String response = openAIService.recommendItinerary(
                    request.getPreferences(),
                    request.getDays(),
                    request.getBudget()
            );
            return Result.success("行程推荐成功", response);
        } catch (Exception e) {
            log.error("行程推荐失败: {}", e.getMessage(), e);
            return Result.error("行程推荐失败: " + e.getMessage());
        }
    }

    @PostMapping("/recognize-scene")
    @Operation(summary = "场景识别")
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
    @Operation(summary = "菜品识别")
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
    @Operation(summary = "文字识别")
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
    @Operation(summary = "旅行问答")
    public Result<String> travelQA(@RequestBody QARequest request) {
        try {
            String response = openAIService.travelQA(request.getQuestion());
            return Result.success("问答成功", response);
        } catch (Exception e) {
            log.error("旅行问答失败: {}", e.getMessage(), e);
            return Result.error("问答失败: " + e.getMessage());
        }
    }

    // 请求DTO
    @lombok.Data
    public static class ChatRequest {
        private String message;
        private String systemPrompt;
    }

    @lombok.Data
    public static class ItineraryRequest {
        private String preferences;
        private int days;
        private String budget;
    }

    @lombok.Data
    public static class QARequest {
        private String question;
    }
}
