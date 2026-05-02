package travel.service.route_planning;

import java.util.List;
import java.util.Map;

/**
 * 实时动态调整服务
 * 根据天气/人流/交通状况自动优化路线
 */
public interface RealtimeRouteAdjustmentService {

    /**
     * 实时监控路线状况
     * @param routeId 路线ID
     * @return 路线实时状况
     */
    Map<String, Object> monitorRouteStatus(Integer routeId);

    /**
     * 根据天气调整路线
     * @param routeId 路线ID
     * @param weatherCondition 天气状况
     * @return 调整建议
     */
    Map<String, Object> adjustRouteByWeather(Integer routeId, Map<String, Object> weatherCondition);

    /**
     * 根据人流调整路线
     * @param routeId 路线ID
     * @param crowdData 人流数据
     * @return 调整建议
     */
    Map<String, Object> adjustRouteByCrowd(Integer routeId, Map<String, Object> crowdData);

    /**
     * 根据交通状况调整路线
     * @param routeId 路线ID
     * @param trafficData 交通数据
     * @return 调整建议
     */
    Map<String, Object> adjustRouteByTraffic(Integer routeId, Map<String, Object> trafficData);

    /**
     * 智能路线重新规划
     * @param routeId 路线ID
     * @param realTimeFactors 实时因素
     * @return 重新规划的路线
     */
    Map<String, Object> intelligentReroute(Integer routeId, Map<String, Object> realTimeFactors);

    /**
     * 获取景点实时人流
     * @param attractionId 景点ID
     * @return 人流信息
     */
    Map<String, Object> getAttractionCrowdStatus(Integer attractionId);

    /**
     * 获取天气对景点的影响
     * @param attractionId 景点ID
     * @param weatherType 天气类型
     * @return 影响评估
     */
    Map<String, Object> assessWeatherImpact(Integer attractionId, String weatherType);

    /**
     * 生成备选路线方案
     * @param routeId 路线ID
     * @param reason 调整原因
     * @return 备选方案列表
     */
    List<Map<String, Object>> generateAlternativeRoutes(Integer routeId, String reason);

    /**
     * 推送实时预警通知
     * @param routeId 路线ID
     * @param alertType 预警类型
     * @param message 预警消息
     * @return 推送结果
     */
    boolean pushRealtimeAlert(Integer routeId, String alertType, String message);

    /**
     * 获取实时路况信息
     * @param routeId 路线ID
     * @return 路况信息
     */
    Map<String, Object> getRealtimeRoadCondition(Integer routeId);

    /**
     * 动态调整游览时长
     * @param routeId 路线ID
     * @param attractionId 景点ID
     * @param crowdLevel 人流等级
     * @return 建议游览时长
     */
    Integer adjustVisitDuration(Integer routeId, Integer attractionId, Integer crowdLevel);
}
