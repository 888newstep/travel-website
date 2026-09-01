package travel.collection.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.RouteComment;
import travel.common.security.AuthenticatedUserSupport;
import travel.collection.service.RouteCommentService;
import travel.collection.dto.CommentLikeToggleResponse;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 路线评论控制器
 * 管理路线的评价、评论、点赞等操作
 */
@RestController
@RequestMapping("/route-comments")
@Slf4j
@RequiredArgsConstructor
public class RouteCommentController {

    private final RouteCommentService routeCommentService;

    /**
     * 创建路线评价
     * POST /api/route-comments
     */
    @PostMapping
    public Result<RouteComment> createComment(@RequestBody RouteComment comment) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("创建路线评价请求: routeId={}, userId={}, rating={}",
                comment.getRouteId(), userId, comment.getRating());
        RouteComment created = routeCommentService.createComment(
                comment.getRouteId(),
                userId,
                comment.getRating(),
                comment.getContent(),
                comment.getImages(),
                comment.getIsAnonymous(),
                comment.getReplyTo()
        );
        return Result.success("创建路线评价成功", created);
    }

    /**
     * 获取路线的评论列表
     * GET /api/route-comments/route/{routeId}
     */
    @GetMapping("/route/{routeId}")
    public Result<List<RouteComment>> getRouteComments(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取路线评论列表请求: routeId={}, page={}, size={}", routeId, page, size);
        List<RouteComment> comments = routeCommentService.getRouteComments(routeId, page, size);
        return Result.success("获取路线评论列表成功", comments);
    }

    /**
     * 获取用户的评论列表
     * GET /api/route-comments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<RouteComment>> getUserComments(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取用户评论列表请求: userId={}, page={}, size={}", userId, page, size);
        List<RouteComment> comments = routeCommentService.getUserComments(userId, page, size);
        return Result.success("获取用户评论列表成功", comments);
    }

    /**
     * 切换点赞状态（合并 like/unlike）
     * POST /api/route-comments/{commentId}/toggle-like
     * @return { liked: true/false, likeCount: number }
     */
    @PostMapping("/{commentId}/toggle-like")
    public Result<CommentLikeToggleResponse> toggleLikeComment(@PathVariable Integer commentId) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("切换评论点赞状态: commentId={}, userId={}", commentId, userId);
        CommentLikeToggleResponse result = routeCommentService.toggleLikeComment(commentId, userId);
        return Result.success(result.liked() ? "点赞成功" : "取消点赞成功", result);
    }

    /**
     * 删除评论
     * DELETE /api/route-comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public Result<Boolean> deleteComment(@PathVariable Integer commentId) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("删除评论请求: commentId={}, userId={}", commentId, userId);
        boolean result = routeCommentService.deleteComment(commentId, userId);
        if (result) {
            return Result.success("删除评论成功", true);
        }
        return Result.error("删除评论失败");
    }

    /**
     * 获取路线的评论统计信息
     * GET /api/route-comments/statistics/{routeId}
     */
    @GetMapping("/statistics/{routeId}")
    public Result<Map<String, Object>> getCommentStatistics(@PathVariable Integer routeId) {
        log.info("获取评论统计信息请求: routeId={}", routeId);
        Map<String, Object> statistics = routeCommentService.getCommentStatistics(routeId);
        return Result.success("获取评论统计信息成功", statistics);
    }

    /**
     * 获取评论的回复列表
     * GET /api/route-comments/{commentId}/replies
     */
    @GetMapping("/{commentId}/replies")
    public Result<List<RouteComment>> getCommentReplies(
            @PathVariable Integer commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("获取评论回复列表请求: commentId={}, page={}, size={}", commentId, page, size);
        List<RouteComment> replies = routeCommentService.getCommentReplies(commentId, page, size);
        return Result.success("获取评论回复列表成功", replies);
    }

    /**
     * 批量获取评论
     * POST /api/route-comments/batch
     */
    @PostMapping("/batch")
    public Result<List<RouteComment>> getBatchComments(@RequestBody List<Integer> commentIds) {
        log.info("批量获取评论请求: count={}", commentIds.size());
        List<RouteComment> comments = routeCommentService.listByIds(commentIds);
        return Result.success("批量获取评论成功", comments);
    }

    /**
     * 获取热门评论
     * GET /api/route-comments/hot/{routeId}
     */
    @GetMapping("/hot/{routeId}")
    public Result<List<RouteComment>> getHotComments(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("获取热门评论请求: routeId={}, limit={}", routeId, limit);
        // 获取路线的所有评论并按点赞数排序
        List<RouteComment> allComments = routeCommentService.getRouteComments(routeId, 1, 100);
        // 按点赞数降序排序，取前limit个
        List<RouteComment> hotComments = allComments.stream()
                .sorted((c1, c2) -> Integer.compare(c2.getLikeCount(), c1.getLikeCount()))
                .limit(limit)
                .toList();
        return Result.success("获取热门评论成功", hotComments);
    }

    /**
     * 获取最新评论
     * GET /api/route-comments/latest/{routeId}
     */
    @GetMapping("/latest/{routeId}")
    public Result<List<RouteComment>> getLatestComments(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("获取最新评论请求: routeId={}, limit={}", routeId, limit);
        // 获取路线的最新评论
        List<RouteComment> latestComments = routeCommentService.getRouteComments(routeId, 1, limit);
        return Result.success("获取最新评论成功", latestComments);
    }

    /**
     * 搜索评论
     * GET /api/route-comments/search
     */
    @GetMapping("/search")
    public Result<List<RouteComment>> searchComments(
            @RequestParam Integer routeId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("搜索评论请求: routeId={}, keyword={}", routeId, keyword);
        // 获取路线的所有评论
        List<RouteComment> allComments = routeCommentService.getRouteComments(routeId, 1, 1000);
        // 过滤包含关键词的评论
        List<RouteComment> filteredComments = allComments.stream()
                .filter(comment -> comment.getContent() != null &&
                        comment.getContent().contains(keyword))
                .toList();
        return Result.success("搜索评论成功", filteredComments);
    }

    /**
     * 获取高评分评论
     * GET /api/route-comments/high-rating/{routeId}
     */
    @GetMapping("/high-rating/{routeId}")
    public Result<List<RouteComment>> getHighRatingComments(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "4.0") double minRating,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("获取高评分评论请求: routeId={}, minRating={}", routeId, minRating);
        // 获取路线的所有评论
        List<RouteComment> allComments = routeCommentService.getRouteComments(routeId, 1, 1000);
        // 过滤评分高于minRating的评论
        List<RouteComment> highRatingComments = allComments.stream()
                .filter(comment -> comment.getRating() != null && comment.getRating() >= minRating)
                .limit(limit)
                .toList();
        return Result.success("获取高评分评论成功", highRatingComments);
    }
}
