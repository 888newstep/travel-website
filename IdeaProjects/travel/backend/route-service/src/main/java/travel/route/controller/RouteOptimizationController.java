package travel.route.controller;

import lombok.RequiredArgsConstructor;
import travel.route.service.RouteOptimizationService;
import travel.common.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 路线优化控制器
 */
@RestController
@RequestMapping("/route-optimization")
@RequiredArgsConstructor
public class RouteOptimizationController {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationController.class);

    private final RouteOptimizationService routeOptimizationService;

    /**
     * 获取路线优化建议
     * GET /api/route-optimization/suggestions/{routeId}
     */
    @GetMapping("/suggestions/{routeId}")
    public Result<List<Map<String, Object>>> getOptimizationSuggestions(@PathVariable Integer routeId) {
        try {
            log.info("获取路线优化建议请求: routeId={}", routeId);
            List<Map<String, Object>> suggestions = routeOptimizationService.getOptimizationSuggestions(routeId);
            return Result.success("获取建议成功", suggestions);
        } catch (Exception e) {
            log.error("获取路线优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取建议失败: " + e.getMessage());
        }
    }

    /**
     * 应用优化方案
     * POST /api/route-optimization/apply
     */
    @PostMapping("/apply")
    public Result<Boolean> applyOptimization(@RequestBody Map<String, Object> optimizationData) {
        try {
            log.info("应用优化方案请求: routeId={}", optimizationData.get("routeId"));
            boolean result = routeOptimizationService.applyOptimization(optimizationData);
            return Result.success("应用优化方案成功", result);
        } catch (Exception e) {
            log.error("应用优化方案失败: error={}", e.getMessage());
            return Result.error("应用优化方案失败: " + e.getMessage());
        }
    }

    /**
     * 获取优化历史记录
     * GET /api/route-optimization/history/{routeId}
     */
    @GetMapping("/history/{routeId}")
    public Result<List<Map<String, Object>>> getOptimizationHistory(@PathVariable Integer routeId) {
        try {
            log.info("获取优化历史记录请求: routeId={}", routeId);
            List<Map<String, Object>> history = routeOptimizationService.getOptimizationHistory(routeId);
            return Result.success("获取历史记录成功", history);
        } catch (Exception e) {
            log.error("获取优化历史记录失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }
}