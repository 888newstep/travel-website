package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.RouteShare;

import java.util.List;
import java.util.Map;

/**
 * 路线分享服务接口
 */
public interface RouteShareService extends IService<RouteShare> {

    /**
     * 创建路线分享
     * @param routeId 路线ID
     * @param userId 用户ID
     * @param shareTitle 分享标题
     * @param shareDescription 分享描述
     * @param expireDays 过期天数
     * @return 路线分享对象
     */
    RouteShare createShare(Integer routeId, Integer userId, String shareTitle, String shareDescription, Integer expireDays);

    /**
     * 根据分享码获取路线分享
     * @param shareCode 分享码
     * @return 路线分享对象
     */
    RouteShare getByShareCode(String shareCode);

    /**
     * 增加分享访问次数
     * @param shareId 分享ID
     * @return 是否成功
     */
    boolean incrementVisitCount(Integer shareId);

    /**
     * 获取用户的分享记录
     * @param userId 用户ID
     * @return 分享记录列表
     */
    List<RouteShare> getUserShares(Integer userId);

    /**
     * 取消分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean cancelShare(Integer shareId, Integer userId);

    /**
     * 获取分享统计信息
     * @param shareId 分享ID
     * @return 统计信息
     */
    Map<String, Object> getShareStatistics(Long shareId);

    // 以下是Controller中使用的方法

    /**
     * 创建路线分享
     * @param routeShare 路线分享对象
     * @return 创建的路线分享
     */
    RouteShare createRouteShare(RouteShare routeShare);

    /**
     * 获取分享信息
     * @param shareCode 分享码
     * @return 路线分享
     */
    RouteShare getShareInfo(String shareCode);

    /**
     * 访问分享文件
     * @param shareCode 分享码
     * @param password 密码
     * @return 访问URL
     */
    String accessShareFile(String shareCode, String password);

    /**
     * 获取用户分享列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 分享列表
     */
    List<RouteShare> getUserShares(Integer userId, int page, int size);

    /**
     * 取消分享
     * @param id 分享ID
     * @return 是否成功
     */
    boolean cancelShare(Long id);

    /**
     * 更新分享设置
     * @param id 分享ID
     * @param settings 设置
     * @return 是否成功
     */
    boolean updateShareSettings(Long id, Map<String, Object> settings);

    /**
     * 批量取消分享
     * @param ids 分享ID列表
     * @return 取消数量
     */
    int batchCancelShares(List<Long> ids);

    /**
     * 获取热门分享
     * @param limit 数量限制
     * @return 热门分享列表
     */
    List<RouteShare> getPopularShares(int limit);

    /**
     * 访问分享路线
     * @param shareCode 分享码
     * @return 路线信息
     */
    Map<String, Object> accessShareRoute(String shareCode);

    /**
     * 增加访问次数
     * @param shareCode 分享码
     * @return 是否成功
     */
    boolean increaseVisitCount(String shareCode);

    /**
     * 清理过期分享
     * @return 清理数量
     */
    int cleanExpiredShares();
}
