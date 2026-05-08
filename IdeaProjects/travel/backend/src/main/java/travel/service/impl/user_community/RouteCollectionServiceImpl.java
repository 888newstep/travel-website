package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import travel.entity.route_planning.Route;
import travel.entity.user_community.RouteCollection;
import travel.entity.user_community.User;
import travel.entity.vo.RouteCollectionVO;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteCollectionServiceImpl extends ServiceImpl<RouteCollectionMapper, RouteCollection> implements RouteCollectionService {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionServiceImpl.class);

    private final RouteService routeService;
    @Lazy
    private final UserService userService;
    private final CacheUtil cacheUtil;

    @Override
    public boolean collectRoute(Integer routeId, Integer userId) {
        try {
            RouteCollection collection = createCollection(routeId, userId, false, null);
            return collection != null;
        } catch (Exception e) {
            log.error("收藏路线失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean uncollectRoute(Integer routeId, Integer userId) {
        return cancelCollect(routeId, userId);
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
                .eq(RouteCollection::getUserId, userId);

        boolean result = count(queryWrapper) > 0;

        cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);

        return result;
    }

    @Override
    public List<RouteCollectionVO> getUserCollections(Integer userId, int page, int size) {
        if (userId == null || userId <= 0 || page <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId, "page", page, "size", size);
        List<?> cachedList = cacheUtil.get(cacheKey, List.class);
        if (cachedList != null && !cachedList.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<RouteCollectionVO> cachedVOs = (List<RouteCollectionVO>) cachedList;
            return cachedVOs;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getUserId, userId)
                .orderByDesc(RouteCollection::getCollectionTime);

        IPage<RouteCollection> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteCollection> collections = pageResult.getRecords();

        List<RouteCollectionVO> voList = convertToVOList(collections);

        cacheUtil.set(cacheKey, voList, 30, TimeUnit.MINUTES);

        return voList;
    }

    @Override
    public long countByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getUserId, userId);
        return count(queryWrapper);
    }

    @Override
    public RouteCollection createCollection(Integer routeId, Integer userId, Boolean isPublic, String notes) {
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
        routeCollection.setRouteId(routeId);
        routeCollection.setUserId(userId);
        routeCollection.setCollectionTime(LocalDateTime.now());
        routeCollection.setIsPublic(isPublic != null && isPublic);
        routeCollection.setNotes(notes);

        save(routeCollection);

        invalidateUserCache(userId);
        invalidateRouteCountCache(routeId);

        log.info("收藏路线成功: routeId={}, userId={}", routeId, userId);
        return routeCollection;
    }

    @Override
    public boolean cancelCollect(Integer routeId, Integer userId) {
        if (routeId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getRouteId, routeId)
                .eq(RouteCollection::getUserId, userId);

        if (count(queryWrapper) == 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "还未收藏该路线");
        }

        boolean result = remove(queryWrapper);

        if (result) {
            invalidateUserCache(userId);
            invalidateRouteCountCache(routeId);
            invalidateCollectedCache(routeId, userId);
            log.info("取消收藏路线成功: routeId={}, userId={}", routeId, userId);
        }

        return result;
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
        queryWrapper.eq(RouteCollection::getRouteId, routeId);

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
        List<?> cachedList = cacheUtil.get(cacheKey, List.class);
        if (cachedList != null && !cachedList.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<RouteCollection> cachedCollections = (List<RouteCollection>) cachedList;
            return cachedCollections;
        }

        LambdaQueryWrapper<RouteCollection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteCollection::getIsPublic, true)
                .orderByDesc(RouteCollection::getCollectionTime);

        IPage<RouteCollection> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteCollection> collections = pageResult.getRecords();

        cacheUtil.set(cacheKey, collections, 30, TimeUnit.MINUTES);

        return collections;
    }

    private List<RouteCollectionVO> convertToVOList(List<RouteCollection> collections) {
        return collections.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private RouteCollectionVO convertToVO(RouteCollection collection) {
        RouteCollectionVO vo = new RouteCollectionVO();
        vo.setId(collection.getId());
        vo.setRouteId(collection.getRouteId());
        vo.setUserId(collection.getUserId());
        vo.setCollectionTime(collection.getCollectionTime());
        vo.setIsPublic(collection.getIsPublic());
        vo.setNotes(collection.getNotes());

        if (collection.getRouteId() != null) {
            Route route = routeService.getById(collection.getRouteId().longValue());
            if (route != null) {
                vo.setRouteTitle(route.getTitle());
                vo.setRouteCoverImage(route.getCoverImage());
                vo.setRouteDurationDays(route.getDurationDays());
                vo.setRouteDifficulty(route.getDifficulty());
            }
        }

        return vo;
    }

    private void invalidateUserCache(Integer userId) {
        String userCollectionsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "user", userId);
        cacheUtil.delete(userCollectionsCacheKey);
    }

    private void invalidateRouteCountCache(Integer routeId) {
        String routeCollectionCountCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "count", routeId);
        cacheUtil.delete(routeCollectionCountCacheKey);
    }

    private void invalidateCollectedCache(Integer routeId, Integer userId) {
        String collectedCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COLLECTION_KEY_PREFIX, "collected", routeId, userId);
        cacheUtil.delete(collectedCacheKey);
    }
}
