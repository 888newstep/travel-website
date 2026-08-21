package travel.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import travel.common.entity.route_planning.Route;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.repository.RouteRepository;
import travel.route.service.RouteService;
import travel.route.service.RouteCacheService;
import travel.route.service.RouteOverviewStatisticsService;
import travel.route.service.RouteRealtimeStatusService;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class RouteServiceImpl extends ServiceImpl<RouteMapper, Route> implements RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteServiceImpl.class);

    private final RouteRepository routeRepository;
    private final CacheUtil cacheUtil;
    private final RouteCacheService routeCacheService;
    private final RouteOverviewStatisticsService routeOverviewStatisticsService;
    private final RouteRealtimeStatusService routeRealtimeStatusService;

    @Override
    public Route getById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }

        // 尝试从缓存获取
        Route cached = routeCacheService.getRouteDetail(id);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，从数据库查询
        Route route = baseMapper.selectById(id);
        if (route != null) {
            routeCacheService.cacheRouteDetail(route);
        }
        return route;
    }

    @Override
    public List<Route> getMyRoutes(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        List<Route> cached = routeCacheService.getUserRoutes(userId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 缓存未命中，从数据库查询
        List<Route> routes = routeRepository.findByUserId(userId);
        routeCacheService.cacheUserRoutes(userId, routes);
        return routes;
    }

    @Override
    public void checkRouteOwner(Long routeId, Long userId) {
        if (routeId == null || routeId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        if (route.getUserId() == null || route.getUserId().longValue() != userId.longValue()) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }
    }

    @Override
    public List<Route> searchRoutesByTitle(String title) {
        if (title == null || title.isBlank()) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "search", title);
        List<Route> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取路线搜索: title={}", title);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("title", title)
                .eq("is_public", true)
                .orderByDesc("view_count");
        List<Route> routes = routeRepository.findByCondition(queryWrapper);

        cacheUtil.set(cacheKey, routes, 30, TimeUnit.MINUTES);
        log.debug("缓存路线搜索: title={}, count={}", title, routes.size());
        return routes;
    }

    @Override
    public int getUserRouteCount(Long userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }
        List<Route> routes = routeRepository.findByUserId(userId);
        return routes.size();
    }

    @Override
    public List<Route> getByCityId(Integer cityId) {
        if (cityId == null || cityId <= 0) {
            return List.of();
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "city", cityId);
        List<Route> cached = cacheUtil.get(cacheKey, List.class);

        if (cached != null && !cached.isEmpty()) {
            log.debug("从缓存获取城市路线: cityId={}", cityId);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }

        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city_id", cityId)
                .eq("is_public", true)
                .orderByDesc("view_count");
        List<Route> routes = routeRepository.findByCondition(queryWrapper);

        cacheUtil.set(cacheKey, routes, 1, TimeUnit.HOURS);
        log.debug("缓存城市路线: cityId={}, count={}", cityId, routes.size());
        return routes;
    }

    @Override
    public boolean save(Route route) {
        boolean result = routeRepository.save(route) != null;
        if (result) {
            invalidateRouteCache(route.getUserId().longValue(), route.getCityId());
        }
        return result;
    }

    @Override
    public boolean updateById(Route route) {
        boolean result = routeRepository.update(route);
        if (result) {
            invalidateRouteCache(route.getUserId().longValue(), route.getCityId());
            routeCacheService.invalidateRouteCache(route.getId());
        }
        return result;
    }

    @Override
    public boolean removeById(Integer id) {
        Route route = getById(id);
        boolean result = routeRepository.deleteById(id.longValue());
        if (result && route != null) {
            invalidateRouteCache(route.getUserId().longValue(), route.getCityId());
            routeCacheService.invalidateRouteCache(id);
        }
        return result;
    }

    private void invalidateRouteCache(Long userId, Integer cityId) {
        // 使用 RouteCacheService 失效用户路线缓存
        if (userId != null) {
            routeCacheService.invalidateUserRoutes(userId);
        }
        // 其他缓存操作保持原样
        if (cityId != null) {
            cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "city", cityId));
        }
        cacheUtil.delete(CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "all"));
        cacheUtil.deleteByPattern(CacheUtil.ROUTE_KEY_PREFIX + ":search:*");
        log.info("路线缓存已失效: userId={}, cityId={}", userId, cityId);
    }

    @Override
    public List<Route> listByIds(List<Integer> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<Route> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", routeIds);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Object> getRouteStatistics() {
        return routeOverviewStatisticsService.getRouteStatistics();
    }

    @Override
    public List<Map<String, Object>> getRouteStatisticsByCity() {
        return routeOverviewStatisticsService.getRouteStatisticsByCity();
    }

    @Override
    public Map<String, Object> getRouteCompletionRate() {
        return routeOverviewStatisticsService.getRouteCompletionRate();
    }

    @Override
    public List<Map<String, Object>> getRouteDurationDistribution() {
        return routeOverviewStatisticsService.getRouteDurationDistribution();
    }

    @Override
    public List<Map<String, Object>> getRoutesNeedingSync(Integer minutes) {
        return routeRealtimeStatusService.getRoutesNeedingSync(minutes);
    }

    @Override
    public Map<String, Object> syncRouteStatus(List<Integer> routeIds) {
        return routeRealtimeStatusService.syncRouteStatus(routeIds);
    }

    @Override
    public Map<String, Object> getRouteRealtimeStatus(Integer routeId) {
        return routeRealtimeStatusService.getRouteRealtimeStatus(routeId);
    }

    @Override
    public Map<String, Object> updateRouteRealtimeStatus(Integer routeId, Map<String, Object> params) {
        return routeRealtimeStatusService.updateRouteRealtimeStatus(routeId, params);
    }

}
