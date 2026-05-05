package travel.service.route_planning;

import java.util.List;
import java.util.Map;

/**
 * 应急路线服务
 * 处理突发情况的路线调整
 */
public interface EmergencyRouteService {

    /**
     * 景点闭园应急处理
     * @param routeId 路线ID
     * @param closedAttractionId 闭园景点ID
     * @return 调整后的路线
     */
    Map<String, Object> handleAttractionClosure(Integer routeId, Integer closedAttractionId);

    /**
     * 交通拥堵应急处理
     * @param routeId 路线ID
     * @param congestedSegment 拥堵路段 (fromAttractionId-toAttractionId)
     * @return 替代路线方案
     */
    Map<String, Object> handleTrafficCongestion(Integer routeId, String congestedSegment);

    /**
     * 恶劣天气应急处理
     * @param routeId 路线ID
     * @param weatherAlert 天气预警信息
     * @return 室内替代方案
     */
    Map<String, Object> handleSevereWeather(Integer routeId, Map<String, Object> weatherAlert);

    /**
     * 用户身体不适应急处理
     * @param routeId 路线ID
     * @param currentLocation 当前位置
     * @return 最近医疗点及路线调整
     */
    Map<String, Object> handleHealthEmergency(Integer routeId, Map<String, Double> currentLocation);

    /**
     * 生成应急联系信息
     * @param routeId 路线ID
     * @return 应急联系信息
     */
    Map<String, Object> generateEmergencyContacts(Integer routeId);

    /**
     * 获取最近服务点
     * @param latitude 纬度
     * @param longitude 经度
     * @param serviceType 服务类型 (hospital/police/pharmacy)
     * @return 最近服务点列表
     */
    List<Map<String, Object>> getNearestServices(Double latitude, Double longitude, String serviceType);

    /**
     * 一键求助
     * @param userId 用户ID
     * @param routeId 路线ID
     * @param emergencyType 紧急情况类型
     * @param location 当前位置
     * @return 求助结果
     */
    Map<String, Object> emergencySOS(Integer userId, Integer routeId, String emergencyType, Map<String, Double> location);

    /**
     * 获取实时安全提示
     * @param routeId 路线ID
     * @return 安全提示列表
     */
    List<Map<String, Object>> getRealtimeSafetyTips(Integer routeId);

    /**
     * 评估路线安全风险
     * @param routeId 路线ID
     * @return 风险评估报告
     */
    Map<String, Object> assessRouteRisk(Integer routeId);

    /**
     * 生成应急备案路线
     * @param routeId 路线ID
     * @return 多条备选路线
     */
    List<Map<String, Object>> generateBackupRoutes(Integer routeId);

    /**
     * 推送应急通知
     * @param routeId 路线ID
     * @param alertType 预警类型
     * @param message 通知内容
     * @return 推送结果
     */
    boolean pushEmergencyAlert(Integer routeId, String alertType, String message);

    /**
     * 获取应急物资点
     * @param latitude 纬度
     * @param longitude 经度
     * @return 应急物资点列表
     */
    List<Map<String, Object>> getEmergencySupplyPoints(Double latitude, Double longitude);

    /**
     * 记录应急事件
     * @param userId 用户ID
     * @param routeId 路线ID
     * @param eventType 事件类型
     * @param description 事件描述
     * @return 记录结果
     */
    boolean logEmergencyEvent(Integer userId, Integer routeId, String eventType, String description);
}
