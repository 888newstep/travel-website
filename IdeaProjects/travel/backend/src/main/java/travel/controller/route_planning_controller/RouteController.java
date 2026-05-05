package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Route;
import travel.service.route_planning.RouteService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RouteService routeService;

    @PostMapping
    public Result<Route> createRoute(@RequestBody Route route) {
        log.info("创建路线请求: userId={}, title={}", route.getUserId(), route.getTitle());
        boolean result = routeService.save(route);
        if (result) {
            return Result.success("创建路线成功", route);
        }
        return Result.error("创建路线失败");
    }

    @GetMapping("/{id}")
    public Result<Route> getRoute(@PathVariable Integer id) {
        log.info("获取路线详情请求: id={}", id);
        Route route = routeService.getById(id);
        if (route != null) {
            return Result.success("获取路线详情成功", route);
        }
        return Result.error("路线不存在");
    }

    @PutMapping("/{id}")
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

    @GetMapping("/my")
    public Result<List<Route>> getMyRoutes(@RequestParam Long userId) {
        log.info("获取我的路线请求: userId={}", userId);
        List<Route> routes = routeService.getMyRoutes(userId);
        return Result.success("获取我的路线成功", routes);
    }

    @GetMapping("/search")
    public Result<List<Route>> searchRoutes(@RequestParam String title) {
        log.info("搜索路线请求: title={}", title);
        List<Route> routes = routeService.searchRoutesByTitle(title);
        return Result.success("搜索路线成功", routes);
    }

    @GetMapping("/city/{cityId}")
    public Result<List<Route>> getRoutesByCity(@PathVariable Integer cityId) {
        log.info("根据城市获取路线请求: cityId={}", cityId);
        List<Route> routes = routeService.getByCityId(cityId);
        return Result.success("根据城市获取路线成功", routes);
    }

    @GetMapping("/count/{userId}")
    public Result<Integer> getUserRouteCount(@PathVariable Long userId) {
        log.info("获取用户路线数量请求: userId={}", userId);
        int count = routeService.getUserRouteCount(userId);
        return Result.success("获取用户路线数量成功", count);
    }

    @PostMapping("/batch")
    public Result<List<Route>> getBatchRoutes(@RequestBody List<Integer> routeIds) {
        log.info("批量获取路线请求: routeIds={}", routeIds);
        List<Route> routes = routeService.listByIds(routeIds);
        return Result.success("批量获取路线成功", routes);
    }

    @PostMapping("/{id}/copy")
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
}