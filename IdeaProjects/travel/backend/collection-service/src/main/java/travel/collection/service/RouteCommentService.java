package travel.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.RouteComment;

import java.util.List;
import java.util.Map;

/**
 * 路线评价服务接口
 */
public interface RouteCommentService extends IService<RouteComment> {

    /**
     * 创建路线评价
     * @param routeId 路线ID
     * @param userId 用户ID
     * @param rating 评分
     * @param content 评论内容
     * @param images 评论图片
     * @param isAnonymous 是否匿名
     * @param replyTo 回复评论ID
     * @return 路线评价对象
     */
    RouteComment createComment(Integer routeId, Integer userId, Double rating, String content, String images, Boolean isAnonymous, Integer replyTo);

    /**
     * 获取路线的评论列表
     * @param routeId 路线ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    List<RouteComment> getRouteComments(Integer routeId, int page, int size);

    /**
     * 获取用户的评论列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    List<RouteComment> getUserComments(Integer userId, int page, int size);

    /**
     * 点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean likeComment(Integer commentId, Integer userId);

    /**
     * 取消点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unlikeComment(Integer commentId, Integer userId);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteComment(Integer commentId, Integer userId);

    /**
     * 获取路线的评论统计信息
     * @param routeId 路线ID
     * @return 统计信息
     */
    Map<String, Object> getCommentStatistics(Integer routeId);

    /**
     * 获取评论的回复列表
     * @param commentId 评论ID
     * @param page 页码
     * @param size 每页大小
     * @return 回复列表
     */
    List<RouteComment> getCommentReplies(Integer commentId, int page, int size);

    /**
     * 切换评论点赞状态（合并 like/unlike）
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return { liked: true/false, likeCount: number }
     */
    Map<String, Object> toggleLikeComment(Integer commentId, Integer userId);
}
