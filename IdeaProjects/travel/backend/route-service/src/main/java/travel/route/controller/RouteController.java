package travel.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import travel.common.entity.route_planning.Route;
import travel.route.service.*;
import travel.common.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Validated
@Tag(name = "路线管理", description = "路线CRUD、智能推荐、优化等接口")
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RouteService routeService;
    private final IntelligentRouteService intelligentRouteService;

    // ==================== CRUD操作 ====================

    @PostMapping
    @Operation(summary = "创建路线", description = "创建新的旅游路线")
    public Result<Route> createRoute(@RequestBody Route route) {
        log.info("创建路线请求: userId={}, title={}", route.getUserId(), route.getTitle());
        boolean result = routeService.save(route);
        if (result) {
            return Result.success("创建路线成功", route);
        }
        return Result.error("创建路线失败");
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取路线详情", description = "根据ID获取路线详情")
    public Result<Route> getRoute(@PathVariable Integer id) {
        log.info("获取路线详情请求: id={}", id);
        Route route = routeService.getById(id);
        if (route != null) {
            return Result.success("获取路线详情成功", route);
        }
        return Result.error("路线不存在");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新路线", description = "更新路线信息")
    public Result<Route> updateRoute(@PathVariable Integer id,
                                    @RequestBody Route route) {
        log.info("更新路线请求: id={}", id);
        route.setId(id);
        boolean result = routeService.updateById(route);
        if (result) {
            return Result.success("更新路线成功", route);
        }
        return Result.error("更新路线失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除路线", description = "删除路线")
    public Result<Boolean> deleteRoute(@PathVariable Integer id,
                                      @RequestParam Long userId) {
        log.info("删除路线请求: id={}, userId={}", id, userId);
        routeService.checkRouteOwner(id.longValue(), userId);
        boolean result = routeService.removeById(id);
        if (result) {
            return Result.success("删除路线成功", true);
        }
        return Result.error("删除路线失败");
    }

    // ==================== 查询操作 ====================

    @GetMapping("/my")
    @Operation(summary = "获取我的路线", description = "获取用户创建的所有路线")
    public Result<List<Route>> getMyRoutes(@RequestParam Long userId) {
        log.info("获取我的路线请求: userId={}", userId);
        List<Route> routes = routeService.getMyRoutes(userId);
        return Result.success("获取我的路线成功", routes);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索路线", description = "根据标题搜索路线")
    public Result<List<Route>> searchRoutes(@RequestParam String title) {
        log.info("搜索路线请求: title={}", title);
        List<Route> routes = routeService.searchRoutesByTitle(title);
        return Result.success("搜索路线成功", routes);
    }

    @GetMapping("/city/{cityId}")
    @Operation(summary = "按城市获取路线", description = "根据城市ID获取路线")
    public Result<List<Route>> getRoutesByCity(@PathVariable Integer cityId) {
        log.info("根据城市获取路线请求: cityId={}", cityId);
        List<Route> routes = routeService.getByCityId(cityId);
        return Result.success("根据城市获取路线成功", routes);
    }

    @GetMapping("/count/{userId}")
    @Operation(summary = "获取用户路线数量", description = "获取用户创建的路线数量")
    public Result<Integer> getUserRouteCount(@PathVariable Long userId) {
        log.info("获取用户路线数量请求: userId={}", userId);
        int count = routeService.getUserRouteCount(userId);
        return Result.success("获取用户路线数量成功", count);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量获取路线", description = "批量获取多条路线")
    public Result<List<Route>> getBatchRoutes(@RequestBody List<Integer> routeIds) {
        log.info("批量获取路线请求: routeIds={}", routeIds);
        List<Route> routes = routeService.listByIds(routeIds);
        return Result.success("批量获取路线成功", routes);
    }

    // ==================== 路线操作 ====================

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制路线", description = "复制路线创建副本")
    public Result<Route> copyRoute(@PathVariable Integer id,
                                   @RequestParam Long userId) {
        log.info("复制路线请求: id={}, userId={}", id, userId);
        Route originalRoute = routeService.getById(id);
        if (originalRoute == null) {
            return Result.error("原路线不存在");
        }

        Route newRoute = new Route();
        newRoute.setTitle(originalRoute.getTitle() + " (副本)");
        newRoute.setUserId(userId.intValue());
        newRoute.setCityId(originalRoute.getCityId());

        boolean result = routeService.save(newRoute);
        if (result) {
            return Result.success("复制路线成功", newRoute);
        }
        return Result.error("复制路线失败");
    }

    @PutMapping("/{id}/visibility")
    @Operation(summary = "设置路线可见性", description = "设置路线是否公开")
    public Result<Boolean> setRouteVisibility(@PathVariable Integer id,
                                              @RequestParam Long userId,
                                              @RequestParam Boolean isPublic) {
        log.info("设置路线可见性请求: id={}, isPublic={}", id, isPublic);
        routeService.checkRouteOwner(id.longValue(), userId);

        Route route = routeService.getById(id);
        if (route == null) {
            return Result.error("路线不存在");
        }

        route.setIsPublic(isPublic);
        boolean result = routeService.updateById(route);
        if (result) {
            return Result.success("设置路线可见性成功", true);
        }
        return Result.error("设置路线可见性失败");
    }

    // ==================== 智能推荐 ====================

    @GetMapping("/smart/list")
    @Operation(summary = "智能路线列表", description = "获取热门/季节性/主题路线，type=popular|seasonal|theme")
    public Result<List<Map<String, Object>>> getSmartRoutes(
            @RequestParam String type,
            @RequestParam Integer cityId,
            @RequestParam Integer days,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String theme) {
        try {
            log.info("智能路线列表请求: type={}, cityId={}, days={}", type, cityId, days);
            List<Map<String, Object>> routes = switch (type) {
                case "seasonal" -> intelligentRouteService.getSeasonalRoutes(cityId, season != null ? season : "spring", days);
                case "theme" -> intelligentRouteService.getThemeRoutes(theme != null ? theme : "文化", cityId, days);
                default -> intelligentRouteService.getPopularRoutes(cityId, days, limit);
            };
            return Result.success("获取智能路线成功", routes);
        } catch (Exception e) {
            log.error("获取智能路线失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/smart/similar/{routeId}")
    @Operation(summary = "相似路线推荐", description = "获取相似路线")
    public Result<List<Map<String, Object>>> getSimilarRoutes(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取相似路线请求: routeId={}, limit={}", routeId, limit);
            List<Map<String, Object>> similarRoutes = intelligentRouteService.getSimilarRoutes(routeId, limit);
            return Result.success("获取相似路线成功", similarRoutes);
        } catch (Exception e) {
            log.error("获取相似路线失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/smart/recommend-by-preference")
    @Operation(summary = "基于偏好推荐", description = "基于用户偏好的智能路线推荐")
    public Result<List<Map<String, Object>>> recommendRoutesByUserPreference(@RequestParam Integer userId,
                                                                            @RequestParam Integer cityId,
                                                                            @RequestParam int days,
                                                                            @RequestBody Map<String, Object> preferences) {
        try {
            log.info("基于用户偏好推荐路线请求: userId={}, cityId={}, days={}", userId, cityId, days);
            List<Map<String, Object>> recommendations = intelligentRouteService.recommendRoutesByUserPreference(userId, cityId, days, preferences);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("基于用户偏好推荐路线失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ==================== 路线优化 ====================

    @PostMapping("/smart/optimize")
    @Operation(summary = "优化路线", description = "优化路线")
    public Result<String> optimizeRoute(@RequestParam Integer routeId) {
        log.info("优化路线请求: routeId={}", routeId);
        String result = intelligentRouteService.optimizeRoute(routeId);
        return Result.success("优化路线成功", result);
    }

    // ==================== 路线评估 ====================

    @PostMapping("/smart/evaluate/{routeId}")
    @Operation(summary = "评估路线质量", description = "评估路线质量")
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

    // ==================== 路线对比 ====================

    @PostMapping("/smart/compare")
    @Operation(summary = "比较路线", description = "多维度比较路线")
    public Result<Map<String, Object>> compareRoutes(@RequestParam List<Integer> routeIds) {
        try {
            log.info("比较路线请求: routeIds={}", routeIds);
            Map<String, Object> comparison = intelligentRouteService.compareRoutes(routeIds);
            return Result.success(comparison);
        } catch (Exception e) {
            log.error("比较路线失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    // ==================== 实时调整 ====================

    @PostMapping("/smart/real-time-adjustment/{routeId}")
    @Operation(summary = "实时路线调整", description = "获取实时路线调整建议")
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

    // ==================== 个性化路线生成 ====================

    @PostMapping("/smart/generate-personalized")
    @Operation(summary = "生成个性化路线", description = "AI生成个性化路线")
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
            log.error("生成个性化路线失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取路线主题列表
     * GET /api/routes/smart/themes
     */
    @GetMapping("/smart/themes")
    public Result<List<Map<String, String>>> getRouteThemes() {
        List<Map<String, String>> themes = List.of(
                Map.of("value", "自然风光", "label", "自然风光"),
                Map.of("value", "历史文化", "label", "历史文化"),
                Map.of("value", "美食之旅", "label", "美食之旅"),
                Map.of("value", "亲子游", "label", "亲子游"),
                Map.of("value", "摄影", "label", "摄影"),
                Map.of("value", "探险", "label", "探险"),
                Map.of("value", "休闲度假", "label", "休闲度假")
        );
        return Result.success(themes);
    }

    /**
     * 获取季节列表
     * GET /api/routes/smart/seasons
     */
    @GetMapping("/smart/seasons")
    public Result<List<Map<String, String>>> getRouteSeasons() {
        List<Map<String, String>> seasons = List.of(
                Map.of("value", "spring", "label", "春季"),
                Map.of("value", "summer", "label", "夏季"),
                Map.of("value", "autumn", "label", "秋季"),
                Map.of("value", "winter", "label", "冬季")
        );
        return Result.success(seasons);
    }
}