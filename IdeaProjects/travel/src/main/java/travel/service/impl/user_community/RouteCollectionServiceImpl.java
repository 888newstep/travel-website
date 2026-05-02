package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import travel.entity.route_planning.Route;
import travel.entity.user_community.RouteCollection;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.route_planning_mapper.RouteCollectionMapper;
import travel.service.user_community.RouteCollectionService;
import travel.service.route_planning.RouteService;
import travel.service.user_community.UserService;
import travel.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RouteCollectionServiceImpl extends ServiceImpl<RouteCollectionMapper, RouteCollection> implements RouteCollectionService {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionServiceImpl.class);

    private final RouteCollectionMapper routeCollectionMapper;
    private final RouteService routeService;
    private final UserService userService;
    private final CacheUtil cacheUtil;

    public RouteCollectionServiceImpl(RouteCollectionMapper routeCollectionMapper, RouteService routeService, UserService userService, CacheUtil cacheUtil) {
        this.routeCollectionMapper = routeCollectionMapper;
        this.routeService = routeService;
        this.userService = userService;
        this.cacheUtil = cacheUtil;
    }

    @Override
    public RouteCollection collectRoute(Integer routeId, Integer userId, Boolean isPublic, String notes) {
        if (routeId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        Route route = routeService.getById(routeId.longValue());
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        User user = userService.getById(userId.longValue());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        if (isCollected(routeId, userId)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "已经收藏过该路线");
        }

        RouteCollection routeCollection = new RouteCollection();
        routeCollection.setRoute(route);
        routeCollection.setUser(user);
        routeCollection.setIsPublic(isPublic != null && isPublic);
        routeCollection.setNotes(notes);

        save(routeCollection);

        String userCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId);
        cacheUtil.delete(userCollectionsCacheKey);
        String routeCollectionCountCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "count", routeId);
        cacheUtil.delete(routeCollectionCountCacheKey);

        log.info("收藏路线成功: routeId={}, userId={}", routeId, userId);
        return routeCollection;
    }

    @Override
    public boolean cancelCollect(Integer routeId, Integer userId) {
        if (routeId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        if (!isCollected(routeId, userId)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "还未收藏该路线");
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(RouteCollection::getCollectionTime);

        boolean result = remove(queryWrapper);

        if (result) {
            String userCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId);
            cacheUtil.delete(userCollectionsCacheKey);
            String routeCollectionCountCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "count", routeId);
            cacheUtil.delete(routeCollectionCountCacheKey);
            String collectedCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", routeId, userId);
            cacheUtil.delete(collectedCacheKey);
            log.info("取消收藏路线成功: routeId={}, userId={}", routeId, userId);
        }

        return result;
    }

    @Override
    public List<RouteCollection> getUserCollections(Integer userId, int page, int size) {
        if (userId == null || userId <= 0 || page <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId, "page", page, "size", size);
        Object cachedObj = cacheUtil.get(cacheKey, List.class);
        if (cachedObj instanceof List) {
            List<?> cachedList = (List<?>) cachedObj;
            List<RouteCollection> cachedCollections = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof RouteCollection) {
                    cachedCollections.add((RouteCollection) item);
                }
            }
            if (!cachedCollections.isEmpty()) {
                return cachedCollections;
            }
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(RouteCollection::getCollectionTime);

        IPage<RouteCollection> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteCollection> collections = pageResult.getRecords();

        cacheUtil.set(cacheKey, collections, 30, TimeUnit.MINUTES);

        return collections;
    }

    @Override
    public boolean isCollected(Integer routeId, Integer userId) {
        if (routeId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", routeId, userId);
        Object cachedObj = cacheUtil.get(cacheKey, Boolean.class);
        if (cachedObj instanceof Boolean) {
            return (Boolean) cachedObj;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(RouteCollection::getCollectionTime);

        boolean result = count(queryWrapper) > 0;

        cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);

        return result;
    }

    @Override
    public int getRouteCollectionCount(Integer routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "count", routeId);
        Object cachedObj = cacheUtil.get(cacheKey, Integer.class);
        if (cachedObj instanceof Integer) {
            return (Integer) cachedObj;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();

        int count = Math.toIntExact(routeCollectionMapper.selectCount(queryWrapper));

        cacheUtil.set(cacheKey, count, 24, TimeUnit.HOURS);

        return count;
    }

    @Override
    public boolean updateCollectionNotes(Integer collectionId, Integer userId, String notes) {
        if (collectionId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteCollection routeCollection = getById(collectionId);
        if (routeCollection == null) {
            throw new BusinessException(ErrorCodeEnum.COLLECTION_NOT_EXIST);
        }

        if (routeCollection.getUser() == null || !routeCollection.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_COLLECTION_PERMISSION);
        }

        routeCollection.setNotes(notes);
        boolean result = updateById(routeCollection);

        if (result) {
            String userCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId);
            cacheUtil.delete(userCollectionsCacheKey);
            log.info("更新收藏备注成功: collectionId={}, userId={}", collectionId, userId);
        }

        return result;
    }

    @Override
    public boolean updateCollectionPublicStatus(Integer collectionId, Integer userId, Boolean isPublic) {
        if (collectionId == null || userId == null || isPublic == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteCollection routeCollection = getById(collectionId);
        if (routeCollection == null) {
            throw new BusinessException(ErrorCodeEnum.COLLECTION_NOT_EXIST);
        }

        if (routeCollection.getUser() == null || !routeCollection.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_COLLECTION_PERMISSION);
        }

        routeCollection.setIsPublic(isPublic);
        boolean result = updateById(routeCollection);

        if (result) {
            String userCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId);
            cacheUtil.delete(userCollectionsCacheKey);
            String publicCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "public");
            cacheUtil.delete(publicCollectionsCacheKey);
            log.info("更新收藏公开状态成功: collectionId={}, userId={}, isPublic={}", collectionId, userId, isPublic);
        }

        return result;
    }

    @Override
    public List<RouteCollection> getPublicCollections(int page, int size) {
        if (page <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "public", "page", page, "size", size);
        Object cachedObj = cacheUtil.get(cacheKey, List.class);
        if (cachedObj instanceof List) {
            List<?> cachedList = (List<?>) cachedObj;
            List<RouteCollection> cachedCollections = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof RouteCollection) {
                    cachedCollections.add((RouteCollection) item);
                }
            }
            if (!cachedCollections.isEmpty()) {
                return cachedCollections;
            }
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getIsPublic, true)
                .orderByDesc(RouteCollection::getCollectionTime);

        IPage<RouteCollection> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteCollection> collections = pageResult.getRecords();

        cacheUtil.set(cacheKey, collections, 30, TimeUnit.MINUTES);

        return collections;
    }

    @Override
    public RouteCollection addCollection(RouteCollection collection) {
        try {
            collection.setCollectionTime(LocalDateTime.now());
            save(collection);
            log.info("添加路线收藏成功: userId={}, routeId={}", collection.getUserId(), collection.getRouteId());
            return collection;
        } catch (Exception e) {
            log.error("添加路线收藏失败: error={}", e.getMessage());
            throw new RuntimeException("添加收藏失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeCollection(Integer userId, Integer routeId) {
        try {
            LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
            boolean result = remove(queryWrapper);
            log.info("取消路线收藏成功: userId={}, routeId={}", userId, routeId);
            return result;
        } catch (Exception e) {
            log.error("取消路线收藏失败: error={}", e.getMessage());
            throw new RuntimeException("取消收藏失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkCollected(Integer userId, Integer routeId) {
        try {
            LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
            boolean result = count(queryWrapper) > 0;
            log.info("检查路线收藏状态: userId={}, routeId={}, collected={}", userId, routeId, result);
            return result;
        } catch (Exception e) {
            log.error("检查路线收藏状态失败: error={}", e.getMessage());
            throw new RuntimeException("检查收藏状态失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> getCollectionCategories(Integer userId) {
        try {
            List<String> categories = new ArrayList<>();
            log.info("获取收藏分类列表成功: userId={}", userId);
            return categories;
        } catch (Exception e) {
            log.error("获取收藏分类列表失败: error={}", e.getMessage());
            throw new RuntimeException("获取分类失败: " + e.getMessage());
        }
    }

    @Override
    public List<RouteCollection> getCollectionsByCategory(Integer userId, String category, int page, int size) {
        try {
            LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
            IPage<RouteCollection> pageResult = page(new Page<>(page, size), queryWrapper);
            List<RouteCollection> collections = pageResult.getRecords();
            log.info("按分类查询收藏成功: userId={}, category={}, count={}", userId, category, collections.size());
            return collections;
        } catch (Exception e) {
            log.error("按分类查询收藏失败: error={}", e.getMessage());
            throw new RuntimeException("查询失败: " + e.getMessage());
        }
    }

    @Override
    public int batchRemoveCollections(List<Long> ids) {
        try {
            int count = 0;
            for (Long id : ids) {
                if (removeById(id.intValue())) {
                    count++;
                }
            }
            log.info("批量取消收藏成功: count={}", count);
            return count;
        } catch (Exception e) {
            log.error("批量取消收藏失败: error={}", e.getMessage());
            throw new RuntimeException("批量取消失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateCollectionNote(Long id, String note) {
        try {
            RouteCollection collection = getById(id.intValue());
            if (collection == null) {
                throw new RuntimeException("收藏不存在");
            }
            collection.setNotes(note);
            boolean result = updateById(collection);
            log.info("更新收藏备注成功: id={}", id);
            return result;
        } catch (Exception e) {
            log.error("更新收藏备注失败: id={}, error={}", id, e.getMessage());
            throw new RuntimeException("更新备注失败: " + e.getMessage());
        }
    }
}