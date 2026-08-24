package travel.route.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.route_planning.Route;
import travel.common.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * 路线核心服务接口
 */
public interface RouteService extends IService<Route> {

    /**
     * 根据ID查询路线
     * @param id 路线ID
     * @return 路线信息
     */
    Route getById(Integer id);

    /**
     * 根据用户ID查询我的路线
     * @param userId 用户ID
     * @return 路线列表
     */
    List<Route> getMyRoutes(Long userId);

    /**
     * 校验路线归属（确保用户只能操作自己的路线）
     * @param routeId 路线ID
     * @param userId 用户ID
     * @throws BusinessException 无权限时抛出异常
     */
    void checkRouteOwner(Long routeId, Long userId);

    /**
     * 根据路线标题模糊查询路线
     * @param title 路线标题
     * @return 路线列表
     */
    List<Route> searchRoutesByTitle(String title);

    /**
     * 获取用户创建的路线数量
     * @param userId 用户ID
     * @return 路线数量
     */
    int getUserRouteCount(Long userId);

    /**
     * 根据城市ID查询路线
     * @param cityId 城市ID
     * @return 路线列表
     */
    List<Route> getByCityId(Integer cityId);

    /**
     * 保存路线
     * @param route 路线信息
     * @return 是否保存成功
     */
    boolean save(Route route);

    /**
     * 更新路线
     * @param route 路线信息
     * @return 是否更新成功
     */
    boolean updateById(Route route);

    /**
     * 根据ID删除路线
     * @param id 路线ID
     * @return 是否删除成功
     */
    boolean removeById(Integer id);

    /**
     * 根据ID列表批量查询路线
     * @param routeIds 路线ID列表
     * @return 路线列表
     */
    List<Route> listByIds(List<Integer> routeIds);

    /**
     * 获取路线统计信息
     * @return 统计信息
     */
    Map<String, Object> getRouteStatistics();

    /**
     * 根据城市获取路线统计
     * @return 城市路线统计
     */
    List<Map<String, Object>> getRouteStatisticsByCity();

    /**
     * 获取路线完成率
     * @return 完成率信息
     */
    Map<String, Object> getRouteCompletionRate();

    /**
     * 获取路线时长分布
     * @return 时长分布信息
     */
    List<Map<String, Object>> getRouteDurationDistribution();

    /**
     * 获取需要同步的路线
     * @param minutes 分钟数
     * @return 需要同步的路线列表
     */
    List<Map<String, Object>> getRoutesNeedingSync(Integer minutes);

    /**
     * 同步路线状态
     * @param routeIds 路线ID列表
     * @return 同步结果
     */
    Map<String, Object> syncRouteStatus(List<Integer> routeIds);

    /**
     * 获取路线实时状态
     * @param routeId 路线ID
     * @return 实时状态信息
     */
    Map<String, Object> getRouteRealtimeStatus(Integer routeId);

    /**
     * 更新路线实时状态
     * @param routeId 路线ID
     * @param params 更新参数
     * @return 更新结果
     */
    Map<String, Object> updateRouteRealtimeStatus(Integer routeId, Map<String, Object> params);

}
