package travel.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import travel.route.service.*;
import travel.route.dto.ai.*;
import travel.common.utils.Result;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI智能助手", description = "AI助手问答、客服、景点介绍、行程优化等接口")
@RequiredArgsConstructor
public class AIAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantController.class);

    private final QwenService qwenService;
    private final AIAssistantService aiAssistantService;

    @PostMapping("/assistant/chat")
    @Operation(summary = "智能客服", description = "智能旅游助手问答")
    public Result<AIAssistantChatResponse> smartAssistant(@Valid @RequestBody AIAssistantChatRequest request) {
        try {
            log.info("智能助手查询: query={}", request.getQuery());

            String contextInfo = request.getContext() != null ? request.getContext().toString() : "";
            String response = qwenService.customerServiceReply(request.getQuery(), contextInfo);

            AIAssistantChatResponse result = AIAssistantChatResponse.builder()
                    .response(response)
                    .suggestions(extractSuggestions(response))
                    .source("qwen")
                    .build();

            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("智能助手查询失败: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/assistant/ask")
    @Operation(summary = "智能问答", description = "AI助手智能问答")
    public Result<AIAskQuestionResponse> askQuestion(
            @Valid @RequestBody AIAskQuestionRequest request) {
        try {
            log.info("智能问答请求: question={}, userId={}", request.getQuestion(), request.getUserId());
            AIAskQuestionResponse response = aiAssistantService.askQuestion(
                    request.getQuestion(), request.getUserId());
            return Result.success("问答成功", response);
        } catch (Exception e) {
            log.error("智能问答失败: {}", e.getMessage(), e);
            return Result.error("问答失败: " + e.getMessage());
        }
    }

    @RequestMapping(
            value = {"/assistant/optimize/{routeId}", "/assistant/optimize-route/{routeId}"},
            method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "行程优化建议", description = "获取AI行程优化建议")
    public Result<AIOptimizeRouteResponse> optimizeRouteByAI(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            log.info("行程优化建议请求: routeId={}", routeId);
            AIOptimizeRouteResponse optimization = aiAssistantService.optimizeRouteByAI(routeId);
            return Result.success("行程优化建议成功", optimization);
        } catch (Exception e) {
            log.error("行程优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/assistant/attraction-intro/{attractionId}")
    @Operation(summary = "景点智能介绍", description = "获取景点AI介绍")
    public Result<AIAttractionIntroResponse> getAttractionIntro(@PathVariable Integer attractionId) {
        try {
            log.info("获取景点智能介绍请求: attractionId={}", attractionId);
            AIAttractionIntroResponse intro = aiAssistantService.getAttractionIntro(attractionId);
            return Result.success("获取景点智能介绍成功", intro);
        } catch (Exception e) {
            log.error("获取景点智能介绍失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    private java.util.List<String> extractSuggestions(String response) {
        return java.util.List.of("查看更多推荐", "调整行程细节", "保存此方案");
    }
}
