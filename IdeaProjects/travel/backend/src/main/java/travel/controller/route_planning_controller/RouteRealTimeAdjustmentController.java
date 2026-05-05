package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.service.route_planning.RouteRealTimeAdjustmentService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 实时路线调整控制器
 * 处理基于实时因素的路线动态调整
 */
@RestController
@RequestMapping("/route-realtime-adjustment")
@RequiredArgsConstructor
public class RouteRealTimeAdjustmentController {

    private static final Logger log = LoggerFactory.getLogger(RouteRealTimeAdjustmentController.class);

    private final RouteRealTimeAdjustmentService routeRealTimeAdjustmentService;

    /**
     * 获取实时路线调整建议
     * POST /api/route-realtime-adjustment/adjust
     */
    @PostMapping("/adjust")
    public Result<Map<String, Object>> getRealTimeAdjustment(@RequestBody Map<String, Object> request) {
        try {
            log.info("获取实时路线调整建议请求: routeId={}", request.get("routeId"));
            Map<String, Object> adjustment = routeRealTimeAdjustmentService.getRealTimeAdjustment(request);
            return Result.success("获取调整建议成功", adjustment);
        } catch (Exception e) {
            log.error("获取实时路线调整建议失败: error={}", e.getMessage());
            return Result.error("获取调整建议失败: " + e.getMessage());
        }
    }

    /**
     * 应用路线调整
     * POST /api/route-realtime-adjustment/apply
     */
    @PostMapping("/apply")
    public Result<Boolean> applyRouteAdjustment(@RequestBody Map<String, Object> adjustmentData) {
        try {
            log.info("应用路线调整请求: routeId={}", adjustmentData.get("routeId"));
            boolean result = routeRealTimeAdjustmentService.applyRouteAdjustment(adjustmentData);
            return Result.success("应用调整成功", result);
        } catch (Exception e) {
            log.error("应用路线调整失败: error={}", e.getMessage());
            return Result.error("应用调整失败: " + e.getMessage());
        }
    }

    /**
     * 获取交通状况数据
     * GET /api/route-realtime-adjustment/traffic
     */
    @GetMapping("/traffic")
    public Result<Map<String, Object>> getTrafficConditions(@RequestParam String location,
                                                             @RequestParam String route) {
        try {
            log.info("获取交通状况数据请求: location={}, route={}", location, route);
            Map<String, Object> trafficData = routeRealTimeAdjustmentService.getTrafficConditions(location, route);
            return Result.success("获取交通状况成功", trafficData);
        } catch (Exception e) {
            log.error("获取交通状况数据失败: error={}", e.getMessage());
            return Result.error("获取交通状况失败: " + e.getMessage());
        }
    }

    /**
     * 获取天气影响评估
     * GET /api/route-realtime-adjustment/weather
     */
    @GetMapping("/weather")
    public Result<Map<String, Object>> getWeatherImpact(@RequestParam String location,
                                                         @RequestParam String route) {
        try {
            log.info("获取天气影响评估请求: location={}, route={}", location, route);
            Map<String, Object> weatherImpact = routeRealTimeAdjustmentService.getWeatherImpact(location, route);
            return Result.success("获取天气影响成功", weatherImpact);
        } catch (Exception e) {
            log.error("获取天气影响评估失败: error={}", e.getMessage());
            return Result.error("获取天气影响失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时拥堵预警
     * GET /api/route-realtime-adjustment/congestion-alert
     */
    @GetMapping("/congestion-alert")
    public Result<List<Map<String, Object>>> getCongestionAlerts(@RequestParam String route) {
        try {
            log.info("获取实时拥堵预警请求: route={}", route);
            List<Map<String, Object>> alerts = routeRealTimeAdjustmentService.getCongestionAlerts(route);
            return Result.success("获取拥堵预警成功", alerts);
        } catch (Exception e) {
            log.error("获取实时拥堵预警失败: error={}", e.getMessage());
            return Result.error("获取拥堵预警失败: " + e.getMessage());
        }
    }

    /**
     * 获取替代路线建议
     * POST /api/route-realtime-adjustment/alternative-routes
     */
    @PostMapping("/alternative-routes")
    public Result<List<Map<String, Object>>> getAlternativeRoutes(@RequestBody Map<String, Object> request) {
        try {
            log.info("获取替代路线建议请求: routeId={}", request.get("routeId"));
            List<Map<String, Object>> alternatives = routeRealTimeAdjustmentService.getAlternativeRoutes(request);
            return Result.success("获取替代路线成功", alternatives);
        } catch (Exception e) {
            log.error("获取替代路线建议失败: error={}", e.getMessage());
            return Result.error("获取替代路线失败: " + e.getMessage());
        }
    }

    /**
     * 更新实时位置
     * POST /api/route-realtime-adjustment/update-location
     */
    @PostMapping("/update-location")
    public Result<Boolean> updateRealTimeLocation(@RequestBody Map<String, Object> locationData) {
        try {
            log.info("更新实时位置请求: userId={}", locationData.get("userId"));
            boolean result = routeRealTimeAdjustmentService.updateRealTimeLocation(locationData);
            return Result.success("更新位置成功", result);
        } catch (Exception e) {
            log.error("更新实时位置失败: error={}", e.getMessage());
            return Result.error("更新位置失败: " + e.getMessage());
        }
    }

    /**
     * 获取预计到达时间
     * GET /api/route-realtime-adjustment/eta
     */
    @GetMapping("/eta")
    public Result<Map<String, Object>> getEstimatedArrivalTime(@RequestParam Integer routeId,
                                                              @RequestParam Double currentDistance) {
        try {
            log.info("获取预计到达时间请求: routeId={}, currentDistance={}", routeId, currentDistance);
            Map<String, Object> eta = routeRealTimeAdjustmentService.getEstimatedArrivalTime(routeId, currentDistance);
            return Result.success("获取预计到达时间成功", eta);
        } catch (Exception e) {
            log.error("获取预计到达时间失败: error={}", e.getMessage());
            return Result.error("获取预计到达时间失败: " + e.getMessage());
        }
    }

    /**
     * 获取调整历史记录
     * GET /api/route-realtime-adjustment/history/{routeId}
     */
    @GetMapping("/history/{routeId}")
    public Result<List<Map<String, Object>>> getAdjustmentHistory(@PathVariable Integer routeId) {
        try {
            log.info("获取调整历史记录请求: routeId={}", routeId);
            List<Map<String, Object>> history = routeRealTimeAdjustmentService.getAdjustmentHistory(routeId);
            return Result.success("获取历史记录成功", history);
        } catch (Exception e) {
            log.error("获取调整历史记录失败: error={}", e.getMessage());
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }
}
