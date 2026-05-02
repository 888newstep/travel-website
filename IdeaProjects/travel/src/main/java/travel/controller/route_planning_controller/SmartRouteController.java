package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Route;
import travel.service.route_planning.SmartRouteService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能路线控制器
 * 处理智能路线生成、推荐和优化
 */
@RestController
@RequestMapping("/smart-route")
@RequiredArgsConstructor
public class SmartRouteController {

    private static final Logger log = LoggerFactory.getLogger(SmartRouteController.class);

    private final SmartRouteService smartRouteService;

    /**
     * 获取智能路线推荐
     * GET /api/smart-route/recommend
     */
    @GetMapping("/recommend")
    public Result<List<Route>> getSmartRouteRecommendations(@RequestParam Integer cityId,
                                                                           @RequestParam int days) {
        log.info("获取智能路线推荐请求: cityId={}, days={}", cityId, days);
        List<Route> recommendations = smartRouteService.getSmartRouteRecommendations(cityId, days);
        return Result.success("获取推荐成功", recommendations);
    }

    /**
     * 优化路线
     * POST /api/smart-route/optimize
     */
    @PostMapping("/optimize")
    public Result<String> optimizeRoute(@RequestParam Integer routeId) {
        log.info("优化路线请求: routeId={}", routeId);
        String result = smartRouteService.optimizeRoute(routeId);
        return Result.success("优化路线成功", result);
    }

    /**
     * 预测路线完成时间
     * POST /api/smart-route/predict-completion
     */
    @PostMapping("/predict-completion")
    public Result<String> predictRouteCompletion(@RequestParam Integer routeId) {
        log.info("预测路线完成时间请求: routeId={}", routeId);
        String result = smartRouteService.predictRouteCompletion(routeId);
        return Result.success("预测路线完成时间成功", result);
    }
}
