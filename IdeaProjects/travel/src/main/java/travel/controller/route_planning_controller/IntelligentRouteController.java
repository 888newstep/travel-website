package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.service.route_planning.IntelligentRouteService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/intelligent-route")
@RequiredArgsConstructor
public class IntelligentRouteController {

    private static final Logger log = LoggerFactory.getLogger(IntelligentRouteController.class);

    private final IntelligentRouteService intelligentRouteService;

    /**
     * 基于用户偏好的智能路线推荐
     * POST /api/intelligent-route/recommend-by-preference
     */
    @PostMapping("/recommend-by-preference")
    public Result<List<Map<String, Object>>> recommendRoutesByUserPreference(@RequestParam Integer userId,
                                                @RequestParam Integer cityId,
                                                @RequestParam int days,
                                                @RequestBody Map<String, Object> preferences) {
        try {
            log.info("基于用户偏好推荐路线请求: userId={}, cityId={}, days={}", userId, cityId, days);
            List<Map<String, Object>> recommendations = intelligentRouteService.recommendRoutesByUserPreference(userId, cityId, days, preferences);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("基于用户偏好推荐路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 多维度路线比较
     * POST /api/intelligent-route/compare
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> compareRoutes(@RequestParam List<Integer> routeIds) {
        try {
            log.info("比较路线请求: routeIds={}", routeIds);
            Map<String, Object> comparison = intelligentRouteService.compareRoutes(routeIds);
            return Result.success(comparison);
        } catch (Exception e) {
            log.error("比较路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实时路线调整建议
     * POST /api/intelligent-route/real-time-adjustment/{routeId}
     */
    @PostMapping("/real-time-adjustment/{routeId}")
    public Result<Map<String, Object>> getRealTimeAdjustment(@PathVariable Integer routeId,
                                       @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("获取实时路线调整建议请求: routeId={}", routeId);
            Map<String, Double> currentLocation = null;
            if (requestBody.get("currentLocation") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Double> tempLocation = (Map<String, Double>) requestBody.get("currentLocation");
                currentLocation = tempLocation;
            }
            Map<String, Object> realTimeFactors = null;
            if (requestBody.get("realTimeFactors") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tempFactors = (Map<String, Object>) requestBody.get("realTimeFactors");
                realTimeFactors = tempFactors;
            }
            Map<String, Object> adjustment = intelligentRouteService.getRealTimeAdjustment(routeId, currentLocation, realTimeFactors);
            return Result.success(adjustment);
        } catch (Exception e) {
            log.error("获取实时路线调整建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 路线质量评估
     * POST /api/intelligent-route/evaluate/{routeId}
     */
    @PostMapping("/evaluate/{routeId}")
    public Result<Map<String, Object>> evaluateRouteQuality(@PathVariable Integer routeId,
                                     @RequestBody Map<String, Object> evaluationParams) {
        try {
            log.info("评估路线质量请求: routeId={}", routeId);
            Map<String, Object> evaluation = intelligentRouteService.evaluateRouteQuality(routeId, evaluationParams);
            return Result.success(evaluation);
        } catch (Exception e) {
            log.error("评估路线质量失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 个性化路线生成
     * POST /api/intelligent-route/generate-personalized
     */
    @PostMapping("/generate-personalized")
    public Result<Map<String, Object>> generatePersonalizedRoute(@RequestBody Map<String, Object> requestBody) {
        try {
            log.info("生成个性化路线请求");
            Map<String, Object> userPreferences = null;
            if (requestBody.get("userPreferences") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tempPreferences = (Map<String, Object>) requestBody.get("userPreferences");
                userPreferences = tempPreferences;
            }
            Map<String, Object> constraints = null;
            if (requestBody.get("constraints") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tempConstraints = (Map<String, Object>) requestBody.get("constraints");
                constraints = tempConstraints;
            }
            Map<String, Object> route = intelligentRouteService.generatePersonalizedRoute(userPreferences, constraints);
            return Result.success(route);
        } catch (Exception e) {
            log.error("生成个性化路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 热门路线推荐
     * GET /api/intelligent-route/popular
     */
    @GetMapping("/popular")
    public Result<List<Map<String, Object>>> getPopularRoutes(@RequestParam Integer cityId,
                                 @RequestParam int days,
                                 @RequestParam(required = false, defaultValue = "5") int limit) {
        try {
            log.info("获取热门路线请求: cityId={}, days={}, limit={}", cityId, days, limit);
            List<Map<String, Object>> popularRoutes = intelligentRouteService.getPopularRoutes(cityId, days, limit);
            return Result.success(popularRoutes);
        } catch (Exception e) {
            log.error("获取热门路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 相似路线推荐
     * GET /api/intelligent-route/similar/{routeId}
     */
    @GetMapping("/similar/{routeId}")
    public Result<List<Map<String, Object>>> getSimilarRoutes(@PathVariable Integer routeId,
                                 @RequestParam(required = false, defaultValue = "5") int limit) {
        try {
            log.info("获取相似路线请求: routeId={}, limit={}", routeId, limit);
            List<Map<String, Object>> similarRoutes = intelligentRouteService.getSimilarRoutes(routeId, limit);
            return Result.success(similarRoutes);
        } catch (Exception e) {
            log.error("获取相似路线失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 季节性路线推荐
     * GET /api/intelligent-route/seasonal
     */
    @GetMapping("/seasonal")
    public Result<List<Map<String, Object>>> getSeasonalRoutes(@RequestParam Integer cityId,
                                  @RequestParam String season,
                                  @RequestParam int days) {
        try {
            log.info("获取季节性路线请求: cityId={}, season={}, days={}", cityId, season, days);
            List<Map<String, Object>> seasonalRoutes = intelligentRouteService.getSeasonalRoutes(cityId, season, days);
            return Result.success(seasonalRoutes);
        } catch (Exception e) {
            log.error("获取季节性路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 主题路线推荐
     * GET /api/intelligent-route/theme
     */
    @GetMapping("/theme")
    public Result<List<Map<String, Object>>> getThemeRoutes(@RequestParam String theme,
                               @RequestParam Integer cityId,
                               @RequestParam int days) {
        try {
            log.info("获取主题路线请求: theme={}, cityId={}, days={}", theme, cityId, days);
            List<Map<String, Object>> themeRoutes = intelligentRouteService.getThemeRoutes(theme, cityId, days);
            return Result.success(themeRoutes);
        } catch (Exception e) {
            log.error("获取主题路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 路线优化建议
     * GET /api/intelligent-route/optimization-suggestions/{routeId}
     */
    @GetMapping("/optimization-suggestions/{routeId}")
    public Result<Map<String, Object>> getRouteOptimizationSuggestions(@PathVariable Integer routeId,
                                           @RequestParam(required = false, defaultValue = "comprehensive") String optimizationType) {
        try {
            log.info("获取路线优化建议请求: routeId={}, optimizationType={}", routeId, optimizationType);
            Map<String, Object> suggestions = intelligentRouteService.getRouteOptimizationSuggestions(routeId, optimizationType);
            return Result.success(suggestions);
        } catch (Exception e) {
            log.error("获取路线优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
