package travel.route.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.utils.CacheUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 路线缓存服务
 * 统一管理路线相关的缓存操作
 */
@Service
public class RouteCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(RouteCacheService.class);
    
    private final CacheUtil cacheUtil;
    
    public RouteCacheService(CacheUtil cacheUtil) {
        this.cacheUtil = cacheUtil;
    }
    
    /**
     * 获取路线详情缓存
     */
    public Route getRouteDetail(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "detail", id);
        Route cached = cacheUtil.get(cacheKey, Route.class);
        
        if (cached != null) {
            log.debug("从缓存获取路线详情: id={}", id);
            cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_hits"), 1);
            return cached;
        }
        
        cacheUtil.increment(CacheUtil.generateKey(CacheUtil.COUNTER_KEY_PREFIX, "cache_misses"), 1);
        return null;
    }
    
    /**
     * 缓存路线详情
     */
    public void cacheRouteDetail(Route route) {
        if (route == null || route.getId() == null) {
            return;
        }
        
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "detail", route.getId());
        cacheUtil.set(cacheKey, route, 1, TimeUnit.HOURS);
        log.debug("缓存路线详情: id={}", route.getId());
    }
    
    /**
     * 失效路线缓存
     */
    public void invalidateRouteCache(Integer routeId) {
        if (routeId == null) {
            return;
        }
        
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "detail", routeId);
        cacheUtil.delete(cacheKey);
        log.debug("失效路线缓存: id={}", routeId);
    }
    
    /**
     * 获取用户路线列表缓存
     */
    @SuppressWarnings("unchecked")
    public List<Route> getUserRoutes(Long userId) {
        if (userId == null) {
            return null;
        }
        
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "user", userId);
        return cacheUtil.get(cacheKey, List.class);
    }
    
    /**
     * 缓存用户路线列表
     */
    public void cacheUserRoutes(Long userId, List<Route> routes) {
        if (userId == null) {
            return;
        }
        
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "user", userId);
        cacheUtil.set(cacheKey, routes, 30, TimeUnit.MINUTES);
        log.debug("缓存用户路线列表: userId={}, count={}", userId, routes != null ? routes.size() : 0);
    }
    
    /**
     * 失效用户路线列表缓存
     */
    public void invalidateUserRoutes(Long userId) {
        if (userId == null) {
            return;
        }
        
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_KEY_PREFIX, "user", userId);
        cacheUtil.delete(cacheKey);
        log.debug("失效用户路线列表缓存: userId={}", userId);
    }
}
