package travel.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.RouteCollection;
import travel.common.vo.RouteCollectionVO;

import java.util.List;

public interface RouteCollectionService extends IService<RouteCollection> {

    /**
     * 原子切换当前用户的路线收藏状态，返回切换后的状态。
     */
    boolean toggleCollection(Integer routeId, Integer userId);

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

    List<RouteCollectionVO> getUserCollectionsByCategory(Integer userId, String category, int page, int size);

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

    /**
     * 获取用户收藏分类列表
     */
    List<String> getUserCollectionCategories(Integer userId);

    /**
     * 批量删除收藏
     */
    int batchRemoveCollections(List<Integer> ids, Integer userId);
}
