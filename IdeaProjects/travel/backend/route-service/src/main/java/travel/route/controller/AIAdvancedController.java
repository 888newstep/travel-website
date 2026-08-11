package travel.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import travel.common.utils.Result;
import travel.route.dto.ai.AIChatResponse;
import travel.route.dto.ai.AIAdvancedChatRequest;
import travel.route.dto.ai.AIBudgetDetails;
import travel.route.dto.ai.AIBudgetResponse;
import travel.route.dto.ai.AIPersonalizedRecommendationItem;
import travel.route.dto.ai.AIEstimateBudgetRequest;
import travel.route.dto.ai.AIGenerateTravelGuideRequest;
import travel.route.dto.ai.AIPlanRouteRequest;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AISafetyAdviceResponse;
import travel.route.dto.ai.AITravelGuideContent;
import travel.route.dto.ai.AITravelGuideResponse;
import travel.route.service.AIAdvancedService;
import travel.route.service.QwenService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/advanced")
@RequiredArgsConstructor
public class AIAdvancedController {

    private static final Logger log = LoggerFactory.getLogger(AIAdvancedController.class);
    private final AIAdvancedService aiAdvancedService;
    private final QwenService qwenService;

    @PostMapping("/plan")
    @Operation(summary = "鏅鸿兘璺嚎瑙勫垝", description = "AI鏅鸿兘璺嚎瑙勫垝")
    public Result<AIPlanRouteResponse> planRoute(@Valid @RequestBody AIPlanRouteRequest request) {
        try {
            log.info("鏅鸿兘璺嚎瑙勫垝璇锋眰");
            AIPlanRouteResponse route = aiAdvancedService.planRoute(
                    request.getPreferences(), request.getConstraints());
            return Result.success("鏅鸿兘璺嚎瑙勫垝鎴愬姛", route);
        } catch (IllegalArgumentException e) {
            log.warn("AI route plan constraint rejected: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("鏅鸿兘璺嚎瑙勫垝澶辫触: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/guide")
    @Operation(summary = "鐢熸垚鏃呮父鏀荤暐", description = "鐢熸垚鏃呮父鏀荤暐")
    public Result<AITravelGuideResponse> generateTravelGuide(@Valid @RequestBody AIGenerateTravelGuideRequest request) {
        try {
            Integer cityId = request.getCityId();
            Integer days = request.getDays();
            Map<String, JsonNode> preferences = request.getPreferences() == null ? Map.of() : request.getPreferences();
            log.info("鐢熸垚鏃呮父鏀荤暐璇锋眰: cityId={}, days={}", cityId, days);
            AITravelGuideContent guide = aiAdvancedService.generateTravelGuide(cityId, days, preferences);
            Integer normalizedDays = guide.getDays();
            AITravelGuideResponse response = AITravelGuideResponse.builder()
                    .cityId(cityId)
                    .days(normalizedDays)
                    .guide(guide)
                    .source("ai-advanced")
                    .build();
            return Result.success("鐢熸垚鏃呮父鏀荤暐鎴愬姛", response);
        } catch (Exception e) {
            log.error("鐢熸垚鏃呮父鏀荤暐澶辫触: {}", e.getMessage(), e);
            return Result.error("鐢熸垚鏃呮父鏀荤暐澶辫触: " + e.getMessage());
        }
    }

    @GetMapping("/safety/{cityId}")
    @Operation(summary = "鏃呮父瀹夊叏寤鸿", description = "鑾峰彇鏃呮父瀹夊叏寤鸿")
    public Result<AISafetyAdviceResponse> getSafetyAdvice(@PathVariable Integer cityId) {
        try {
            log.info("鑾峰彇鏃呮父瀹夊叏寤鸿璇锋眰: cityId={}", cityId);
            AISafetyAdviceResponse advice = aiAdvancedService.getSafetyAdvice(cityId);
            return Result.success("鑾峰彇鏃呮父瀹夊叏寤鸿鎴愬姛", advice);
        } catch (Exception e) {
            log.error("鑾峰彇鏃呮父瀹夊叏寤鸿澶辫触: cityId={}, error={}", cityId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/budget")
    @Operation(summary = "棰勭畻浼扮畻", description = "AI浼扮畻鏃呮父棰勭畻")
    public Result<AIBudgetResponse> estimateBudget(@Valid @RequestBody AIEstimateBudgetRequest request) {
        try {
            String destination = request.getDestination() != null ? request.getDestination() : "鐑棬鍩庡競";
            Integer days = request.getDays() != null ? request.getDays() : 1;
            Integer people = request.getPeople() != null ? request.getPeople() : 1;
            Integer budget = request.getBudget() != null ? request.getBudget() : 5000;
            String style = request.getStyle() != null ? request.getStyle() : "鑸掗€傚瀷";
            log.info("鏃呮父棰勭畻浼扮畻璇锋眰: destination={}, days={}, people={}", destination, days, people);
            AIBudgetDetails budgetDetails = aiAdvancedService.estimateBudget(null, days, null);
            AIBudgetResponse response = AIBudgetResponse.builder()
                    .destination(destination)
                    .days(budgetDetails.getDays())
                    .people(people)
                    .totalBudget(String.valueOf(budget))
                    .advice(style)
                    .source("ai")
                    .details(budgetDetails)
                    .build();
            return Result.success("鏃呮父棰勭畻浼扮畻鎴愬姛", response);
        } catch (Exception e) {
            log.error("鏃呮父棰勭畻浼扮畻澶辫触: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/recommendations")
    @Operation(summary = "个性化推荐", description = "根据用户ID和类型获取个性化推荐")
    public Result<List<AIPersonalizedRecommendationItem>> getPersonalizedRecommendations(
            @RequestParam Integer userId,
            @RequestParam String recommendationType,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("个性化推荐请求: userId={}, type={}, limit={}", userId, recommendationType, limit);
            List<AIPersonalizedRecommendationItem> recommendations = aiAdvancedService.getPersonalizedRecommendations(
                    userId, recommendationType, limit);
            return Result.success("个性化推荐成功", recommendations);
        } catch (Exception e) {
            log.error("个性化推荐失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "高级聊天机器人", description = "支持上下文的高级聊天")
    public Result<AIChatResponse> advancedChatbot(@Valid @RequestBody AIAdvancedChatRequest request) {
        try {
            log.info("高级聊天: conversationId={}, message={}", request.getConversationId(), request.getMessage());

            String response = qwenService.chatCompletion(
                    request.getMessage(),
                    "你是一个专业的旅游顾问，擅长提供个性化的旅行建议。请保持友好、专业的态度。"
            );

            String conversationId = request.getConversationId() != null ? request.getConversationId() : "default";
            AIChatResponse result = new AIChatResponse(response, "qwen", conversationId, System.currentTimeMillis());

            return Result.success("聊天成功", result);
        } catch (Exception e) {
            log.error("高级聊天失败: {}", e.getMessage(), e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }

}
