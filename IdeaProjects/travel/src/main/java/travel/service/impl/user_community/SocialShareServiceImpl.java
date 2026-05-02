package travel.service.impl.user_community;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.service.route_planning.RouteService;
import travel.service.user_community.SocialShareService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialShareServiceImpl implements SocialShareService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String SHARE_PREFIX = "social:share:";
    private static final String LIKE_PREFIX = "social:like:";
    private static final String FOLLOW_PREFIX = "social:follow:";

    @Override
    public Map<String, Object> shareRoute(Integer routeId, Integer userId, String shareContent, List<String> tags) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        // 模拟创建分享
        Integer shareId = generateShareId();

        Map<String, Object> share = new HashMap<>();
        share.put("id", shareId);
        share.put("routeId", routeId);
        share.put("userId", userId);
        share.put("content", shareContent);
        share.put("tags", tags);
        share.put("createTime", LocalDateTime.now());
        share.put("likeCount", 0);
        share.put("commentCount", 0);
        share.put("repostCount", 0);

        // 缓存分享
        String cacheKey = SHARE_PREFIX + shareId;
        cacheUtil.set(cacheKey, share, 7, TimeUnit.DAYS);

        result.put("success", true);
        result.put("shareId", shareId);
        result.put("message", "分享成功");

        return result;
    }

    @Override
    public List<Map<String, Object>> getHotShares(Integer limit) {
        List<Map<String, Object>> hotShares = new ArrayList<>();

        // 模拟热门分享数据
        for (int i = 1; i <= limit; i++) {
            Map<String, Object> share = new HashMap<>();
            share.put("id", i);
            share.put("routeId", i * 10);
            share.put("userId", i * 5);
            share.put("content", "热门分享内容 " + i);
            share.put("likeCount", 100 - i * 5);
            share.put("commentCount", 50 - i * 2);
            share.put("createTime", LocalDateTime.now().minusHours(i));
            hotShares.add(share);
        }

        return hotShares;
    }

    @Override
    public List<Map<String, Object>> getFollowingShares(Integer userId) {
        String cacheKey = SHARE_PREFIX + "following:" + userId;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cached = cacheUtil.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        // 获取关注用户的分享
        List<Map<String, Object>> shares = new ArrayList<>();
        List<Map<String, Object>> following = getFollowing(userId);

        for (Map<String, Object> follow : following) {
            Integer followUserId = (Integer) follow.get("userId");
            // 模拟获取该用户的分享
            Map<String, Object> share = new HashMap<>();
            share.put("id", followUserId * 100);
            share.put("userId", followUserId);
            share.put("content", "关注用户的分享");
            shares.add(share);
        }

        cacheUtil.set(cacheKey, shares, 30, TimeUnit.MINUTES);
        return shares;
    }

    @Override
    public boolean likeShare(Integer shareId, Integer userId) {
        String likeKey = LIKE_PREFIX + shareId + ":" + userId;

        // 检查是否已经点赞
        if (cacheUtil.exists(likeKey)) {
            return false;
        }

        // 记录点赞
        cacheUtil.set(likeKey, true, 30, TimeUnit.DAYS);

        // 更新点赞数
        String shareKey = SHARE_PREFIX + shareId;
        @SuppressWarnings("unchecked")
        Map<String, Object> share = cacheUtil.get(shareKey, Map.class);
        if (share != null) {
            Integer likeCount = (Integer) share.getOrDefault("likeCount", 0);
            share.put("likeCount", likeCount + 1);
            cacheUtil.set(shareKey, share, 7, TimeUnit.DAYS);
        }

        return true;
    }

    @Override
    public boolean unlikeShare(Integer shareId, Integer userId) {
        String likeKey = LIKE_PREFIX + shareId + ":" + userId;

        // 删除点赞记录
        cacheUtil.delete(likeKey);

        // 更新点赞数
        String shareKey = SHARE_PREFIX + shareId;
        @SuppressWarnings("unchecked")
        Map<String, Object> share = cacheUtil.get(shareKey, Map.class);
        if (share != null) {
            Integer likeCount = (Integer) share.getOrDefault("likeCount", 0);
            share.put("likeCount", Math.max(0, likeCount - 1));
            cacheUtil.set(shareKey, share, 7, TimeUnit.DAYS);
        }

        return true;
    }

    @Override
    public Map<String, Object> commentShare(Integer shareId, Integer userId, String content, Integer parentId) {
        Map<String, Object> result = new HashMap<>();

        Integer commentId = generateCommentId();

        Map<String, Object> comment = new HashMap<>();
        comment.put("id", commentId);
        comment.put("shareId", shareId);
        comment.put("userId", userId);
        comment.put("content", content);
        comment.put("parentId", parentId);
        comment.put("createTime", LocalDateTime.now());

        // 更新评论数
        String shareKey = SHARE_PREFIX + shareId;
        @SuppressWarnings("unchecked")
        Map<String, Object> share = cacheUtil.get(shareKey, Map.class);
        if (share != null) {
            Integer commentCount = (Integer) share.getOrDefault("commentCount", 0);
            share.put("commentCount", commentCount + 1);
            cacheUtil.set(shareKey, share, 7, TimeUnit.DAYS);
        }

        result.put("success", true);
        result.put("commentId", commentId);
        result.put("comment", comment);

        return result;
    }

    @Override
    public boolean favoriteShare(Integer shareId, Integer userId) {
        String favoriteKey = "social:favorite:" + userId + ":" + shareId;
        cacheUtil.set(favoriteKey, true, 30, TimeUnit.DAYS);
        return true;
    }

    @Override
    public Map<String, Object> repostShare(Integer shareId, Integer userId, String comment) {
        Map<String, Object> result = new HashMap<>();

        // 获取原分享
        String shareKey = SHARE_PREFIX + shareId;
        @SuppressWarnings("unchecked")
        Map<String, Object> originalShare = cacheUtil.get(shareKey, Map.class);

        if (originalShare == null) {
            result.put("success", false);
            result.put("message", "分享不存在");
            return result;
        }

        // 创建转发
        Integer newShareId = generateShareId();
        Map<String, Object> repost = new HashMap<>();
        repost.put("id", newShareId);
        repost.put("userId", userId);
        repost.put("content", comment);
        repost.put("originalShareId", shareId);
        repost.put("createTime", LocalDateTime.now());

        // 更新转发数
        Integer repostCount = (Integer) originalShare.getOrDefault("repostCount", 0);
        originalShare.put("repostCount", repostCount + 1);
        cacheUtil.set(shareKey, originalShare, 7, TimeUnit.DAYS);

        result.put("success", true);
        result.put("shareId", newShareId);

        return result;
    }

    @Override
    public boolean followUser(Integer userId, Integer followUserId) {
        if (userId.equals(followUserId)) {
            return false;
        }

        String followKey = FOLLOW_PREFIX + userId + ":" + followUserId;
        cacheUtil.set(followKey, true, 365, TimeUnit.DAYS);

        // 更新关注数缓存
        String followingCountKey = "social:following_count:" + userId;
        cacheUtil.delete(followingCountKey);

        String followerCountKey = "social:follower_count:" + followUserId;
        cacheUtil.delete(followerCountKey);

        return true;
    }

    @Override
    public boolean unfollowUser(Integer userId, Integer followUserId) {
        String followKey = FOLLOW_PREFIX + userId + ":" + followUserId;
        cacheUtil.delete(followKey);

        // 更新关注数缓存
        String followingCountKey = "social:following_count:" + userId;
        cacheUtil.delete(followingCountKey);

        String followerCountKey = "social:follower_count:" + followUserId;
        cacheUtil.delete(followerCountKey);

        return true;
    }

    @Override
    public List<Map<String, Object>> getFollowers(Integer userId) {
        // 模拟获取粉丝列表
        List<Map<String, Object>> followers = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> follower = new HashMap<>();
            follower.put("userId", i * 100);
            follower.put("username", "粉丝用户" + i);
            follower.put("followTime", LocalDateTime.now().minusDays(i));
            followers.add(follower);
        }

        return followers;
    }

    @Override
    public List<Map<String, Object>> getFollowing(Integer userId) {
        String cacheKey = FOLLOW_PREFIX + "list:" + userId;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cached = cacheUtil.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        // 模拟获取关注列表
        List<Map<String, Object>> following = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> follow = new HashMap<>();
            follow.put("userId", i * 200);
            follow.put("username", "关注用户" + i);
            follow.put("followTime", LocalDateTime.now().minusDays(i));
            following.add(follow);
        }

        cacheUtil.set(cacheKey, following, 60, TimeUnit.MINUTES);
        return following;
    }

    @Override
    public List<Map<String, Object>> searchShares(String keyword, List<String> tags) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 模拟搜索
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> share = new HashMap<>();
            share.put("id", i);
            share.put("content", "包含关键词 '" + keyword + "' 的分享 " + i);
            share.put("tags", tags != null ? tags : Arrays.asList("旅行", "攻略"));
            share.put("likeCount", 50 + i * 5);
            results.add(share);
        }

        return results;
    }

    @Override
    public Map<String, Object> getShareDetail(Integer shareId) {
        String cacheKey = SHARE_PREFIX + shareId;

        @SuppressWarnings("unchecked")
        Map<String, Object> share = cacheUtil.get(cacheKey, Map.class);

        if (share == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "分享不存在");
            return result;
        }

        return share;
    }

    @Override
    public Map<String, Object> getUserShareStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("userId", userId);
        stats.put("shareCount", 25);
        stats.put("totalLikes", 1250);
        stats.put("totalComments", 380);
        stats.put("followerCount", 156);
        stats.put("followingCount", 89);
        stats.put("favoriteCount", 45);

        return stats;
    }

    // 辅助方法
    private Integer generateShareId() {
        return (int) (System.currentTimeMillis() % 1000000);
    }

    private Integer generateCommentId() {
        return (int) (System.currentTimeMillis() % 1000000) + 1000000;
    }
}
