package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.entity.user_community.RouteComment;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.route_planning_mapper.RouteCommentMapper;
import travel.service.user_community.RouteCommentService;
import travel.service.route_planning.RouteService;
import travel.service.user_community.UserService;
import travel.utils.CacheUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 路线评价服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCommentServiceImpl extends ServiceImpl<RouteCommentMapper, RouteComment> implements RouteCommentService {

    private final RouteService routeService;
    private final UserService userService;
    private final CacheUtil cacheUtil;

    @Override
    public RouteComment createComment(Integer routeId, Integer userId, Double rating, String content, String images, Boolean isAnonymous, Integer replyTo) {
        // 1. 参数校验
        if (routeId == null || userId == null || rating == null || content == null || content.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 2. 校验评分范围
        if (rating < 1.0 || rating > 5.0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "评分必须在1.0到5.0之间");
        }

        // 3. 校验路线是否存在
        Route route = routeService.getById(routeId.longValue());
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        // 4. 校验用户是否存在
        User user = userService.getById(userId.longValue());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 5. 如果是回复评论，校验回复的评论是否存在
        if (replyTo != null) {
            RouteComment replyComment = getById(replyTo);
            if (replyComment == null) {
                throw new BusinessException(ErrorCodeEnum.COMMENT_NOT_EXIST);
            }
        }

        // 6. 创建评论
        RouteComment routeComment = new RouteComment();
        routeComment.setRouteId(routeId);
        routeComment.setUserId(userId);
        routeComment.setRating(rating);
        routeComment.setContent(content);
        routeComment.setImages(images);
        routeComment.setIsAnonymous(isAnonymous != null && isAnonymous);
        routeComment.setReplyTo(replyTo);
        routeComment.setIsPublished(true);
        routeComment.setLikesCount(0);
        routeComment.setCreatedAt(LocalDateTime.now());
        routeComment.setUpdatedAt(LocalDateTime.now());

        // 7. 保存到数据库
        save(routeComment);

        // 8. 清除缓存
        String routeCommentsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "route", routeId);
        cacheUtil.delete(routeCommentsCacheKey);
        String routeStatsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "stats", routeId);
        cacheUtil.delete(routeStatsCacheKey);

        log.info("创建路线评论成功: routeId={}, userId={}, rating={}", routeId, userId, rating);
        return routeComment;
    }

    @Override
    public List<RouteComment> getRouteComments(Integer routeId, int page, int size) {
        if (routeId == null || routeId <= 0 || page <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "route", routeId, "page", page, "size", size);
        Object cachedObj = cacheUtil.get(cacheKey, List.class);
        if (cachedObj instanceof List) {
            List<?> cachedList = (List<?>) cachedObj;
            List<RouteComment> cachedComments = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof RouteComment) {
                    cachedComments.add((RouteComment) item);
                }
            }
            if (!cachedComments.isEmpty()) {
                return cachedComments;
            }
        }

        // 从数据库获取
        LambdaQueryWrapper<RouteComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteComment::getIsPublished, true)
                .eq(RouteComment::getReplyTo, null) // 只获取顶级评论
                .orderByDesc(RouteComment::getCreatedAt);

        IPage<RouteComment> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteComment> comments = pageResult.getRecords();

        // 缓存结果
        cacheUtil.set(cacheKey, comments, 30, TimeUnit.MINUTES);

        return comments;
    }

    @Override
    public List<RouteComment> getUserComments(Integer userId, int page, int size) {
        if (userId == null || userId <= 0 || page <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "user", userId, "page", page, "size", size);
        Object cachedObj = cacheUtil.get(cacheKey, List.class);
        if (cachedObj instanceof List) {
            List<?> cachedList = (List<?>) cachedObj;
            List<RouteComment> cachedComments = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof RouteComment) {
                    cachedComments.add((RouteComment) item);
                }
            }
            if (!cachedComments.isEmpty()) {
                return cachedComments;
            }
        }

        // 从数据库获取
        LambdaQueryWrapper<RouteComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteComment::getUserId, userId);
        queryWrapper.orderByDesc(RouteComment::getCreatedAt);

        IPage<RouteComment> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteComment> comments = pageResult.getRecords();

        // 缓存结果
        cacheUtil.set(cacheKey, comments, 30, TimeUnit.MINUTES);

        return comments;
    }

    @Override
    public boolean likeComment(Integer commentId, Integer userId) {
        if (commentId == null || commentId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteComment routeComment = getById(commentId);
        if (routeComment == null) {
            throw new BusinessException(ErrorCodeEnum.COMMENT_NOT_EXIST);
        }

        // 检查是否已经点赞
        String likeCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "like", commentId, "user", userId);
        if (cacheUtil.exists(likeCacheKey)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "已经点赞过该评论");
        }

        // 增加点赞数
        routeComment.setLikesCount(routeComment.getLikesCount() + 1);
        boolean result = updateById(routeComment);

        if (result) {
            // 缓存点赞记录
            cacheUtil.set(likeCacheKey, true, 365, TimeUnit.DAYS);
            // 清除缓存
            if (routeComment.getRouteId() != null) {
                String routeCommentsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "route", routeComment.getRouteId());
                cacheUtil.delete(routeCommentsCacheKey);
            }
            log.info("点赞评论成功: commentId={}, userId={}", commentId, userId);
        }

        return result;
    }

    @Override
    public boolean unlikeComment(Integer commentId, Integer userId) {
        if (commentId == null || commentId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteComment routeComment = getById(commentId);
        if (routeComment == null) {
            throw new BusinessException(ErrorCodeEnum.COMMENT_NOT_EXIST);
        }

        // 检查是否已经点赞
        String likeCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "like", commentId, "user", userId);
        if (!cacheUtil.exists(likeCacheKey)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "还未点赞该评论");
        }

        // 减少点赞数
        if (routeComment.getLikesCount() > 0) {
            routeComment.setLikesCount(routeComment.getLikesCount() - 1);
        }
        boolean result = updateById(routeComment);

        if (result) {
            // 删除点赞记录
            cacheUtil.delete(likeCacheKey);
            // 清除缓存
            if (routeComment.getRouteId() != null) {
                String routeCommentsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "route", routeComment.getRouteId());
                cacheUtil.delete(routeCommentsCacheKey);
            }
            log.info("取消点赞评论成功: commentId={}, userId={}", commentId, userId);
        }

        return result;
    }

    @Override
    public boolean deleteComment(Integer commentId, Integer userId) {
        if (commentId == null || commentId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        RouteComment routeComment = getById(commentId);
        if (routeComment == null) {
            throw new BusinessException(ErrorCodeEnum.COMMENT_NOT_EXIST);
        }

        // 校验归属
        if (!routeComment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_COMMENT_PERMISSION);
        }

        // 删除评论
        boolean result = removeById(commentId);

        if (result) {
            // 清除缓存
            if (routeComment.getRouteId() != null) {
                String routeCommentsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "route", routeComment.getRouteId());
                cacheUtil.delete(routeCommentsCacheKey);
                String routeStatsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "stats", routeComment.getRouteId());
                cacheUtil.delete(routeStatsCacheKey);
            }
            String userCommentsCacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "user", userId);
            cacheUtil.delete(userCommentsCacheKey);
            log.info("删除评论成功: commentId={}, userId={}", commentId, userId);
        }

        return result;
    }

    @Override
    public Map<String, Object> getCommentStatistics(Integer routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "stats", routeId);
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

        // 从数据库获取
        LambdaQueryWrapper<RouteComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteComment::getIsPublished, true);

        List<RouteComment> comments = list(queryWrapper);
        int totalComments = comments.size();
        double averageRating = 0.0;
        Map<Integer, Integer> ratingDistribution = new HashMap<>();

        if (!comments.isEmpty()) {
            double totalRating = 0.0;
            for (RouteComment comment : comments) {
                double rating = comment.getRating();
                totalRating += rating;
                int ratingInt = (int) Math.round(rating);
                ratingDistribution.put(ratingInt, ratingDistribution.getOrDefault(ratingInt, 0) + 1);
            }
            averageRating = totalRating / totalComments;
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("routeId", routeId);
        statistics.put("totalComments", totalComments);
        statistics.put("averageRating", averageRating);
        statistics.put("ratingDistribution", ratingDistribution);

        // 缓存结果
        cacheUtil.set(cacheKey, statistics, 24, TimeUnit.HOURS);

        return statistics;
    }

    @Override
    public List<RouteComment> getCommentReplies(Integer commentId, int page, int size) {
        if (commentId == null || commentId <= 0 || page <= 0 || size <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 尝试从缓存获取
        String cacheKey = CacheUtil.generateKey(CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "replies", commentId, "page", page, "size", size);
        Object cachedObj = cacheUtil.get(cacheKey, List.class);
        if (cachedObj instanceof List) {
            List<?> cachedList = (List<?>) cachedObj;
            List<RouteComment> cachedReplies = new ArrayList<>();
            for (Object item : cachedList) {
                if (item instanceof RouteComment) {
                    cachedReplies.add((RouteComment) item);
                }
            }
            if (!cachedReplies.isEmpty()) {
                return cachedReplies;
            }
        }

        // 从数据库获取
        LambdaQueryWrapper<RouteComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RouteComment::getReplyTo, commentId)
                .eq(RouteComment::getIsPublished, true)
                .orderByAsc(RouteComment::getCreatedAt);

        IPage<RouteComment> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteComment> replies = pageResult.getRecords();

        // 缓存结果
        cacheUtil.set(cacheKey, replies, 30, TimeUnit.MINUTES);

        return replies;
    }


}
