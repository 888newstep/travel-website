package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.service.route_planning.RouteOptimizationService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 路线优化控制器
 * 处理路线优化、路线改进建议等
 */
@RestController
@RequestMapping("/route-optimization")
@RequiredArgsConstructor
public class RouteOptimizationController {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationController.class);

    private final RouteOptimizationService routeOptimizationService;

    /**
     * 优化路线
     * POST /api/route-optimization/optimize
     */
    @PostMapping("/optimize")
    public Result<Map<String, Object>> optimizeRoute(@RequestBody Map<String, Object> routeData) {
        try {
            log.info("优化路线请求: routeId={}", routeData.get("routeId"));
            Map<String, Object> optimizedRoute = routeOptimizationService.optimizeRoute(routeData);
            return Result.success("路线优化成功", optimizedRoute);
        } catch (Exception e) {
            log.error("优化路线失败: error={}", e.getMessage());
            return Result.error("路线优化失败: " + e.getMessage());
        }
    }

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
     * 时间优化
     * POST /api/route-optimization/time
     */
    @PostMapping("/time")
    public Result<Map<String, Object>> optimizeTime(@RequestBody Map<String, Object> routeData) {
        try {
            log.info("时间优化请求: routeId={}", routeData.get("routeId"));
            Map<String, Object> result = routeOptimizationService.optimizeTime(routeData);
            return Result.success("时间优化成功", result);
        } catch (Exception e) {
            log.error("时间优化失败: error={}", e.getMessage());
            return Result.error("时间优化失败: " + e.getMessage());
        }
    }

    /**
     * 成本优化
     * POST /api/route-optimization/cost
     */
    @PostMapping("/cost")
    public Result<Map<String, Object>> optimizeCost(@RequestBody Map<String, Object> routeData) {
        try {
            log.info("成本优化请求: routeId={}", routeData.get("routeId"));
            Map<String, Object> result = routeOptimizationService.optimizeCost(routeData);
            return Result.success("成本优化成功", result);
        } catch (Exception e) {
            log.error("成本优化失败: error={}", e.getMessage());
            return Result.error("成本优化失败: " + e.getMessage());
        }
    }

    /**
     * 距离优化
     * POST /api/route-optimization/distance
     */
    @PostMapping("/distance")
    public Result<Map<String, Object>> optimizeDistance(@RequestBody Map<String, Object> routeData) {
        try {
            log.info("距离优化请求: routeId={}", routeData.get("routeId"));
            Map<String, Object> result = routeOptimizationService.optimizeDistance(routeData);
            return Result.success("距离优化成功", result);
        } catch (Exception e) {
            log.error("距离优化失败: error={}", e.getMessage());
            return Result.error("距离优化失败: " + e.getMessage());
        }
    }

    /**
     * 获取优化对比报告
     * GET /api/route-optimization/compare/{routeId}
     */
    @GetMapping("/compare/{routeId}")
    public Result<Map<String, Object>> getOptimizationComparison(@PathVariable Integer routeId) {
        try {
            log.info("获取优化对比报告请求: routeId={}", routeId);
            Map<String, Object> comparison = routeOptimizationService.getOptimizationComparison(routeId);
            return Result.success("获取对比报告成功", comparison);
        } catch (Exception e) {
            log.error("获取优化对比报告失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取对比报告失败: " + e.getMessage());
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

    /**
     * 批量优化路线
     * POST /api/route-optimization/batch
     */
    @PostMapping("/batch")
    public Result<Map<String, Object>> batchOptimizeRoutes(@RequestBody List<Integer> routeIds) {
        try {
            log.info("批量优化路线请求: count={}", routeIds.size());
            Map<String, Object> result = routeOptimizationService.batchOptimizeRoutes(routeIds);
            return Result.success("批量优化成功", result);
        } catch (Exception e) {
            log.error("批量优化路线失败: error={}", e.getMessage());
            return Result.error("批量优化失败: " + e.getMessage());
        }
    }
}