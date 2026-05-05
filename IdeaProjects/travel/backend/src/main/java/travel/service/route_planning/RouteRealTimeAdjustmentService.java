package travel.service.route_planning;

import travel.entity.vo.AdjustmentSuggestionVO;
import travel.entity.vo.RouteAdjustmentVO;

import java.util.List;
import java.util.Map;

/**
 * 路线实时调整服务接口
 */
public interface RouteRealTimeAdjustmentService {

    /**
     * 获取实时路线调整建议
     * @param request 请求参数
     * @return 调整建议
     */
    Map<String, Object> getRealTimeAdjustment(Map<String, Object> request);

    /**
     * 应用路线调整
     * @param adjustmentData 调整数据
     * @return 应用结果
     */
    boolean applyRouteAdjustment(Map<String, Object> adjustmentData);

    /**
     * 获取交通状况数据
     * @param location 位置
     * @param route 路线
     * @return 交通状况
     */
    Map<String, Object> getTrafficConditions(String location, String route);

    /**
     * 获取天气影响评估
     * @param location 位置
     * @param route 路线
     * @return 天气影响评估
     */
    Map<String, Object> getWeatherImpact(String location, String route);

    /**
     * 获取实时拥堵预警
     * @param route 路线
     * @return 拥堵预警
     */
    List<Map<String, Object>> getCongestionAlerts(String route);

    /**
     * 获取替代路线建议
     * @param request 请求参数
     * @return 替代路线
     */
    List<Map<String, Object>> getAlternativeRoutes(Map<String, Object> request);

    /**
     * 更新实时位置
     * @param locationData 位置数据
     * @return 更新结果
     */
    boolean updateRealTimeLocation(Map<String, Object> locationData);

    /**
     * 获取预计到达时间
     * @param routeId 路线ID
     * @param currentDistance 当前距离
     * @return 预计到达时间
     */
    Map<String, Object> getEstimatedArrivalTime(Integer routeId, Double currentDistance);

    /**
     * 获取调整历史记录
     * @param routeId 路线ID
     * @return 调整历史
     */
    List<Map<String, Object>> getAdjustmentHistory(Integer routeId);

    // 以下是实现类中额外的方法

    /**
     * 获取调整建议
     * @param routeId 路线ID
     * @param currentPoint 当前点
     * @param userId 用户ID
     * @return 调整建议
     */
    AdjustmentSuggestionVO getAdjustmentSuggestion(Long routeId, String currentPoint, Long userId);

    /**
     * 执行路线调整
     * @param routeId 路线ID
     * @param adjustmentVO 调整参数
     * @param userId 用户ID
     * @return 调整后的路线
     */
    RouteAdjustmentVO adjustRoute(Long routeId, RouteAdjustmentVO adjustmentVO, Long userId);

    /**
     * 获取实时交通信息
     * @param routeId 路线ID
     * @return 实时交通信息
     */
    Map<String, Object> getRealTimeTrafficInfo(Long routeId);

    /**
     * 获取景点实时状态
     * @param attractionIds 景点ID列表
     * @return 景点实时状态
     */
    Map<Long, Map<String, Object>> getRealTimeAttractionStatus(List<Long> attractionIds);

    /**
     * 预测路线拥堵情况
     * @param routeId 路线ID
     * @param departureTime 出发时间
     * @return 拥堵预测
     */
    Map<String, Object> predictRouteCongestion(Long routeId, String departureTime);

    /**
     * 获取备选路线
     * @param routeId 路线ID
     * @param currentPoint 当前点
     * @param userId 用户ID
     * @return 备选路线列表
     */
    List<RouteAdjustmentVO> getAlternativeRoutes(Long routeId, String currentPoint, Long userId);

    /**
     * 保存调整历史
     * @param routeId 路线ID
     * @param adjustmentVO 调整参数
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean saveAdjustmentHistory(Long routeId, RouteAdjustmentVO adjustmentVO, Long userId);

    /**
     * 获取调整历史
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 调整历史列表
     */
    List<Map<String, Object>> getAdjustmentHistory(Long routeId, Long userId);
}
