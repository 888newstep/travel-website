package travel.route.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travel.common.utils.Result;
import travel.route.dto.ai.AISmartItineraryGenerateRequest;
import travel.route.dto.ai.AISmartItineraryOptimizeRequest;
import travel.route.dto.ai.AISmartItineraryOptimizeResponse;
import travel.route.dto.ai.AISmartItineraryResponse;
import travel.route.service.AISmartItineraryService;


@RestController
@RequestMapping("/ai/smart-itinerary")
@RequiredArgsConstructor
public class AISmartItineraryController {

    private static final Logger log = LoggerFactory.getLogger(AISmartItineraryController.class);

    private final AISmartItineraryService aiSmartItineraryService;

    @PostMapping("/generate")
    public Result<AISmartItineraryResponse> generateSmartItinerary(@Valid @RequestBody AISmartItineraryGenerateRequest itineraryRequest) {
        try {
            log.info("生成智能行程请求: userId={}, cityId={}", itineraryRequest.getUserId(), itineraryRequest.getCityId());
            Integer userId = itineraryRequest.getUserId();
            Integer cityId = itineraryRequest.getCityId();
            int days = itineraryRequest.getDays() != null ? itineraryRequest.getDays() : 3;
            double budget = itineraryRequest.getBudget() != null ? itineraryRequest.getBudget() : 1000.0;

            AISmartItineraryResponse response = aiSmartItineraryService.generateItinerary(
                    itineraryRequest.getPreferences(), budget, days, cityId, userId);
            return Result.success("生成智能行程成功", response);
        } catch (Exception e) {
            log.error("生成智能行程失败: {}", e.getMessage(), e);
            return Result.error("生成智能行程失败: " + e.getMessage());
        }
    }

    @PostMapping("/optimize")
    public Result<AISmartItineraryOptimizeResponse> optimizeItinerary(@Valid @RequestBody AISmartItineraryOptimizeRequest itineraryData) {
        try {
            log.info("优化行程请求: routeId={}", itineraryData.getRouteId());
            Integer routeId = itineraryData.getRouteId();

            AISmartItineraryOptimizeResponse response = aiSmartItineraryService.optimizeItinerary(
                    routeId, itineraryData.getPreferences());
            return Result.success("优化行程成功", response);
        } catch (Exception e) {
            log.error("优化行程失败: {}", e.getMessage(), e);
            return Result.error("优化行程失败: " + e.getMessage());
        }
    }
}
