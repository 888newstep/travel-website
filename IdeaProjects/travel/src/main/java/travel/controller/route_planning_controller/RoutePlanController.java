package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.service.route_planning.IntelligentRouteService;
import travel.service.route_planning.RouteService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 路线规划控制器
 * 提供路线规划、智能路线生成、路线优化等功能
 */
@RestController
@RequestMapping("/route-plan")
@RequiredArgsConstructor
public class RoutePlanController {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanController.class);

    private final IntelligentRouteService intelligentRouteService;
    private final RouteService routeService;

    /**
     * 创建路线规划
     * POST /api/route-plan/create
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createRoutePlan(@RequestBody Map<String, Object> request) {
        try {
            log.info("创建路线规划请求");
            Map<String, Object> preferences = new java.util.HashMap<>();
            Object preferencesObj = request.get("preferences");
            if (preferencesObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) preferencesObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        preferences.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            Map<String, Object> constraints = new java.util.HashMap<>();
            Object constraintsObj = request.get("constraints");
            if (constraintsObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) constraintsObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        constraints.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            Map<String, Object> routePlan = intelligentRouteService.generatePersonalizedRoute(preferences, constraints);
            return Result.success("创建路线规划成功", routePlan);
        } catch (Exception e) {
            log.error("创建路线规划失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能路线推荐
     * POST /api/route-plan/recommend
     */
    @PostMapping("/recommend")
    public Result<List<Map<String, Object>>> recommendRoutes(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = (Integer) request.get("userId");
            Integer cityId = (Integer) request.get("cityId");
            Integer days = (Integer) request.get("days");
            Map<String, Object> preferences = new java.util.HashMap<>();
            Object preferencesObj = request.get("preferences");
            if (preferencesObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) preferencesObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        preferences.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            log.info("智能路线推荐请求: userId={}, cityId={}, days={}", userId, cityId, days);
            List<Map<String, Object>> recommendations = intelligentRouteService.recommendRoutesByUserPreference(
                    userId, cityId, days, preferences);
            return Result.success("智能路线推荐成功", recommendations);
        } catch (Exception e) {
            log.error("智能路线推荐失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 优化路线
     * POST /api/route-plan/optimize/{routeId}
     */
    @PostMapping("/optimize/{routeId}")
    public Result<Map<String, Object>> optimizeRoute(@PathVariable Integer routeId,
                                                      @RequestBody Map<String, Object> request) {
        try {
            String optimizationType = (String) request.get("optimizationType");
            log.info("优化路线请求: routeId={}, type={}", routeId, optimizationType);
            Map<String, Object> suggestions = intelligentRouteService.getRouteOptimizationSuggestions(
                    routeId, optimizationType);
            return Result.success("优化路线成功", suggestions);
        } catch (Exception e) {
            log.error("优化路线失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 评估路线质量
     * POST /api/route-plan/evaluate/{routeId}
     */
    @PostMapping("/evaluate/{routeId}")
    public Result<Map<String, Object>> evaluateRoute(@PathVariable Integer routeId,
                                                      @RequestBody Map<String, Object> request) {
        try {
            log.info("评估路线质量请求: routeId={}", routeId);
            Map<String, Object> evaluation = intelligentRouteService.evaluateRouteQuality(
                    routeId, request);
            return Result.success("评估路线质量成功", evaluation);
        } catch (Exception e) {
            log.error("评估路线质量失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取我的路线规划
     * GET /api/route-plan/my
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyRoutePlans(@RequestParam Long userId) {
        try {
            log.info("获取我的路线规划请求: userId={}", userId);
            List<Map<String, Object>> routes = routeService.getMyRoutes(userId).stream()
                    .map(route -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", route.getId());
                        map.put("title", route.getTitle());
                        map.put("days", route.getDurationDays());
                        map.put("createTime", route.getCreatedAt());
                        return map;
                    })
                    .toList();
            return Result.success("获取我的路线规划成功", routes);
        } catch (Exception e) {
            log.error("获取我的路线规划失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 比较多个路线
     * POST /api/route-plan/compare
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> compareRoutes(@RequestBody Map<String, Object> request) {
        try {
            List<Integer> routeIds = new java.util.ArrayList<>();
            Object routeIdsObj = request.get("routeIds");
            if (routeIdsObj instanceof List) {
                for (Object item : (List<?>) routeIdsObj) {
                    if (item instanceof Integer) {
                        routeIds.add((Integer) item);
                    }
                }
            }
            log.info("比较路线请求: routeIds={}", routeIds);
            Map<String, Object> comparison = intelligentRouteService.compareRoutes(routeIds);
            return Result.success("比较路线成功", comparison);
        } catch (Exception e) {
            log.error("比较路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实时路线调整
     * POST /api/route-plan/adjust/{routeId}
     */
    @PostMapping("/adjust/{routeId}")
    public Result<Map<String, Object>> realTimeAdjust(@PathVariable Integer routeId,
                                                       @RequestBody Map<String, Object> request) {
        try {
            log.info("实时路线调整请求: routeId={}", routeId);
            Map<String, Double> currentLocation = new java.util.HashMap<>();
            Object currentLocationObj = request.get("currentLocation");
            if (currentLocationObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) currentLocationObj).entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof Double) {
                        currentLocation.put((String) entry.getKey(), (Double) entry.getValue());
                    }
                }
            }
            Map<String, Object> realTimeFactors = new java.util.HashMap<>();
            Object realTimeFactorsObj = request.get("realTimeFactors");
            if (realTimeFactorsObj instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) realTimeFactorsObj).entrySet()) {
                    if (entry.getKey() instanceof String) {
                        realTimeFactors.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
            Map<String, Object> adjustment = intelligentRouteService.getRealTimeAdjustment(
                    routeId, currentLocation, realTimeFactors);
            return Result.success("实时路线调整成功", adjustment);
        } catch (Exception e) {
            log.error("实时路线调整失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取热门路线
     * GET /api/route-plan/popular
     */
    @GetMapping("/popular")
    public Result<List<Map<String, Object>>> getPopularRoutes(
            @RequestParam Integer cityId,
            @RequestParam Integer days,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取热门路线请求: cityId={}, days={}, limit={}", cityId, days, limit);
            List<Map<String, Object>> popularRoutes = intelligentRouteService.getPopularRoutes(
                    cityId, days, limit);
            return Result.success("获取热门路线成功", popularRoutes);
        } catch (Exception e) {
            log.error("获取热门路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取相似路线
     * GET /api/route-plan/similar/{routeId}
     */
    @GetMapping("/similar/{routeId}")
    public Result<List<Map<String, Object>>> getSimilarRoutes(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取相似路线请求: routeId={}, limit={}", routeId, limit);
            List<Map<String, Object>> similarRoutes = intelligentRouteService.getSimilarRoutes(
                    routeId, limit);
            return Result.success("获取相似路线成功", similarRoutes);
        } catch (Exception e) {
            log.error("获取相似路线失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取季节性路线
     * GET /api/route-plan/seasonal
     */
    @GetMapping("/seasonal")
    public Result<List<Map<String, Object>>> getSeasonalRoutes(
            @RequestParam Integer cityId,
            @RequestParam String season,
            @RequestParam Integer days) {
        try {
            log.info("获取季节性路线请求: cityId={}, season={}, days={}", cityId, season, days);
            List<Map<String, Object>> seasonalRoutes = intelligentRouteService.getSeasonalRoutes(
                    cityId, season, days);
            return Result.success("获取季节性路线成功", seasonalRoutes);
        } catch (Exception e) {
            log.error("获取季节性路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取主题路线
     * GET /api/route-plan/theme
     */
    @GetMapping("/theme")
    public Result<List<Map<String, Object>>> getThemeRoutes(
            @RequestParam String theme,
            @RequestParam Integer cityId,
            @RequestParam Integer days) {
        try {
            log.info("获取主题路线请求: theme={}, cityId={}, days={}", theme, cityId, days);
            List<Map<String, Object>> themeRoutes = intelligentRouteService.getThemeRoutes(
                    theme, cityId, days);
            return Result.success("获取主题路线成功", themeRoutes);
        } catch (Exception e) {
            log.error("获取主题路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索路线
     * GET /api/route-plan/search
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchRoutes(
            @RequestParam String title) {
        try {
            log.info("搜索路线请求: title={}", title);
            List<Map<String, Object>> routes = routeService.searchRoutesByTitle(title).stream()
                    .map(route -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", route.getId());
                        map.put("title", route.getTitle());
                        map.put("days", route.getDurationDays());
                        map.put("cityId", route.getCityId());
                        return map;
                    })
                    .toList();
            return Result.success("搜索路线成功", routes);
        } catch (Exception e) {
            log.error("搜索路线失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据城市获取路线
     * GET /api/route-plan/city/{cityId}
     */
    @GetMapping("/city/{cityId}")
    public Result<List<Map<String, Object>>> getRoutesByCity(@PathVariable Integer cityId) {
        try {
            log.info("根据城市获取路线请求: cityId={}", cityId);
            List<Map<String, Object>> routes = routeService.getByCityId(cityId).stream()
                    .map(route -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", route.getId());
                        map.put("title", route.getTitle());
                        map.put("days", route.getDurationDays());
                        map.put("userId", route.getUserId());
                        return map;
                    })
                    .toList();
            return Result.success("根据城市获取路线成功", routes);
        } catch (Exception e) {
            log.error("根据城市获取路线失败: cityId={}, error={}", cityId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
