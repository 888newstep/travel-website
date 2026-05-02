package travel.service.route_planning;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.route_planning.RouteRating;

import java.util.List;

/**
 * 路线评分服务接口
 */
public interface RouteRatingService extends IService<RouteRating> {

    /**
     * 根据路线ID获取评分列表
     * @param routeId 路线ID
     * @return 评分列表
     */
    List<RouteRating> getByRouteId(Integer routeId);

    /**
     * 根据用户ID获取评分列表
     * @param userId 用户ID
     * @return 评分列表
     */
    List<RouteRating> getByUserId(Integer userId);

    /**
     * 获取路线的平均评分
     * @param routeId 路线ID
     * @return 平均评分
     */
    Double getAverageRating(Integer routeId);

    /**
     * 获取路线的评分数量
     * @param routeId 路线ID
     * @return 评分数量
     */
    Integer getRatingCount(Integer routeId);

    /**
     * 用户是否已对路线评分
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 是否已评分
     */
    boolean hasRated(Integer routeId, Integer userId);

    /**
     * 获取用户评分
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 评分记录
     */
    RouteRating getUserRating(Integer routeId, Integer userId);
}
