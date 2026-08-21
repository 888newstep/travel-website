package travel.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import travel.route.service.*;
import travel.route.dto.ai.*;
import travel.common.utils.Result;
import travel.common.security.AuthenticatedUserSupport;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI\u57fa\u7840\u670d\u52a1", description = "AI\u5bf9\u8bdd\u3001\u63a8\u8350\u3001\u884c\u7a0b\u751f\u6210\u3001\u95ee\u7b54\u7b49\u57fa\u7840\u63a5\u53e3")
@RequiredArgsConstructor
public class AIController {

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    private final QwenService qwenService;

    // ==================== \u57fa\u7840\u5bf9\u8bdd ====================

    @PostMapping("/chat")
    @Operation(summary = "\u667a\u80fd\u5bf9\u8bdd", description = "\u4f7f\u7528\u901a\u7528\u5343\u95ee\u8fdb\u884c\u667a\u80fd\u5bf9\u8bdd")
    public Result<AIChatResponse> chat(@Valid @RequestBody AIChatRequest request) {
        String response = qwenService.chatCompletion(request.getMessage(), request.getSystemPrompt());
        AIChatResponse result = new AIChatResponse(response, "qwen", null, null);
        return Result.success("\u5bf9\u8bdd\u6210\u529f", result);
    }

    @PostMapping("/qa")
    @Operation(summary = "\u65c5\u884c\u95ee\u7b54", description = "\u4f7f\u7528\u901a\u7528\u5343\u95ee\u56de\u7b54\u65c5\u884c\u76f8\u5173\u95ee\u9898")
    public Result<AIChatResponse> travelQA(@Valid @RequestBody AIQARequest request) {
        String response = qwenService.travelQA(request.getQuestion());
        AIChatResponse chatResponse = new AIChatResponse(response, "qwen", null, null);
        return Result.success("\u95ee\u7b54\u6210\u529f", chatResponse);
    }

    // ==================== \u63a8\u8350\u670d\u52a1 ====================

    @PostMapping("/recommend")
    @Operation(summary = "AI\u667a\u80fd\u63a8\u8350", description = "\u6839\u636e\u7528\u6237\u504f\u597d\u63a8\u8350\u65c5\u6e38\u8def\u7ebf")
    public Result<List<AIRecommendationItem>> recommend(@Valid @RequestBody AIRecommendRequest request) {
        log.info("AI\u667a\u80fd\u63a8\u8350\u8bf7\u6c42: userId={}, location={}", request.getUserId(), request.getLocation());
        String userInput = buildUserInput(request);
        Integer cityId = request.getCityId();
        Integer days = request.getDuration() != null ? request.getDuration() : 3;
        Integer userId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        List<AIRecommendationItem> recommendations = qwenRecommendByAI(userInput, cityId, days, userId);
        return Result.success("\u63a8\u8350\u6210\u529f", recommendations);
    }

    // ==================== \u884c\u7a0b\u751f\u6210 ====================

    @PostMapping("/itinerary/generate")
    @Operation(summary = "\u751f\u6210\u884c\u7a0b", description = "\u4f7f\u7528\u901a\u7528\u5343\u95ee\u751f\u6210\u65c5\u884c\u884c\u7a0b")
    public Result<AIItineraryResponseV2> generateItinerary(@Valid @RequestBody AIItineraryGenerateRequest request) {
        log.info("\u751f\u6210\u884c\u7a0b\u8bf7\u6c42: destination={}, days={}", request.getDestination(), request.getDays());
        String preferences = buildPreferencesString(request.getPreferences());
        String itineraryJson = qwenService.recommendItinerary(
                request.getDestination(), preferences, request.getDays(),
                buildBudgetDescription(request.getBudget()));
        AIItineraryResponseV2 result = AIItineraryResponseV2.builder()
                .destination(request.getDestination())
                .days(request.getDays())
                .itinerary(itineraryJson)
                .source("qwen")
                .build();
        return Result.success("\u751f\u6210\u884c\u7a0b\u6210\u529f", result);
    }

    // ==================== \u8f85\u52a9\u65b9\u6cd5 ====================

    private String buildUserInput(AIRecommendRequest request) {
        StringBuilder input = new StringBuilder();
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            input.append("\u76ee\u7684\u5730\uff1a").append(request.getLocation()).append("\u3002");
        }
        if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
            input.append("\u504f\u597d\uff1a").append(request.getPreferences().toString()).append("\u3002");
        }
        if (request.getBudget() != null) {
            input.append("\u9884\u7b97\uff1a\u00a5").append(request.getBudget()).append("\u3002");
        }
        if (request.getDuration() != null) {
            input.append("\u5929\u6570\uff1a").append(request.getDuration()).append("\u5929\u3002");
        }
        return input.toString();
    }

    private List<AIRecommendationItem> qwenRecommendByAI(String userInput, Integer cityId, int days, Integer userId) {
        String prompt = "\u6839\u636e\u4ee5\u4e0b\u7528\u6237\u9700\u6c42\uff0c\u63a8\u83505\u4e2a\u5408\u9002\u7684\u65c5\u6e38\u8def\u7ebf\u6216\u666f\u70b9\uff0c\u4ee5JSON\u6570\u7ec4\u683c\u5f0f\u8fd4\u56de\uff0c\u6bcf\u4e2a\u5305\u542bid\u3001name\u3001description\u3001matchScore\u5b57\u6bb5\uff1a\n" + userInput;
        String aiResponse = qwenService.chatCompletion(prompt, "\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u65c5\u6e38\u63a8\u8350\u52a9\u624b");
        AIRecommendationItem route = AIRecommendationItem.builder()
                .id(1)
                .name("AI\u667a\u80fd\u63a8\u8350\u8def\u7ebf")
                .description(aiResponse)
                .matchScore(null)
                .source("qwen")
                .build();
        return List.of(route);
    }

    private String buildBudgetDescription(Integer budget) {
        if (budget == null) {
            return "\u4e2d\u7b49\u9884\u7b97";
        }
        return "\u603b\u9884\u7b97\u4e0d\u8d85\u8fc7" + budget + "\u5143";
    }

    private String buildPreferencesString(Map<String, JsonNode> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "\u65e0\u7279\u6b8a\u504f\u597d";
        }
        return preferences.toString();
    }
}
