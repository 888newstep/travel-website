package travel.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import travel.common.entity.route_planning.Route;
import travel.common.entity.user_community.RouteCollection;
import travel.common.entity.user_community.User;
import travel.common.vo.RouteCollectionVO;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteCollectionMapper;
import travel.common.performance.PerformanceStageRecorder;
import travel.collection.service.RouteCollectionService;
import travel.collection.service.RouteService;
import travel.collection.service.UserService;
import travel.common.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.service.DistributedLockService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RouteCollectionServiceImpl extends ServiceImpl<RouteCollectionMapper, RouteCollection> implements RouteCollectionService {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionServiceImpl.class);
    private static final String ROUTE_ITEM_TYPE = "route";
    private static final String COLLECT_ACTION = "collect";

    private final RouteService routeService;
    private final DistributedLockService distributedLockService;
    @Lazy
    private final UserService userService;
    private final CacheUtil cacheUtil;
    private final PerformanceStageRecorder performanceStageRecorder;

    public RouteCollectionServiceImpl(
            RouteService routeService,
            DistributedLockService distributedLockService,
            @Lazy UserService userService,
            CacheUtil cacheUtil,
            PerformanceStageRecorder performanceStageRecorder) {
        this.routeService = routeService;
        this.distributedLockService = distributedLockService;
        this.userService = userService;
        this.cacheUtil = cacheUtil;
        this.performanceStageRecorder = performanceStageRecorder;
    }

    @Override
    public boolean toggleCollection(Integer routeId, Integer userId) {
        validateCollectionTarget(routeId, userId);
        long startedAtNanos = performanceStageRecorder.start();
        String outcome = "error";
        try {
            boolean collected = distributedLockService.executeWithLock(collectionLockKey(routeId, userId), () -> {
                RouteCollection existing = findRouteCollection(routeId, userId);
                if (existing != null) {
                    removeCollection(existing);
                    return false;
                }
                createCollectionRecord(routeId, userId, false, null);
                return true;
            });
            outcome = collected ? "collected" : "uncollected";
            return collected;
        } finally {
            performanceStageRecorder.record("collection.toggle-locked", startedAtNanos, outcome);
        }
    }

    @Override
    public boolean collectRoute(Integer routeId, Integer userId) {
        validateCollectionTarget(routeId, userId);
        return distributedLockService.executeWithLock(collectionLockKey(routeId, userId), () -> {
            if (findRouteCollection(routeId, userId) == null) {
                createCollectionRecord(routeId, userId, false, null);
            }
            return true;
        });
    }

    @Override
    public boolean uncollectRoute(Integer routeId, Integer userId) {
        validateCollectionTarget(routeId, userId);
        return distributedLockService.executeWithLock(collectionLockKey(routeId, userId), () -> {
            RouteCollection existing = findRouteCollection(routeId, userId);
            if (existing != null) {
                removeCollection(existing);
            }
            return true;
        });
    }

    private void invalidateCollectedCache(Integer routeId, Integer userId) {
        String collectedCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", routeId, userId);
        cacheUtil.delete(collectedCacheKey);
    }

    @Override
    public List<String> getUserCollectionCategories(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        log.info("获取用户收藏分类: userId={}", userId);

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION)
                .select(RouteCollection::getCategory)
                .groupBy(RouteCollection::getCategory);

        List<RouteCollection> collections = list(queryWrapper);
        List<String> categories = collections.stream()
                .map(RouteCollection::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (categories.isEmpty()) {
            categories.add("默认分类");
        }

        log.info("用户收藏分类: userId={}, categories={}", userId, categories);
        return categories;
    }

    @Override
    public int batchRemoveCollections(List<Integer> ids, Integer userId) {
        if (ids == null || ids.isEmpty() || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        log.info("批量删除收藏: count={}", ids.size());

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(RouteCollection::getId, ids)
                .eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION);

        List<RouteCollection> collections = list(queryWrapper);

        boolean result = remove(queryWrapper);

        if (result) {
            for (RouteCollection collection : collections) {
                invalidateUserCache(collection.getUserId());
                invalidateRouteCountCache(collection.getRouteId());
                invalidateCollectedCache(collection.getRouteId(), collection.getUserId());
            }
            if (collections.stream().anyMatch(collection -> Boolean.TRUE.equals(collection.getIsPublic()))) {
                invalidatePublicCache();
            }
            log.info("批量删除收藏成功: count={}", ids.size());
        }

        return result ? collections.size() : 0;
    }



    @Override
    public boolean isCollected(Integer routeId, Integer userId) {
        if (routeId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", routeId, userId);
        Boolean cached = cacheUtil.get(cacheKey, Boolean.class);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getRouteId, routeId)
                .eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION);

        boolean result = count(queryWrapper) > 0;

        cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);

        return result;
    }

    @Override
    public List<RouteCollectionVO> getUserCollections(Integer userId, int page, int size) {
        if (userId == null || userId <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        page = Math.max(page, 1);

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId, "page", page, "size", size);
        List<?> cachedList = cacheUtil.get(cacheKey, List.class);
        if (cachedList != null) {
            @SuppressWarnings("unchecked")
            List<RouteCollectionVO> cachedVOs = (List<RouteCollectionVO>) cachedList;
            return cachedVOs;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION)
                .orderByDesc(RouteCollection::getCollectionTime);

        // 收藏列表只返回当前页，不需要执行额外的 COUNT(*)。
        IPage<RouteCollection> pageResult = page(new Page<>(page, size, false), queryWrapper);
        List<RouteCollection> collections = pageResult.getRecords();

        List<RouteCollectionVO> voList = convertToVOList(collections);

        cacheUtil.set(cacheKey, voList, 30, TimeUnit.MINUTES);

        return voList;
    }

    @Override
    public List<RouteCollectionVO> getUserCollectionsByCategory(Integer userId, String category, int page, int size) {
        if (userId == null || userId <= 0 || category == null || category.isBlank() || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        page = Math.max(page, 1);

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION)
                .eq(RouteCollection::getCategory, category.trim())
                .orderByDesc(RouteCollection::getCollectionTime);

        IPage<RouteCollection> pageResult = page(new Page<>(page, size, false), queryWrapper);
        return convertToVOList(pageResult.getRecords());
    }

    @Override
    public long countByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getUserId, userId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION);
        return count(queryWrapper);
    }

    @Override
    public RouteCollection createCollection(Integer routeId, Integer userId, Boolean isPublic, String notes) {
        validateCollectionTarget(routeId, userId);
        return distributedLockService.executeWithLock(collectionLockKey(routeId, userId), () -> {
            RouteCollection existing = findRouteCollection(routeId, userId);
            return existing != null
                    ? existing
                    : createCollectionRecord(routeId, userId, isPublic, notes);
        });
    }

    @Override
    public boolean cancelCollect(Integer routeId, Integer userId) {
        return uncollectRoute(routeId, userId);
    }

    @Override
    public int getRouteCollectionCount(Integer routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "count", routeId);
        Integer cached = cacheUtil.get(cacheKey, Integer.class);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getRouteId, routeId)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION);

        int count = Math.toIntExact(count(queryWrapper));

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

        if (!routeCollection.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }

        routeCollection.setNotes(notes);
        boolean result = updateById(routeCollection);

        if (result) {
            invalidateUserCache(userId);
            if (Boolean.TRUE.equals(routeCollection.getIsPublic())) {
                invalidatePublicCache();
            }
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

        if (!routeCollection.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }

        routeCollection.setIsPublic(isPublic);
        boolean result = updateById(routeCollection);

        if (result) {
            invalidateUserCache(userId);
            invalidatePublicCache();
            log.info("更新收藏公开状态成功: collectionId={}, userId={}, isPublic={}", collectionId, userId, isPublic);
        }

        return result;
    }

    @Override
    public List<RouteCollection> getPublicCollections(int page, int size) {
        if (size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        page = Math.max(page, 1);

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "public", "page", page, "size", size);
        List<?> cachedList = cacheUtil.get(cacheKey, List.class);
        if (cachedList != null) {
            @SuppressWarnings("unchecked")
            List<RouteCollection> cachedCollections = (List<RouteCollection>) cachedList;
            return cachedCollections;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getIsPublic, true)
                .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                .eq(RouteCollection::getCollectionType, COLLECT_ACTION)
                .orderByDesc(RouteCollection::getCollectionTime);

        IPage<RouteCollection> pageResult = page(new Page<>(page, size, false), queryWrapper);
        List<RouteCollection> collections = pageResult.getRecords();

        cacheUtil.set(cacheKey, collections, 30, TimeUnit.MINUTES);

        return collections;
    }

    private List<RouteCollectionVO> convertToVOList(List<RouteCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return List.of();
        }

        List<Integer> routeIds = collections.stream()
                .filter(Objects::nonNull)
                .map(RouteCollection::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Route> routesById = Collections.emptyMap();
        if (!routeIds.isEmpty()) {
            try {
                routesById = routeService.getByIds(routeIds).stream()
                        .filter(Objects::nonNull)
                        .filter(route -> route.getId() != null)
                        .collect(Collectors.toMap(
                                Route::getId,
                                Function.identity(),
                                (first, ignored) -> first));
            } catch (Exception e) {
                // 路线详情失败不影响收藏基础记录返回，避免跨服务抖动放大为列表整体失败。
                log.warn("批量加载路线详情失败，降级返回基础收藏数据: routeIds={}, error={}",
                        routeIds, e.getMessage());
            }
        }

        Map<Integer, Route> resolvedRoutes = routesById;
        return collections.stream()
                .filter(Objects::nonNull)
                .map(collection -> convertToVO(collection, resolvedRoutes.get(collection.getRouteId())))
                .collect(Collectors.toList());
    }

    private RouteCollectionVO convertToVO(RouteCollection collection, Route route) {
        RouteCollectionVO vo = new RouteCollectionVO();
        vo.setId(collection.getId());
        vo.setRouteId(collection.getRouteId());
        vo.setUserId(collection.getUserId());
        vo.setCollectionTime(collection.getCollectionTime());
        vo.setIsPublic(collection.getIsPublic());
        vo.setNotes(collection.getNotes());

        if (route != null) {
            vo.setRouteTitle(route.getTitle());
            vo.setRouteCoverImage(route.getCoverImage());
            vo.setRouteDurationDays(route.getDurationDays());
            vo.setRouteDifficulty(route.getDifficulty());
        }

        return vo;
    }

    private void invalidateUserCache(Integer userId) {
        String userCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId);
        cacheUtil.delete(userCollectionsCacheKey);
        // 用户收藏发生变化时，按页缓存也必须失效，否则新增/删除收藏会被旧分页结果遮蔽。
        cacheUtil.deleteByPattern(userCollectionsCacheKey + ":page:*:size:*");
    }

    private void invalidateRouteCountCache(Integer routeId) {
        String routeCollectionCountCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "count", routeId);
        cacheUtil.delete(routeCollectionCountCacheKey);
    }

    private void invalidatePublicCache() {
        String publicCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "public");
        cacheUtil.deleteByPattern(publicCacheKey + ":page:*:size:*");
    }

    private String collectionLockKey(Integer routeId, Integer userId) {
        return "collection:" + userId + ":" + routeId;
    }

    private void validateCollectionTarget(Integer routeId, Integer userId) {
        if (routeId == null || routeId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private RouteCollection findRouteCollection(Integer routeId, Integer userId) {
        long startedAtNanos = performanceStageRecorder.start();
        String outcome = "error";
        try {
            LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RouteCollection::getRouteId, routeId)
                    .eq(RouteCollection::getUserId, userId)
                    .eq(RouteCollection::getItemType, ROUTE_ITEM_TYPE)
                    .eq(RouteCollection::getCollectionType, COLLECT_ACTION);
            RouteCollection collection = getOne(queryWrapper, false);
            outcome = collection == null ? "missing" : "found";
            return collection;
        } finally {
            performanceStageRecorder.record("collection.lookup", startedAtNanos, outcome);
        }
    }

    private RouteCollection createCollectionRecord(
            Integer routeId,
            Integer userId,
            Boolean isPublic,
            String notes) {
        Route route = routeService.getById(routeId.longValue());
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        User user = userService.getById(userId.longValue());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        RouteCollection routeCollection = new RouteCollection();
        routeCollection.setRouteId(routeId);
        routeCollection.setUserId(userId);
        routeCollection.setItemType(ROUTE_ITEM_TYPE);
        routeCollection.setCollectionType(COLLECT_ACTION);
        routeCollection.setCollectionTime(LocalDateTime.now());
        routeCollection.setIsPublic(Boolean.TRUE.equals(isPublic));
        routeCollection.setNotes(notes);

        long insertStartedAtNanos = performanceStageRecorder.start();
        String insertOutcome = "error";
        try {
            if (!save(routeCollection)) {
                insertOutcome = "failed";
                throw new BusinessException(ErrorCodeEnum.COLLECTION_CREATE_FAILED);
            }
            insertOutcome = "success";
        } finally {
            performanceStageRecorder.record("collection.db-insert", insertStartedAtNanos, insertOutcome);
        }

        invalidateCollectionCaches(routeCollection);
        log.info("收藏路线成功: routeId={}, userId={}", routeId, userId);
        return routeCollection;
    }

    private void removeCollection(RouteCollection collection) {
        if (!removeById(collection.getId())) {
            throw new BusinessException(ErrorCodeEnum.COLLECTION_DELETE_FAILED);
        }
        invalidateCollectionCaches(collection);
        log.info("取消收藏路线成功: routeId={}, userId={}", collection.getRouteId(), collection.getUserId());
    }

    private void invalidateCollectionCaches(RouteCollection collection) {
        long startedAtNanos = performanceStageRecorder.start();
        String outcome = "error";
        try {
            invalidateUserCache(collection.getUserId());
            invalidateRouteCountCache(collection.getRouteId());
            invalidateCollectedCache(collection.getRouteId(), collection.getUserId());
            if (Boolean.TRUE.equals(collection.getIsPublic())) {
                invalidatePublicCache();
            }
            outcome = "success";
        } finally {
            performanceStageRecorder.record("collection.cache-invalidation", startedAtNanos, outcome);
        }
    }

}
