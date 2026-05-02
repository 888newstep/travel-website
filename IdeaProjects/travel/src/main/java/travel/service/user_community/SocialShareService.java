package travel.service.user_community;

import java.util.List;
import java.util.Map;

/**
 * 社交分享服务
 * 支持路线分享、点赞、评论、收藏等社交功能
 */
public interface SocialShareService {

    /**
     * 分享路线到社区
     * @param routeId 路线ID
     * @param userId 用户ID
     * @param shareContent 分享内容
     * @param tags 标签
     * @return 分享结果
     */
    Map<String, Object> shareRoute(Integer routeId, Integer userId, String shareContent, List<String> tags);

    /**
     * 获取热门分享
     * @param limit 数量限制
     * @return 热门分享列表
     */
    List<Map<String, Object>> getHotShares(Integer limit);

    /**
     * 获取关注用户的分享
     * @param userId 用户ID
     * @return 分享列表
     */
    List<Map<String, Object>> getFollowingShares(Integer userId);

    /**
     * 点赞分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    boolean likeShare(Integer shareId, Integer userId);

    /**
     * 取消点赞
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 取消结果
     */
    boolean unlikeShare(Integer shareId, Integer userId);

    /**
     * 评论分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @param content 评论内容
     * @param parentId 父评论ID（回复）
     * @return 评论结果
     */
    Map<String, Object> commentShare(Integer shareId, Integer userId, String content, Integer parentId);

    /**
     * 收藏分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 收藏结果
     */
    boolean favoriteShare(Integer shareId, Integer userId);

    /**
     * 转发分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @param comment 转发评论
     * @return 转发结果
     */
    Map<String, Object> repostShare(Integer shareId, Integer userId, String comment);

    /**
     * 关注用户
     * @param userId 用户ID
     * @param followUserId 被关注用户ID
     * @return 关注结果
     */
    boolean followUser(Integer userId, Integer followUserId);

    /**
     * 取消关注
     * @param userId 用户ID
     * @param followUserId 被关注用户ID
     * @return 取消结果
     */
    boolean unfollowUser(Integer userId, Integer followUserId);

    /**
     * 获取用户粉丝列表
     * @param userId 用户ID
     * @return 粉丝列表
     */
    List<Map<String, Object>> getFollowers(Integer userId);

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    List<Map<String, Object>> getFollowing(Integer userId);

    /**
     * 搜索分享
     * @param keyword 关键词
     * @param tags 标签筛选
     * @return 搜索结果
     */
    List<Map<String, Object>> searchShares(String keyword, List<String> tags);

    /**
     * 获取分享详情
     * @param shareId 分享ID
     * @return 分享详情
     */
    Map<String, Object> getShareDetail(Integer shareId);

    /**
     * 获取用户分享统计
     * @param userId 用户ID
     * @return 统计数据
     */
    Map<String, Object> getUserShareStats(Integer userId);
}
