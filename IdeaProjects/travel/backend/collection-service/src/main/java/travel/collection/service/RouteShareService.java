package travel.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.RouteShare;

import java.util.List;
import java.util.Map;

public interface RouteShareService extends IService<RouteShare> {

    /**
     * 分享路线
     */
    RouteShare shareRoute(Integer routeId, Integer userId, String platform, String shareContent);

    /**
     * 生成分享码
     */
    RouteShare generateShareCode(RouteShare share);

    /**
     * 验证分享码是否有效
     */
    boolean validateShareCode(String shareCode);

    /**
     * 获取路线的分享列表
     */
    List<Map<String, Object>> getRouteShares(Integer routeId, int page, int size);

    /**
     * 统计用户的分享数量
     */
    long countByUserId(Integer userId);

    /**
     * 创建分享
     */
    RouteShare createShare(Integer routeId, Integer userId, String shareTitle, String shareDescription, Integer expireDays);

    /**
     * 根据分享码获取分享
     */
    RouteShare getByShareCode(String shareCode);

    /**
     * 增加访问次数
     */
    boolean incrementVisitCount(Integer shareId);

    /**
     * 获取用户的所有分享
     */
    List<RouteShare> getUserShares(Integer userId);

    /**
     * 取消分享
     */
    boolean cancelShare(Integer shareId, Integer userId);

    /**
     * 增加访问次数（通过分享码）
     */
    boolean increaseVisitCount(String shareCode);

    /**
     * 获取分享统计信息
     */
    Map<String, Object> getShareStatistics(Long shareId, Integer userId);

    /**
     * 清理过期分享
     */
    int cleanExpiredShares();

    /**
     * 创建路线分享
     */
    RouteShare createRouteShare(RouteShare routeShare);

    /**
     * 获取分享信息
     */
    RouteShare getShareInfo(String shareCode);

    /**
     * 访问分享文件
     */
    String accessShareFile(String shareCode, String password);

    /**
     * 获取用户分享列表（分页）
     */
    List<RouteShare> getUserShares(Integer userId, int page, int size);

    /**
     * 更新分享设置
     */
    boolean updateShareSettings(Long id, Integer userId, Map<String, Object> settings);

    /**
     * 批量取消分享
     */
    int batchCancelShares(List<Long> ids, Integer userId);

    /**
     * 获取热门分享
     */
    List<RouteShare> getPopularShares(int limit);

    /**
     * 访问分享路线
     */
    Map<String, Object> accessShareRoute(String shareCode);
}
