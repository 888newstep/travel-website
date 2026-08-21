package travel.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import travel.common.entity.route_planning.Route;
import travel.common.security.AuthenticatedUserSupport;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.route.service.*;
import travel.route.dto.route.*;
import travel.common.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

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
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        route.setId(null);
        route.setUserId(userId);
        route.setViewCount(0);
        route.setLikeCount(0);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        log.info("创建路线请求: userId={}, title={}", userId, route.getTitle());
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
            Integer currentUserId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
            if (!Boolean.TRUE.equals(route.getIsPublic())
                    && !java.util.Objects.equals(route.getUserId(), currentUserId)) {
                throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
            }
            return Result.success("获取路线详情成功", route);
        }
        return Result.error("路线不存在");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新路线", description = "更新路线信息")
    public Result<Route> updateRoute(@PathVariable Integer id,
                                    @RequestBody Route route) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("更新路线请求: id={}, userId={}", id, userId);
        routeService.checkRouteOwner(id.longValue(), userId.longValue());
        Route existingRoute = routeService.getById(id);
        if (existingRoute == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        applyRouteChanges(existingRoute, route);
        existingRoute.setUpdatedAt(LocalDateTime.now());
        boolean result = routeService.updateById(existingRoute);
        if (result) {
            return Result.success("更新路线成功", existingRoute);
        }
        return Result.error("更新路线失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除路线", description = "删除路线")
    public Result<Boolean> deleteRoute(@PathVariable Integer id) {
        Long userId = AuthenticatedUserSupport.requireUserId();
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
    public Result<List<Route>> getMyRoutes() {
        Long userId = AuthenticatedUserSupport.requireUserId();
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
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        List<Route> routes = routeService.listByIds(routeIds).stream()
                .filter(route -> Boolean.TRUE.equals(route.getIsPublic())
                        || java.util.Objects.equals(route.getUserId(), currentUserId))
                .toList();
        return Result.success("批量获取路线成功", routes);
    }

    // ==================== 路线操作 ====================

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制路线", description = "复制路线创建副本")
    public Result<Route> copyRoute(@PathVariable Integer id) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("复制路线请求: id={}, userId={}", id, userId);
        Route originalRoute = routeService.getById(id);
        if (originalRoute == null) {
            return Result.error("原路线不存在");
        }
        if (!Boolean.TRUE.equals(originalRoute.getIsPublic())
                && !userId.equals(originalRoute.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }

        Route newRoute = new Route();
        newRoute.setTitle(originalRoute.getTitle() + " (副本)");
        newRoute.setUserId(userId);
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
                                              @RequestParam Boolean isPublic) {
        Long userId = AuthenticatedUserSupport.requireUserId();
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
    public Result<List<SmartRouteItem>> getSmartRoutes(
            @RequestParam String type,
            @RequestParam Integer cityId,
            @RequestParam Integer days,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String theme) {
        log.info("智能路线列表请求: type={}, cityId={}, days={}", type, cityId, days);
        List<SmartRouteItem> routes = switch (type) {
            case "seasonal" -> intelligentRouteService.getSeasonalRoutes(cityId, season != null ? season : "spring", days);
            case "theme" -> intelligentRouteService.getThemeRoutes(theme != null ? theme : "文化", cityId, days);
            default -> intelligentRouteService.getPopularRoutes(cityId, days, limit);
        };
        return Result.success("获取智能路线成功", routes);
    }

    @GetMapping("/smart/similar/{routeId}")
    @Operation(summary = "相似路线推荐", description = "获取相似路线")
    public Result<List<SmartRouteItem>> getSimilarRoutes(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "5") int limit) {
        requireReadableRoute(routeId);
        log.info("获取相似路线请求: routeId={}, limit={}", routeId, limit);
        List<SmartRouteItem> similarRoutes = intelligentRouteService.getSimilarRoutes(routeId, limit);
        return Result.success("获取相似路线成功", similarRoutes);
    }

    @PostMapping("/smart/recommend-by-preference")
    @Operation(summary = "基于偏好推荐", description = "基于用户偏好的智能路线推荐")
    public Result<List<UserPreferenceRecommendation>> recommendRoutesByUserPreference(@RequestParam Integer cityId,
                                                                            @RequestParam int days,
                                                                            @RequestBody RoutePreferenceRequest request) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("基于用户偏好推荐路线请求: userId={}, cityId={}, days={}", userId, cityId, days);
        java.util.Map<String, Object> preferences = new java.util.HashMap<>();
        if (request.getPreferredTypes() != null) preferences.put("preferredTypes", request.getPreferredTypes());
        if (request.getBudget() != null) preferences.put("budget", request.getBudget());
        if (request.getTransportPreference() != null) preferences.put("transportPreference", request.getTransportPreference());
        List<UserPreferenceRecommendation> recommendations = intelligentRouteService.recommendRoutesByUserPreference(userId, cityId, days, preferences);
        return Result.success(recommendations);
    }

    // ==================== 路线优化 ====================

    @PostMapping("/smart/optimize")
    @Operation(summary = "优化路线", description = "优化路线")
    public Result<String> optimizeRoute(@RequestParam Integer routeId) {
        routeService.checkRouteOwner(routeId.longValue(), AuthenticatedUserSupport.requireUserId());
        log.info("优化路线请求: routeId={}", routeId);
        String result = intelligentRouteService.optimizeRoute(routeId);
        return Result.success("优化路线成功", result);
    }

    // ==================== 路线评估 ====================

    @PostMapping("/smart/evaluate/{routeId}")
    @Operation(summary = "评估路线质量", description = "评估路线质量")
    public Result<RouteQualityEvaluation> evaluateRouteQuality(@PathVariable Integer routeId,
                                                            @Valid @RequestBody RouteQualityEvaluationRequest request) {
        requireReadableRoute(routeId);
        log.info("评估路线质量请求: routeId={}", routeId);
        RouteQualityEvaluation evaluation = intelligentRouteService.evaluateRouteQuality(routeId, request);
        return Result.success(evaluation);
    }

    // ==================== 路线对比 ====================

    @PostMapping("/smart/compare")
    @Operation(summary = "比较路线", description = "多维度比较路线")
    public Result<RouteComparisonResult> compareRoutes(@RequestParam List<Integer> routeIds) {
        if (routeIds == null || routeIds.size() < 2 || routeIds.size() > 10) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        routeIds.forEach(this::requireReadableRoute);
        log.info("比较路线请求: routeIds={}", routeIds);
        RouteComparisonResult comparison = intelligentRouteService.compareRoutes(routeIds);
        return Result.success(comparison);
    }

    // ==================== 实时调整 ====================

    @PostMapping("/smart/real-time-adjustment/{routeId}")
    @Operation(summary = "实时路线调整", description = "获取实时路线调整建议")
    public Result<RealTimeAdjustmentResult> getRealTimeAdjustment(@PathVariable Integer routeId,
                                                             @Valid @RequestBody RealTimeAdjustmentRequest request) {
        routeService.checkRouteOwner(routeId.longValue(), AuthenticatedUserSupport.requireUserId());
        log.info("获取实时路线调整建议请求: routeId={}", routeId);
        RealTimeAdjustmentResult adjustment = intelligentRouteService.getRealTimeAdjustment(routeId, request);
        return Result.success(adjustment);
    }

    // ==================== 个性化路线生成 ====================

    @PostMapping("/smart/generate-personalized")
    @Operation(summary = "生成个性化路线", description = "AI生成个性化路线")
    public Result<PersonalizedRouteResult> generatePersonalizedRoute(@Valid @RequestBody PersonalizedRouteRequest request) {
        log.info("生成个性化路线请求");
        PersonalizedRouteResult route = intelligentRouteService.generatePersonalizedRoute(
                request.getUserPreferences(), request.getConstraints());
        return Result.success(route);
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

    private Route requireReadableRoute(Integer routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        Integer currentUserId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        if (!Boolean.TRUE.equals(route.getIsPublic())
                && !java.util.Objects.equals(route.getUserId(), currentUserId)) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }
        return route;
    }

    private void applyRouteChanges(Route target, Route source) {
        if (source.getTitle() != null) target.setTitle(source.getTitle());
        if (source.getDescription() != null) target.setDescription(source.getDescription());
        if (source.getCityId() != null) target.setCityId(source.getCityId());
        if (source.getDurationDays() != null) target.setDurationDays(source.getDurationDays());
        if (source.getDifficulty() != null) target.setDifficulty(source.getDifficulty());
        if (source.getCoverImage() != null) target.setCoverImage(source.getCoverImage());
        if (source.getIsPublic() != null) target.setIsPublic(source.getIsPublic());
    }
}
