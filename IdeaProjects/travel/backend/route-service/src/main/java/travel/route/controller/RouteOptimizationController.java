package travel.route.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import travel.common.utils.Result;
import travel.route.dto.optimization.ApplyOptimizationRequest;
import travel.route.dto.optimization.OptimizationHistoryItem;
import travel.route.dto.optimization.OptimizationSuggestion;
import travel.route.service.RouteOptimizationService;

import java.util.List;

@RestController
@RequestMapping("/route-optimization")
@RequiredArgsConstructor
public class RouteOptimizationController {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationController.class);
    private final RouteOptimizationService routeOptimizationService;

    @GetMapping("/suggestions/{routeId}")
    public Result<List<OptimizationSuggestion>> getOptimizationSuggestions(@PathVariable Integer routeId) {
        try {
            log.info("获取路线优化建议请求: routeId={}", routeId);
            List<OptimizationSuggestion> suggestions = routeOptimizationService.getOptimizationSuggestions(routeId);
            return Result.success("获取建议成功", suggestions);
        } catch (Exception e) {
            log.error("获取路线优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取建议失败: " + e.getMessage());
        }
    }

    @PostMapping("/apply")
    public Result<Boolean> applyOptimization(@Valid @RequestBody ApplyOptimizationRequest request) {
        try {
            log.info("应用优化方案请求: routeId={}, suggestionId={}, optimizationType={}",
                    request.getRouteId(), request.getSuggestionId(), request.getOptimizationType());
            boolean result = routeOptimizationService.applyOptimization(request);
            return Result.success("应用优化方案成功", result);
        } catch (Exception e) {
            log.error("应用优化方案失败: error={}", e.getMessage());
            return Result.error("应用优化方案失败: " + e.getMessage());
        }
    }

    @GetMapping("/history/{routeId}")
    public Result<List<OptimizationHistoryItem>> getOptimizationHistory(@PathVariable Integer routeId) {
        try {
            log.info("获取优化历史记录请求: routeId={}", routeId);
            List<OptimizationHistoryItem> history = routeOptimizationService.getOptimizationHistory(routeId);
            return Result.success("获取历史记录成功", history);
        } catch (Exception e) {
            log.error("获取优化历史记录失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }
}
