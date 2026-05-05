package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.service.route_planning.TravelAnalyticsService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 旅游分析控制器
 * 提供数据统计、趋势分析、用户行为分析等功能
 */
@RestController
@RequestMapping("/travel-analytics")
@RequiredArgsConstructor
public class TravelAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(TravelAnalyticsController.class);

    private final TravelAnalyticsService travelAnalyticsService;

    /**
     * 获取平台整体数据概览
     * GET /api/travel-analytics/overview
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getPlatformOverview() {
        try {
            log.info("获取平台整体数据概览请求");
            Map<String, Object> overview = travelAnalyticsService.getPlatformOverview();
            return Result.success("获取平台整体数据概览成功", overview);
        } catch (Exception e) {
            log.error("获取平台整体数据概览失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户增长趋势
     * GET /api/travel-analytics/user-growth
     */
    @GetMapping("/user-growth")
    public Result<List<Map<String, Object>>> getUserGrowthTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String groupBy) {
        try {
            log.info("获取用户增长趋势请求: startDate={}, endDate={}, groupBy={}", startDate, endDate, groupBy);
            List<Map<String, Object>> trend = travelAnalyticsService.getUserGrowthTrend(startDate, endDate, groupBy);
            return Result.success("获取用户增长趋势成功", trend);
        } catch (Exception e) {
            log.error("获取用户增长趋势失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取路线热度排行
     * GET /api/travel-analytics/route-popularity
     */
    @GetMapping("/route-popularity")
    public Result<List<Map<String, Object>>> getRoutePopularityRanking(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "week") String timeRange) {
        try {
            log.info("获取路线热度排行请求: limit={}, timeRange={}", limit, timeRange);
            List<Map<String, Object>> ranking = travelAnalyticsService.getRoutePopularityRanking(limit, timeRange);
            return Result.success("获取路线热度排行成功", ranking);
        } catch (Exception e) {
            log.error("获取路线热度排行失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取景点访问量统计
     * GET /api/travel-analytics/attraction-visits
     */
    @GetMapping("/attraction-visits")
    public Result<List<Map<String, Object>>> getAttractionVisitStats(
            @RequestParam Integer cityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            log.info("获取景点访问量统计请求: cityId={}", cityId);
            List<Map<String, Object>> stats = travelAnalyticsService.getAttractionVisitStats(cityId, startDate, endDate);
            return Result.success("获取景点访问量统计成功", stats);
        } catch (Exception e) {
            log.error("获取景点访问量统计失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户行为分析
     * GET /api/travel-analytics/user-behavior/{userId}
     */
    @GetMapping("/user-behavior/{userId}")
    public Result<Map<String, Object>> getUserBehaviorAnalysis(@PathVariable Integer userId) {
        try {
            log.info("获取用户行为分析请求: userId={}", userId);
            Map<String, Object> analysis = travelAnalyticsService.getUserBehaviorAnalysis(userId);
            return Result.success("获取用户行为分析成功", analysis);
        } catch (Exception e) {
            log.error("获取用户行为分析失败: userId={}, error={}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取路线完成率统计
     * GET /api/travel-analytics/route-completion
     */
    @GetMapping("/route-completion")
    public Result<Map<String, Object>> getRouteCompletionStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            log.info("获取路线完成率统计请求");
            Map<String, Object> stats = travelAnalyticsService.getRouteCompletionStats(startDate, endDate);
            return Result.success("获取路线完成率统计成功", stats);
        } catch (Exception e) {
            log.error("获取路线完成率统计失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取预算分析
     * GET /api/travel-analytics/budget/{routeId}
     */
    @GetMapping("/budget/{routeId}")
    public Result<Map<String, Object>> getBudgetAnalysis(@PathVariable Integer routeId) {
        try {
            log.info("获取预算分析请求: routeId={}", routeId);
            Map<String, Object> analysis = travelAnalyticsService.getBudgetAnalysis(routeId);
            return Result.success("获取预算分析成功", analysis);
        } catch (Exception e) {
            log.error("获取预算分析失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取交通方式偏好统计
     * GET /api/travel-analytics/transport-preference/{cityId}
     */
    @GetMapping("/transport-preference/{cityId}")
    public Result<Map<String, Object>> getTransportPreferenceStats(@PathVariable Integer cityId) {
        try {
            log.info("获取交通方式偏好统计请求: cityId={}", cityId);
            Map<String, Object> stats = travelAnalyticsService.getTransportPreferenceStats(cityId);
            return Result.success("获取交通方式偏好统计成功", stats);
        } catch (Exception e) {
            log.error("获取交通方式偏好统计失败: cityId={}, error={}", cityId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取季节性趋势分析
     * GET /api/travel-analytics/seasonal-trends
     */
    @GetMapping("/seasonal-trends")
    public Result<List<Map<String, Object>>> getSeasonalTrends(
            @RequestParam Integer cityId,
            @RequestParam Integer year) {
        try {
            log.info("获取季节性趋势分析请求: cityId={}, year={}", cityId, year);
            List<Map<String, Object>> trends = travelAnalyticsService.getSeasonalTrends(cityId, year);
            return Result.success("获取季节性趋势分析成功", trends);
        } catch (Exception e) {
            log.error("获取季节性趋势分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户满意度分析
     * GET /api/travel-analytics/satisfaction/{routeId}
     */
    @GetMapping("/satisfaction/{routeId}")
    public Result<Map<String, Object>> getSatisfactionAnalysis(@PathVariable Integer routeId) {
        try {
            log.info("获取用户满意度分析请求: routeId={}", routeId);
            Map<String, Object> analysis = travelAnalyticsService.getSatisfactionAnalysis(routeId);
            return Result.success("获取用户满意度分析成功", analysis);
        } catch (Exception e) {
            log.error("获取用户满意度分析失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成数据报表
     * POST /api/travel-analytics/report
     */
    @PostMapping("/report")
    public Result<Map<String, Object>> generateReport(@RequestBody Map<String, Object> request) {
        try {
            String reportType = (String) request.get("reportType");
            LocalDate startDate = LocalDate.parse(request.get("startDate").toString());
            LocalDate endDate = LocalDate.parse(request.get("endDate").toString());
            String format = (String) request.get("format");
            log.info("生成数据报表请求: reportType={}, format={}", reportType, format);
            Map<String, Object> report = travelAnalyticsService.generateReport(reportType, startDate, endDate, format);
            return Result.success("生成数据报表成功", report);
        } catch (Exception e) {
            log.error("生成数据报表失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取实时数据监控
     * GET /api/travel-analytics/realtime-monitoring
     */
    @GetMapping("/realtime-monitoring")
    public Result<Map<String, Object>> getRealtimeMonitoring() {
        try {
            log.info("获取实时数据监控请求");
            Map<String, Object> monitoring = travelAnalyticsService.getRealtimeMonitoring();
            return Result.success("获取实时数据监控成功", monitoring);
        } catch (Exception e) {
            log.error("获取实时数据监控失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取热门搜索关键词
     * GET /api/travel-analytics/hot-keywords
     */
    @GetMapping("/hot-keywords")
    public Result<List<Map<String, Object>>> getHotSearchKeywords(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "week") String timeRange) {
        try {
            log.info("获取热门搜索关键词请求: limit={}, timeRange={}", limit, timeRange);
            List<Map<String, Object>> keywords = travelAnalyticsService.getHotSearchKeywords(limit, timeRange);
            return Result.success("获取热门搜索关键词成功", keywords);
        } catch (Exception e) {
            log.error("获取热门搜索关键词失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户留存率分析
     * GET /api/travel-analytics/user-retention
     */
    @GetMapping("/user-retention")
    public Result<Map<String, Object>> getUserRetentionAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortDate) {
        try {
            log.info("获取用户留存率分析请求: cohortDate={}", cohortDate);
            Map<String, Object> analysis = travelAnalyticsService.getUserRetentionAnalysis(cohortDate);
            return Result.success("获取用户留存率分析成功", analysis);
        } catch (Exception e) {
            log.error("获取用户留存率分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取转化率漏斗分析
     * GET /api/travel-analytics/conversion-funnel
     */
    @GetMapping("/conversion-funnel")
    public Result<Map<String, Object>> getConversionFunnel() {
        try {
            log.info("获取转化率漏斗分析请求");
            Map<String, Object> funnel = travelAnalyticsService.getConversionFunnel();
            return Result.success("获取转化率漏斗分析成功", funnel);
        } catch (Exception e) {
            log.error("获取转化率漏斗分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取地理分布统计
     * GET /api/travel-analytics/geographic-distribution
     */
    @GetMapping("/geographic-distribution")
    public Result<List<Map<String, Object>>> getGeographicDistribution() {
        try {
            log.info("获取地理分布统计请求");
            List<Map<String, Object>> distribution = travelAnalyticsService.getGeographicDistribution();
            return Result.success("获取地理分布统计成功", distribution);
        } catch (Exception e) {
            log.error("获取地理分布统计失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 预测未来趋势
     * GET /api/travel-analytics/predict
     */
    @GetMapping("/predict")
    public Result<Map<String, Object>> predictFutureTrends(
            @RequestParam String metric,
            @RequestParam Integer days) {
        try {
            log.info("预测未来趋势请求: metric={}, days={}", metric, days);
            Map<String, Object> prediction = travelAnalyticsService.predictFutureTrends(metric, days);
            return Result.success("预测未来趋势成功", prediction);
        } catch (Exception e) {
            log.error("预测未来趋势失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取异常检测报告
     * GET /api/travel-analytics/anomaly-detection
     */
    @GetMapping("/anomaly-detection")
    public Result<List<Map<String, Object>>> getAnomalyDetectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            log.info("获取异常检测报告请求");
            List<Map<String, Object>> report = travelAnalyticsService.getAnomalyDetectionReport(startDate, endDate);
            return Result.success("获取异常检测报告成功", report);
        } catch (Exception e) {
            log.error("获取异常检测报告失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 对比分析
     * POST /api/travel-analytics/compare
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> comparePeriods(@RequestBody Map<String, Object> request) {
        try {
            String metric = (String) request.get("metric");
            @SuppressWarnings("unchecked")
            Map<String, LocalDate> period1 = (Map<String, LocalDate>) request.get("period1");
            @SuppressWarnings("unchecked")
            Map<String, LocalDate> period2 = (Map<String, LocalDate>) request.get("period2");
            log.info("对比分析请求: metric={}", metric);
            Map<String, Object> comparison = travelAnalyticsService.comparePeriods(metric, period1, period2);
            return Result.success("对比分析成功", comparison);
        } catch (Exception e) {
            log.error("对比分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户画像分析
     * GET /api/travel-analytics/user-persona
     */
    @GetMapping("/user-persona")
    public Result<Map<String, Object>> getUserPersonaAnalysis() {
        try {
            log.info("获取用户画像分析请求");
            Map<String, Object> analysis = travelAnalyticsService.getUserPersonaAnalysis();
            return Result.success("获取用户画像分析成功", analysis);
        } catch (Exception e) {
            log.error("获取用户画像分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取推荐效果分析
     * GET /api/travel-analytics/recommendation-effect
     */
    @GetMapping("/recommendation-effect")
    public Result<Map<String, Object>> getRecommendationEffectAnalysis() {
        try {
            log.info("获取推荐效果分析请求");
            Map<String, Object> analysis = travelAnalyticsService.getRecommendationEffectAnalysis();
            return Result.success("获取推荐效果分析成功", analysis);
        } catch (Exception e) {
            log.error("获取推荐效果分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}