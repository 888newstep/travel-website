package travel.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.route_planning.Route;
import travel.common.entity.user_community.RouteComment;
import travel.common.entity.user_community.User;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteCommentMapper;
import travel.collection.service.RouteCommentService;
import travel.collection.dto.CommentLikeToggleResponse;
import travel.collection.service.RouteService;
import travel.collection.service.UserService;
import travel.common.service.DistributedLockService;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String COMMENT_LIKE_LOCK_PREFIX = "route-comment-like";

    private final RouteService routeService;
    private final UserService userService;
    private final CacheUtil cacheUtil;
    private final DistributedLockService distributedLockService;

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
            if (!routeId.equals(replyComment.getRouteId())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "不能跨路线回复评论");
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

        // 评论列表使用分页键，必须按前缀失效；回复还需要清理父评论的回复缓存。
        invalidateCommentCaches(routeComment, true);

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
        queryWrapper.eq(RouteComment::getRouteId, routeId)
                .eq(RouteComment::getIsPublished, true)
                .isNull(RouteComment::getReplyTo)
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
        queryWrapper.eq(RouteComment::getUserId, userId)
                .eq(RouteComment::getIsPublished, true)
                .orderByDesc(RouteComment::getCreatedAt);

        IPage<RouteComment> pageResult = page(new Page<>(page, size), queryWrapper);
        List<RouteComment> comments = pageResult.getRecords();

        // 缓存结果
        cacheUtil.set(cacheKey, comments, 30, TimeUnit.MINUTES);

        return comments;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeComment(Integer commentId, Integer userId) {
        validateCommentAction(commentId, userId);
        RouteComment routeComment = requirePublishedComment(commentId);
        return distributedLockService.executeWithLock(commentLikeLockKey(commentId, userId), () -> {
            if (baseMapper.countCommentLike(commentId, userId) > 0) {
                return false;
            }
            int inserted = baseMapper.insertCommentLike(commentId, userId);
            requireSingleRow(inserted, "insert comment like");
            requireSingleRow(baseMapper.incrementCommentLikeCount(commentId), "increment comment like count");
            invalidateCommentListCaches(routeComment);
            log.info("点赞评论成功: commentId={}, userId={}", commentId, userId);
            return true;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikeComment(Integer commentId, Integer userId) {
        validateCommentAction(commentId, userId);
        RouteComment routeComment = requirePublishedComment(commentId);
        return distributedLockService.executeWithLock(commentLikeLockKey(commentId, userId), () -> {
            int deleted = baseMapper.deleteCommentLike(commentId, userId);
            if (deleted > 0) {
                requireSingleRow(baseMapper.decrementCommentLikeCount(commentId), "decrement comment like count");
                invalidateCommentListCaches(routeComment);
                log.info("取消点赞评论成功: commentId={}, userId={}", commentId, userId);
            }
            return deleted > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentLikeToggleResponse toggleLikeComment(Integer commentId, Integer userId) {
        validateCommentAction(commentId, userId);
        RouteComment routeComment = requirePublishedComment(commentId);
        return distributedLockService.executeWithLock(commentLikeLockKey(commentId, userId), () -> {
            boolean alreadyLiked = baseMapper.countCommentLike(commentId, userId) > 0;
            boolean liked;
            if (alreadyLiked) {
                int deleted = baseMapper.deleteCommentLike(commentId, userId);
                if (deleted > 0) {
                    requireSingleRow(baseMapper.decrementCommentLikeCount(commentId), "decrement comment like count");
                }
                liked = false;
            } else {
                int inserted = baseMapper.insertCommentLike(commentId, userId);
                requireSingleRow(inserted, "insert comment like");
                requireSingleRow(baseMapper.incrementCommentLikeCount(commentId), "increment comment like count");
                liked = true;
            }

            Integer likeCount = baseMapper.selectCommentLikeCount(commentId);
            if (likeCount == null) {
                throw new IllegalStateException("Comment disappeared while toggling like: " + commentId);
            }
            invalidateCommentListCaches(routeComment);
            log.info("切换评论点赞状态成功: commentId={}, userId={}, liked={}, likeCount={}",
                    commentId, userId, liked, likeCount);

            return new CommentLikeToggleResponse(liked, likeCount);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 使用软删除保留回复关系，避免 ON DELETE SET NULL 将回复错误提升为顶级评论。
        baseMapper.deleteAllCommentLikes(commentId);
        routeComment.setLikesCount(0);
        routeComment.setIsPublished(false);
        routeComment.setUpdatedAt(LocalDateTime.now());
        boolean result = updateById(routeComment);

        if (!result) {
            throw new IllegalStateException("Failed to soft delete comment: " + commentId);
        }

        invalidateCommentCaches(routeComment, true);
        log.info("删除评论成功: commentId={}, userId={}", commentId, userId);

        return true;
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
        queryWrapper.eq(RouteComment::getRouteId, routeId)
                .eq(RouteComment::getIsPublished, true)
                .isNull(RouteComment::getReplyTo);

        List<RouteComment> comments = list(queryWrapper);
        int totalComments = comments.size();
        double averageRating = 0.0;
        Map<Integer, Integer> ratingDistribution = new HashMap<>();

        int ratedComments = 0;
        if (!comments.isEmpty()) {
            double totalRating = 0.0;
            for (RouteComment comment : comments) {
                Double rating = comment.getRating();
                if (rating == null) {
                    continue;
                }
                totalRating += rating;
                ratedComments++;
                int ratingInt = (int) Math.round(rating);
                ratingDistribution.put(ratingInt, ratingDistribution.getOrDefault(ratingInt, 0) + 1);
            }
            if (ratedComments > 0) {
                averageRating = totalRating / ratedComments;
            }
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

        requirePublishedComment(commentId);

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

    private void validateCommentAction(Integer commentId, Integer userId) {
        if (commentId == null || commentId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void requireSingleRow(int affectedRows, String operation) {
        if (affectedRows != 1) {
            throw new IllegalStateException(operation + " affected " + affectedRows + " rows");
        }
    }

    private RouteComment requirePublishedComment(Integer commentId) {
        RouteComment routeComment = getById(commentId);
        if (routeComment == null || !Boolean.TRUE.equals(routeComment.getIsPublished())) {
            throw new BusinessException(ErrorCodeEnum.COMMENT_NOT_EXIST);
        }
        return routeComment;
    }

    private String commentLikeLockKey(Integer commentId, Integer userId) {
        return CacheUtil.generateKey(COMMENT_LIKE_LOCK_PREFIX, commentId, userId);
    }

    private void invalidateCommentCaches(RouteComment routeComment, boolean invalidateStatistics) {
        invalidateCommentListCaches(routeComment);
        if (invalidateStatistics && routeComment.getRouteId() != null) {
            cacheUtil.delete(CacheUtil.generateKey(
                    CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "stats", routeComment.getRouteId()));
        }
    }

    private void invalidateCommentListCaches(RouteComment routeComment) {
        if (routeComment.getRouteId() != null) {
            cacheUtil.deleteByPattern(CacheUtil.generateKey(
                    CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "route", routeComment.getRouteId(), "*"));
        }
        if (routeComment.getUserId() != null) {
            cacheUtil.deleteByPattern(CacheUtil.generateKey(
                    CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "user", routeComment.getUserId(), "*"));
        }
        if (routeComment.getReplyTo() != null) {
            cacheUtil.deleteByPattern(CacheUtil.generateKey(
                    CacheUtil.ROUTE_COMMENT_KEY_PREFIX, "replies", routeComment.getReplyTo(), "*"));
        }
    }


}
