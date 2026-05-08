package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import travel.entity.route_planning.Route;
import travel.entity.user_community.RouteShare;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.route_planning_mapper.RouteShareMapper;
import travel.service.route_planning.RouteService;
import travel.service.user_community.RouteShareService;
import travel.service.user_community.UserService;
import travel.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.baomidou.mybatisplus.extension.toolkit.Db.count;

/**
 * 路线分享服务实现类
 */
@Service
@RequiredArgsConstructor
public class RouteShareServiceImpl extends ServiceImpl<RouteShareMapper, RouteShare> implements RouteShareService {

    private static final Logger log = LoggerFactory.getLogger(RouteShareServiceImpl.class);

    private final RouteService routeService;
    private final UserService userService;
    private final CacheUtil cacheUtil;

    private static final String SHARE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHARE_CODE_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public RouteShare createShare(Integer routeId, Integer userId, String shareTitle, String shareDescription, Integer expireDays) {
        // 1. 参数校验
        if (routeId == null || userId == null || shareTitle == null || shareTitle.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 2. 校验路线是否存在
        Route route = routeService.getById(routeId.longValue());
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        // 3. 校验用户是否存在
        User user = userService.getById(userId.longValue());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 4. 生成唯一分享码
        String shareCode = generateUniqueShareCode();

        // 5. 创建分享记录
        RouteShare routeShare = new RouteShare();
        routeShare.setRouteId(routeId);
        routeShare.setUserId(userId);
        routeShare.setShareCode(shareCode);
        routeShare.setShareTitle(shareTitle);
        routeShare.setShareDescription(shareDescription != null ? shareDescription : route.getDescription());
        
        // 6. 设置过期时间
        if (expireDays != null && expireDays > 0) {
            routeShare.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        }

        // 7. 保存到数据库
        save(routeShare);

        log.info("创建路线分享成功: routeId={}, userId={}, shareCode={}", routeId, userId, shareCode);
        return routeShare;
    }

    @Override
    public List<Map<String, Object>> getRouteShares(Integer routeId, int page, int size) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        log.info("获取路线分享列表: routeId={}, page={}, size={}", routeId, page, size);

        // 从数据库获取
        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteShare::getRouteId, routeId)
                .eq(RouteShare::getIsActive, true)
                .orderByDesc(RouteShare::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RouteShare> pageResult =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RouteShare> result = page(pageResult, queryWrapper);

        // 转换为 Map 格式返回
        List<Map<String, Object>> shareList = new ArrayList<>();
        for (RouteShare share : result.getRecords()) {
            Map<String, Object> shareMap = new HashMap<>();
            shareMap.put("id", share.getId());
            shareMap.put("routeId", share.getRouteId());
            shareMap.put("userId", share.getUserId());
            shareMap.put("shareCode", share.getShareCode());
            shareMap.put("shareTitle", share.getShareTitle());
            shareMap.put("shareDescription", share.getShareDescription());
            shareMap.put("shareCount", share.getShareCount());
            shareMap.put("visitCount", share.getVisitCount());
            shareMap.put("expireTime", share.getExpireTime());
            shareMap.put("isActive", share.getIsActive());
            shareMap.put("createdAt", share.getCreatedAt());

            // 检查是否过期
            shareMap.put("isExpired", !isShareValid(share));

            shareList.add(shareMap);
        }

        log.info("获取路线分享列表成功: routeId={}, count={}", routeId, shareList.size());
        return shareList;
    }

    @Override
    public RouteShare shareRoute(Integer routeId, Integer userId, String platform, String shareContent) {
        log.info("分享路线: routeId={}, userId={}, platform={}", routeId, userId, platform);

        // 参数校验
        if (routeId == null || userId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 校验路线是否存在
        Route route = routeService.getById(routeId.longValue());
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        // 创建分享记录
        RouteShare routeShare = new RouteShare();
        routeShare.setRouteId(routeId);
        routeShare.setUserId(userId);
        routeShare.setShareCode(generateUniqueShareCode());
        routeShare.setShareTitle(route.getTitle() + " - 分享");
        routeShare.setShareDescription(shareContent != null ? shareContent : route.getDescription());
        routeShare.setShareCount(1);
        routeShare.setVisitCount(0);
        routeShare.setIsActive(true);
        routeShare.setCreatedAt(LocalDateTime.now());

        // 保存到数据库
        save(routeShare);

        log.info("路线分享成功: routeId={}, userId={}, shareCode={}", routeId, userId, routeShare.getShareCode());
        return routeShare;
    }

    @Override
    public RouteShare getByShareCode(String shareCode) {
        if (shareCode == null || shareCode.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "code", shareCode);
        Object cachedObj = cacheUtil.get(cacheKey, RouteShare.class);
        if (cachedObj instanceof RouteShare) {
            RouteShare routeShare = (RouteShare) cachedObj;
            // 检查是否过期
            if (isShareValid(routeShare)) {
                return routeShare;
            }
        }

        // 从数据库获取
        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteShare::getShareCode, shareCode)
                .eq(RouteShare::getIsActive, true);

        RouteShare routeShare = getOne(queryWrapper);
        if (routeShare == null) {
            throw new BusinessException(ErrorCodeEnum.SHARE_NOT_EXIST);
        }

        // 检查是否过期
        if (!isShareValid(routeShare)) {
            // 标记为无效
            routeShare.setIsActive(false);
            updateById(routeShare);
            throw new BusinessException(ErrorCodeEnum.SHARE_EXPIRED);
        }

        // 缓存结果
        cacheUtil.set(cacheKey, routeShare, 24, TimeUnit.HOURS);

        return routeShare;
    }

