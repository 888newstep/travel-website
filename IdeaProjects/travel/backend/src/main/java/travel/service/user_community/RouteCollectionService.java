package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.RouteCollection;
import travel.entity.vo.RouteCollectionVO;

import java.util.List;

public interface RouteCollectionService extends IService<RouteCollection> {

    /**
     * 收藏路线
     */
    boolean collectRoute(Integer routeId, Integer userId);

    /**
     * 取消收藏路线
     */
    boolean uncollectRoute(Integer routeId, Integer userId);

    /**
     * 检查是否已收藏
     */
    boolean isCollected(Integer routeId, Integer userId);

    /**
     * 获取用户的收藏列表
     */
    List<RouteCollectionVO> getUserCollections(Integer userId, int page, int size);

    /**
     * 统计用户的收藏数量
     */
    long countByUserId(Integer userId);

    /**
     * 创建收藏
     */
    RouteCollection createCollection(Integer routeId, Integer userId, Boolean isPublic, String notes);

    /**
     * 取消收藏
     */
    boolean cancelCollect(Integer routeId, Integer userId);

    /**
     * 获取路线收藏数量
     */
    int getRouteCollectionCount(Integer routeId);

    /**
     * 更新收藏备注
     */
    boolean updateCollectionNotes(Integer collectionId, Integer userId, String notes);

    /**
     * 更新收藏公开状态
     */
    boolean updateCollectionPublicStatus(Integer collectionId, Integer userId, Boolean isPublic);

    /**
     * 获取公开收藏列表
     */
    List<RouteCollection> getPublicCollections(int page, int size);
}
