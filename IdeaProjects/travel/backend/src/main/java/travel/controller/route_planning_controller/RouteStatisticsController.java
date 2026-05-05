package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.service.route_planning.RouteStatisticsService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 路线统计控制器
 * 处理路线数据统计、分析和报告生成
 */
@RestController
@RequestMapping("/route-statistics")
@RequiredArgsConstructor
public class RouteStatisticsController {

    private static final Logger log = LoggerFactory.getLogger(RouteStatisticsController.class);

    private final RouteStatisticsService routeStatisticsService;

    /**
     * 获取路线基础统计
     * GET /api/route-statistics/basic/{routeId}
     */
    @GetMapping("/basic/{routeId}")
    public Result<Map<String, Object>> getBasicStatistics(@PathVariable Integer routeId) {
        try {
            log.info("获取路线基础统计请求: routeId={}", routeId);
            Map<String, Object> statistics = routeStatisticsService.getBasicStatistics(routeId);
            return Result.success("获取基础统计成功", statistics);
        } catch (Exception e) {
            log.error("获取路线基础统计失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取基础统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取路线使用统计
     * GET /api/route-statistics/usage/{routeId}
     */
    @GetMapping("/usage/{routeId}")
    public Result<Map<String, Object>> getUsageStatistics(@PathVariable Integer routeId,
                                                           @RequestParam String timeRange) {
        try {
            log.info("获取路线使用统计请求: routeId={}, timeRange={}", routeId, timeRange);
            Map<String, Object> statistics = routeStatisticsService.getUsageStatistics(routeId, timeRange);
            return Result.success("获取使用统计成功", statistics);
        } catch (Exception e) {
            log.error("获取路线使用统计失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取使用统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取路线评分统计
     * GET /api/route-statistics/rating/{routeId}
     */
    @GetMapping("/rating/{routeId}")
    public Result<Map<String, Object>> getRatingStatistics(@PathVariable Integer routeId) {
        try {
            log.info("获取路线评分统计请求: routeId={}", routeId);
            Map<String, Object> statistics = routeStatisticsService.getRatingStatistics(routeId);
            return Result.success("获取评分统计成功", statistics);
        } catch (Exception e) {
            log.error("获取路线评分统计失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取评分统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取路线趋势分析
     * GET /api/route-statistics/trend/{routeId}
     */
    @GetMapping("/trend/{routeId}")
    public Result<Map<String, Object>> getTrendAnalysis(@PathVariable Integer routeId,
                                                         @RequestParam int days) {
        try {
            log.info("获取路线趋势分析请求: routeId={}, days={}", routeId, days);
            Map<String, Object> analysis = routeStatisticsService.getTrendAnalysis(routeId, days);
            return Result.success("获取趋势分析成功", analysis);
        } catch (Exception e) {
            log.error("获取路线趋势分析失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取趋势分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户偏好统计
     * GET /api/route-statistics/user-preference/{userId}
     */
    @GetMapping("/user-preference/{userId}")
    public Result<Map<String, Object>> getUserPreferenceStatistics(@PathVariable Integer userId) {
        try {
            log.info("获取用户偏好统计请求: userId={}", userId);
            Map<String, Object> statistics = routeStatisticsService.getUserPreferenceStatistics(userId);
            return Result.success("获取用户偏好统计成功", statistics);
        } catch (Exception e) {
            log.error("获取用户偏好统计失败: userId={}, error={}", userId, e.getMessage());
            return Result.error("获取用户偏好统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门路线统计
     * GET /api/route-statistics/popular
     */
    @GetMapping("/popular")
    public Result<List<Map<String, Object>>> getPopularRoutes(@RequestParam String cityId,
                                                               @RequestParam int limit) {
        try {
            log.info("获取热门路线统计请求: cityId={}, limit={}", cityId, limit);
            List<Map<String, Object>> routes = routeStatisticsService.getPopularRoutes(cityId, limit);
            return Result.success("获取热门路线成功", routes);
        } catch (Exception e) {
            log.error("获取热门路线统计失败: error={}", e.getMessage());
            return Result.error("获取热门路线失败: " + e.getMessage());
        }
    }

    /**
     * 生成统计报告
     * POST /api/route-statistics/report
     */
    @PostMapping("/report")
    public Result<String> generateStatisticsReport(@RequestBody Map<String, Object> reportConfig) {
        try {
            log.info("生成统计报告请求: type={}", reportConfig.get("type"));
            String reportUrl = routeStatisticsService.generateStatisticsReport(reportConfig);
            return Result.success("生成报告成功", reportUrl);
        } catch (Exception e) {
            log.error("生成统计报告失败: error={}", e.getMessage());
            return Result.error("生成报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取路线对比统计
     * POST /api/route-statistics/compare
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> compareRouteStatistics(@RequestBody List<Integer> routeIds) {
        try {
            log.info("获取路线对比统计请求: count={}", routeIds.size());
            Map<String, Object> comparison = routeStatisticsService.compareRouteStatistics(routeIds);
            return Result.success("获取对比统计成功", comparison);
        } catch (Exception e) {
            log.error("获取路线对比统计失败: error={}", e.getMessage());
            return Result.error("获取对比统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取路线完成率统计
     * GET /api/route-statistics/completion-rate/{routeId}
     */
    @GetMapping("/completion-rate/{routeId}")
    public Result<Map<String, Object>> getCompletionRateStatistics(@PathVariable Integer routeId) {
        try {
            log.info("获取路线完成率统计请求: routeId={}", routeId);
            Map<String, Object> statistics = routeStatisticsService.getCompletionRateStatistics(routeId);
            return Result.success("获取完成率统计成功", statistics);
        } catch (Exception e) {
            log.error("获取路线完成率统计失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error("获取完成率统计失败: " + e.getMessage());
        }
    }

    /**
     * 导出统计数据
     * POST /api/route-statistics/export
     */
    @PostMapping("/export")
    public Result<String> exportStatisticsData(@RequestBody Map<String, Object> exportConfig) {
        try {
            log.info("导出统计数据请求: format={}", exportConfig.get("format"));
            String downloadUrl = routeStatisticsService.exportStatisticsData(exportConfig);
            return Result.success("导出成功", downloadUrl);
        } catch (Exception e) {
            log.error("导出统计数据失败: error={}", e.getMessage());
            return Result.error("导出失败: " + e.getMessage());
        }
    }
}