    @Override
    public boolean incrementVisitCount(Integer shareId) {
        if (shareId == null || shareId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteShare routeShare = getById(shareId);
        if (routeShare == null) {
            throw new BusinessException(ErrorCodeEnum.SHARE_NOT_EXIST);
        }

        // 增加访问次数
        routeShare.setVisitCount(routeShare.getVisitCount() + 1);
        boolean result = updateById(routeShare);

        if (result) {
            // 清除缓存
            String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "code", routeShare.getShareCode());
            cacheUtil.delete(cacheKey);
            String statsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "stats", shareId);
            cacheUtil.delete(statsCacheKey);
            log.info("增加分享访问次数成功: shareId={}, newCount={}", shareId, routeShare.getVisitCount());
        }

        return result;
    }

    @Override
    public List<RouteShare> getUserShares(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "user", userId);
        Object cachedObj = cacheUtil.get(cacheKey, List.class);
        if (cachedObj instanceof List) {
            List<?> cachedList = (List<?>) cachedObj;
            List<RouteShare> cachedShares = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof RouteShare) {
                    cachedShares.add((RouteShare) item);
                }
            }
            if (!cachedShares.isEmpty()) {
                return cachedShares;
            }
        }

        // 从数据库获取
        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(RouteShare::getCreatedAt);

        List<RouteShare> shares = list(queryWrapper);

        // 缓存结果
        cacheUtil.set(cacheKey, shares, 30, TimeUnit.MINUTES);

        return shares;
    }

    @Override
    public boolean cancelShare(Integer shareId, Integer userId) {
        if (shareId == null || shareId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteShare routeShare = getById(shareId);
        if (routeShare == null) {
            throw new BusinessException(ErrorCodeEnum.SHARE_NOT_EXIST);
        }

        // 校验归属
        if (routeShare.getUser() == null || !routeShare.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }

        // 标记为无效
        routeShare.setIsActive(false);
        boolean result = updateById(routeShare);

        if (result) {
            // 清除缓存
            String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "code", routeShare.getShareCode());
            cacheUtil.delete(cacheKey);
            String userCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "user", userId);
            cacheUtil.delete(userCacheKey);
            log.info("取消分享成功: shareId={}, userId={}", shareId, userId);
        }

        return result;
    }

    @Override
    public boolean increaseVisitCount(String shareCode) {
        log.info("增加访问次数: shareCode={}", shareCode);
        RouteShare share = getByShareCode(shareCode);
        if (share == null) {
            return false;
        }
        return incrementVisitCount(share.getId());
    }

    @Override
    public long countByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteShare::getUserId, userId);
        return count(queryWrapper);
    }

    @Override
    public Map<String, Object> getShareStatistics(Long shareId) {
        if (shareId == null || shareId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "stats", shareId);
        Object cachedObj = cacheUtil.get(cacheKey, Map.class);
        if (cachedObj instanceof Map<?, ?>) {
            Map<?, ?> cachedMap = (Map<?, ?>) cachedObj;
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : cachedMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), entry.getValue());
                }
            }
            return result;
        }

        RouteShare routeShare = getById(shareId);
        if (routeShare == null) {
            throw new BusinessException(ErrorCodeEnum.SHARE_NOT_EXIST);
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("shareId", shareId);
        statistics.put("shareCode", routeShare.getShareCode());
        statistics.put("shareTitle", routeShare.getShareTitle());
        statistics.put("shareCount", routeShare.getShareCount());
        statistics.put("visitCount", routeShare.getVisitCount());
        statistics.put("createdAt", routeShare.getCreatedAt());
        statistics.put("expireTime", routeShare.getExpireTime());
        statistics.put("isActive", routeShare.getIsActive());
        statistics.put("isExpired", !isShareValid(routeShare));

        // 缓存结果
        cacheUtil.set(cacheKey, statistics, 1, TimeUnit.HOURS);

        return statistics;
    }

    @Override
    public int cleanExpiredShares() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(RouteShare::getExpireTime, now)
                .eq(RouteShare::getIsActive, true);

        List<RouteShare> expiredShares = list(queryWrapper);
        int count = 0;

        for (RouteShare share : expiredShares) {
            share.setIsActive(false);
            if (updateById(share)) {
                count++;
                // 清除缓存
                String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_SHARE_KEY_PREFIX, "code", share.getShareCode());
                cacheUtil.delete(cacheKey);
            }
        }

        log.info("清理过期分享成功: 清理数量={}", count);
        return count;
    }

    /**
     * 生成唯一分享码
     */
    private String generateUniqueShareCode() {
        String shareCode;
        boolean exists;

        do {
            StringBuilder sb = new StringBuilder(SHARE_CODE_LENGTH);
            for (int i = 0; i < SHARE_CODE_LENGTH; i++) {
                int index = SECURE_RANDOM.nextInt(SHARE_CODE_CHARS.length());
                sb.append(SHARE_CODE_CHARS.charAt(index));
            }
            shareCode = sb.toString();

            // 检查是否已存在
            LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RouteShare::getShareCode, shareCode);
            exists = count(queryWrapper) > 0;
        } while (exists);

        return shareCode;
    }

    /**
     * 检查分享是否有效
     */
    private boolean isShareValid(RouteShare routeShare) {
        if (!routeShare.getIsActive()) {
            return false;
        }

        LocalDateTime expireTime = routeShare.getExpireTime();
        return expireTime == null || expireTime.isAfter(LocalDateTime.now());
    }

    // 以下是Controller中使用的方法实现

    @Override
    public RouteShare createRouteShare(RouteShare routeShare) {
        log.info("创建路线分享: {}", routeShare);
        routeShare.setShareCode(generateUniqueShareCode());
        routeShare.setIsActive(true);
        routeShare.setCreatedAt(LocalDateTime.now());
        save(routeShare);
        return routeShare;
    }

    @Override
    public RouteShare getShareInfo(String shareCode) {
        log.info("获取分享信息: shareCode={}", shareCode);
        return getByShareCode(shareCode);
    }

    @Override
    public String accessShareFile(String shareCode, String password) {
        log.info("访问分享文件: shareCode={}", shareCode);
        RouteShare share = getByShareCode(shareCode);
        if (share == null || !isShareValid(share)) {
            return null;
        }
        if (share.getPassword() != null && !share.getPassword().equals(password)) {
            return null;
        }
        incrementVisitCount(share.getId());
        return "/access/" + shareCode;
    }

    @Override
    public List<RouteShare> getUserShares(Integer userId, int page, int size) {
        log.info("获取用户分享列表: userId={}, page={}, size={}", userId, page, size);
        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteShare::getUserId, userId);
        queryWrapper.orderByDesc(RouteShare::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RouteShare> pageResult = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        return page(pageResult, queryWrapper).getRecords();
    }

    @Override
    public boolean cancelShare(Long id) {
        log.info("取消分享: id={}", id);
        RouteShare share = getById(id.intValue());
        if (share == null) {
            return false;
        }
        share.setIsActive(false);
        return updateById(share);
    }

    @Override
    public boolean updateShareSettings(Long id, Map<String, Object> settings) {
        log.info("更新分享设置: id={}", id);
        RouteShare share = getById(id.intValue());
        if (share == null) {
            return false;
        }
        if (settings.containsKey("shareTitle")) {
            share.setShareTitle((String) settings.get("shareTitle"));
        }
        if (settings.containsKey("shareDescription")) {
            share.setShareDescription((String) settings.get("shareDescription"));
        }
        if (settings.containsKey("password")) {
            share.setPassword((String) settings.get("password"));
        }
        return updateById(share);
    }

    @Override
    public int batchCancelShares(List<Long> ids) {
        log.info("批量取消分享: count={}", ids.size());
        int count = 0;
        for (Long id : ids) {
            if (cancelShare(id)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<RouteShare> getPopularShares(int limit) {
        log.info("获取热门分享: limit={}", limit);
        LambdaQueryWrapper<RouteShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteShare::getIsActive, true);
        queryWrapper.orderByDesc(RouteShare::getVisitCount);
        return list(queryWrapper);
    }

    @Override
    public Map<String, Object> accessShareRoute(String shareCode) {
        log.info("访问分享路线: shareCode={}", shareCode);
        RouteShare share = getByShareCode(shareCode);
        if (share == null || !isShareValid(share)) {
            return null;
        }
        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("routeId", share.getRouteId());
        routeInfo.put("shareTitle", share.getShareTitle());
        routeInfo.put("shareDescription", share.getShareDescription());
        return routeInfo;
    }
}
