package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.service.AIAssistantResponseSupport;
import travel.route.service.AIAssistantService;
import travel.route.service.AttractionService;
import travel.route.service.QwenService;
import travel.route.service.RouteService;

@Service
@Primary
@RequiredArgsConstructor
public class RealQwenAssistantServiceImpl implements AIAssistantService {

    private static final Logger log = LoggerFactory.getLogger(RealQwenAssistantServiceImpl.class);
    private static final String SOURCE_QWEN = "qwen";

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final QwenService qwenService;

    @Override
    public AIAskQuestionResponse askQuestion(String question, Integer userId) {
        try {
            String answer = qwenService.travelQA(question);
            return AIAssistantResponseSupport.toAskQuestionResponse(
                    AIAssistantResponseSupport.buildAskQuestionPayload(question, answer, SOURCE_QWEN));
        } catch (Exception e) {
            log.error("Qwen question failed: question={}", question, e);
            throw dependencyFailure(e);
        }
    }

    @Override
    public AIOptimizeRouteResponse optimizeRouteByAI(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        try {
            String prompt = String.join("\n",
                    "请用中文优化以下旅行路线。",
                    "路线标题：" + defaultIfBlank(route.getTitle(), "未提供"),
                    "城市：" + routeCityName(route),
                    "请给出具体、可执行的调整建议。"
            );
            String suggestion = qwenService.chatCompletion(
                    prompt,
                    "你是一名专业旅行规划师，请只基于已提供的路线信息给出具体、可执行的中文建议。"
            );
            return AIAssistantResponseSupport.buildOptimizationSuccess(routeId, suggestion, SOURCE_QWEN);
        } catch (Exception e) {
            log.error("Qwen route optimization failed: routeId={}", routeId, e);
            throw dependencyFailure(e);
        }
    }

    @Override
    public AIAttractionIntroResponse getAttractionIntro(Integer attractionId) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }

        try {
            String intro = qwenService.generateAttractionIntro(
                    defaultIfBlank(attraction.getName(), "未提供"),
                    defaultIfBlank(attraction.getAddress(), attractionCityName(attraction))
            );
            return AIAssistantResponseSupport.toAttractionIntroResponse(
                    AIAssistantResponseSupport.buildAttractionIntroPayload(
                            attractionId, attraction.getName(), intro, SOURCE_QWEN));
        } catch (Exception e) {
            log.error("Qwen attraction intro failed: attractionId={}", attractionId, e);
            throw dependencyFailure(e);
        }
    }

    private RuntimeException dependencyFailure(Exception exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException;
        }
        return new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR);
    }

    private String routeCityName(Route route) {
        if (route.getCity() != null && route.getCity().getName() != null
                && !route.getCity().getName().isBlank()) {
            return route.getCity().getName();
        }
        return "未提供";
    }

    private String attractionCityName(Attraction attraction) {
        if (attraction.getCity() != null && attraction.getCity().getName() != null
                && !attraction.getCity().getName().isBlank()) {
            return attraction.getCity().getName();
        }
        return "未提供";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
