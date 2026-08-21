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
import travel.common.security.AuthenticatedUserSupport;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI智能助手", description = "AI助手问答、客服、景点介绍、行程优化等接口")
@RequiredArgsConstructor
public class AIAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantController.class);

    private final QwenService qwenService;
    private final AIAssistantService aiAssistantService;
    private final RouteService routeService;

    @PostMapping("/assistant/chat")
    @Operation(summary = "智能客服", description = "智能旅游助手问答")
    public Result<AIAssistantChatResponse> smartAssistant(@Valid @RequestBody AIAssistantChatRequest request) {
        log.info("智能助手查询请求");
        String contextInfo = request.getContext() != null ? request.getContext().toString() : "";
        String response = qwenService.customerServiceReply(request.getQuery(), contextInfo);
        AIAssistantChatResponse result = AIAssistantChatResponse.builder()
                .response(response)
                .suggestions(null)
                .source("qwen")
                .build();
        return Result.success("查询成功", result);
    }

    @PostMapping("/assistant/ask")
    @Operation(summary = "智能问答", description = "AI助手智能问答")
    public Result<AIAskQuestionResponse> askQuestion(
            @Valid @RequestBody AIAskQuestionRequest request) {
        log.info("智能问答请求: userId={}", request.getUserId());
        Integer userId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        AIAskQuestionResponse response = aiAssistantService.askQuestion(request.getQuestion(), userId);
        return Result.success("问答成功", response);
    }

    @RequestMapping(
            value = {"/assistant/optimize/{routeId}", "/assistant/optimize-route/{routeId}"},
            method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "行程优化建议", description = "获取AI行程优化建议")
    public Result<AIOptimizeRouteResponse> optimizeRouteByAI(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        routeService.checkRouteOwner(routeId.longValue(), AuthenticatedUserSupport.requireUserId());
        log.info("行程优化建议请求: routeId={}", routeId);
        AIOptimizeRouteResponse optimization = aiAssistantService.optimizeRouteByAI(routeId);
        return Result.success("行程优化建议成功", optimization);
    }

    @GetMapping("/assistant/attraction-intro/{attractionId}")
    @Operation(summary = "景点智能介绍", description = "获取景点AI介绍")
    public Result<AIAttractionIntroResponse> getAttractionIntro(@PathVariable Integer attractionId) {
        log.info("获取景点智能介绍请求: attractionId={}", attractionId);
        AIAttractionIntroResponse intro = aiAssistantService.getAttractionIntro(attractionId);
        return Result.success("获取景点智能介绍成功", intro);
    }
}
