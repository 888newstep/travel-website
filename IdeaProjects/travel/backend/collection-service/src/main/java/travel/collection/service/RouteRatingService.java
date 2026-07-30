package travel.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.RouteComment;

import java.util.List;

/**
 * 路线评分服务接口 (已合并到 route_comment 表)
 */
public interface RouteRatingService extends IService<RouteComment> {

    /**
     * 根据路线ID获取评分列表
     */
    List<RouteComment> getByRouteId(Integer routeId);

    /**
     * 根据用户ID获取评分列表
     */
    List<RouteComment> getByUserId(Integer userId);

    /**
     * 获取路线的平均评分
     */
    Double getAverageRating(Integer routeId);

    /**
     * 获取路线的评分数量
     */
    Integer getRatingCount(Integer routeId);

    /**
     * 用户是否已对路线评分
     */
    boolean hasRated(Integer routeId, Integer userId);

    /**
     * 获取用户评分
     */
    RouteComment getUserRating(Integer routeId, Integer userId);
}