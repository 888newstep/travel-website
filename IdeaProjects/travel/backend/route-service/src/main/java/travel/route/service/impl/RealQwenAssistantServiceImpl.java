package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.service.AIAssistantResponseSupport;
import travel.route.service.AIAssistantService;
import travel.route.service.AttractionService;
import travel.route.service.QwenService;
import travel.route.service.RouteService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Primary
@RequiredArgsConstructor
public class RealQwenAssistantServiceImpl implements AIAssistantService {

    private static final Logger log = LoggerFactory.getLogger(RealQwenAssistantServiceImpl.class);
    private static final String AI_PREFIX = "ai:";
    private static final String SOURCE_QWEN = "qwen";

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final CacheUtil cacheUtil;
    private final QwenService qwenService;

    @Override
    public AIAskQuestionResponse askQuestion(String question, Integer userId) {
        String cacheKey = AI_PREFIX + "qa:" + question.hashCode();

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = cacheUtil.get(cacheKey, Map.class);
        if (cached != null) {
            log.info("Return cached Qwen answer: question={}", question);
            return AIAssistantResponseSupport.toAskQuestionResponse(cached);
        }

        try {
            String answer = qwenService.travelQA(question);
            Map<String, Object> result = AIAssistantResponseSupport.buildAskQuestionPayload(question, answer, 0.95, SOURCE_QWEN);
            cacheUtil.set(cacheKey, result, 60, TimeUnit.MINUTES);
            log.info("Qwen question answered: question={}", question);
            return AIAssistantResponseSupport.toAskQuestionResponse(result);
        } catch (Exception e) {
            log.error("Qwen question failed, fallback applied: question={}", question, e);
            return AIAssistantResponseSupport.toAskQuestionResponse(
                    AIAssistantResponseSupport.buildFallbackAskQuestionPayload(question)
            );
        }
    }

    @Override
    public AIOptimizeRouteResponse optimizeRouteByAI(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            return AIAssistantResponseSupport.buildRouteNotFound(routeId);
        }

        try {
            String prompt = String.join("\n",
                    "Please optimize the following travel route and reply in Chinese.",
                    "Route title: " + defaultIfBlank(route.getTitle(), "N/A"),
                    "City: " + routeCityName(route),
                    "Give concrete and practical suggestions."
            );
            String suggestion = qwenService.chatCompletion(
                    prompt,
                    "You are a professional travel planner. Reply in Chinese with concrete and actionable optimization suggestions."
            );
            return AIAssistantResponseSupport.buildOptimizationSuccess(routeId, suggestion, 88, SOURCE_QWEN);
        } catch (Exception e) {
            log.error("Qwen route optimization failed, fallback applied: routeId={}", routeId, e);
            return AIAssistantResponseSupport.buildOptimizationFallback(routeId, e.getMessage());
        }
    }

    @Override
    public AIAttractionIntroResponse getAttractionIntro(Integer attractionId) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            return AIAssistantResponseSupport.buildAttractionNotFound(attractionId);
        }

        String cacheKey = AI_PREFIX + "intro:" + attractionId;
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = cacheUtil.get(cacheKey, Map.class);
        if (cached != null) {
            return AIAssistantResponseSupport.toAttractionIntroResponse(cached);
        }

        try {
            String intro = qwenService.generateAttractionIntro(
                    defaultIfBlank(attraction.getName(), "N/A"),
                    defaultIfBlank(attraction.getAddress(), routeCityName(attraction))
            );
            Map<String, Object> result = AIAssistantResponseSupport.buildAttractionIntroPayload(
                    attractionId,
                    attraction.getName(),
                    intro,
                    "\u6682\u65e0\u8da3\u95fb",
                    "\u5efa\u8bae\u4e0a\u5348 9:00-11:00 \u6216\u4e0b\u5348 14:00-17:00 \u524d\u5f80",
                    "2-3\u5c0f\u65f6",
                    SOURCE_QWEN
            );
            cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);
            return AIAssistantResponseSupport.toAttractionIntroResponse(result);
        } catch (Exception e) {
            log.error("Qwen attraction intro failed, fallback applied: attractionId={}", attractionId, e);
            return AIAssistantResponseSupport.toAttractionIntroResponse(
                    AIAssistantResponseSupport.buildAttractionIntroFallbackPayload(attraction, attractionId, e.getMessage())
            );
        }
    }

    private String routeCityName(Route route) {
        if (route.getCity() != null && route.getCity().getName() != null && !route.getCity().getName().isBlank()) {
            return route.getCity().getName();
        }
        return "unknown";
    }

    private String routeCityName(Attraction attraction) {
        if (attraction.getCity() != null && attraction.getCity().getName() != null && !attraction.getCity().getName().isBlank()) {
            return attraction.getCity().getName();
        }
        return "unknown";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}