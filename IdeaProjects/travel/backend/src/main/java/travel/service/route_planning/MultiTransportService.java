package travel.service.route_planning;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 多交通方式组合服务
 * 提供公交/打车/自驾等多种交通方案对比
 */
public interface MultiTransportService {

    /**
     * 获取多交通方式对比方案
     * @param fromAttractionId 出发景点ID
     * @param toAttractionId 目的景点ID
     * @param travelerCount 出行人数
     * @param preference 偏好（cost-成本优先, time-时间优先, comfort-舒适优先）
     * @return 交通方案对比列表
     */
    List<Map<String, Object>> compareTransportOptions(Integer fromAttractionId, Integer toAttractionId, 
                                                      Integer travelerCount, String preference);

    /**
     * 获取最优交通组合方案
     * @param routeId 路线ID
     * @param travelerCount 出行人数
     * @param preference 偏好
     * @return 最优交通组合
     */
    Map<String, Object> getOptimalTransportCombination(Integer routeId, Integer travelerCount, String preference);

    /**
     * 计算交通费用
     * @param transportType 交通类型
     * @param distance 距离（公里）
     * @param travelerCount 人数
     * @return 费用
     */
    BigDecimal calculateTransportCost(String transportType, Double distance, Integer travelerCount);

    /**
     * 估算交通时间
     * @param transportType 交通类型
     * @param distance 距离（公里）
     * @param trafficCondition 交通状况（smooth-畅通, moderate-一般, congested-拥堵）
     * @return 时间（分钟）
     */
    Integer estimateTransportTime(String transportType, Double distance, String trafficCondition);

    /**
     * 获取实时交通状况
     * @param fromLat 出发地纬度
     * @param fromLng 出发地经度
     * @param toLat 目的地纬度
     * @param toLng 目的地经度
     * @return 交通状况信息
     */
    Map<String, Object> getRealtimeTrafficCondition(Double fromLat, Double fromLng, 
                                                    Double toLat, Double toLng);

    /**
     * 推荐最佳出行时间
     * @param fromAttractionId 出发景点ID
     * @param toAttractionId 目的景点ID
     * @param transportType 交通类型
     * @return 最佳出行时间建议
     */
    Map<String, Object> recommendBestDepartureTime(Integer fromAttractionId, Integer toAttractionId, 
                                                   String transportType);

    /**
     * 获取碳排放对比
     * @param transportTypes 交通类型列表
     * @param distance 距离
     * @return 碳排放对比数据
     */
    Map<String, Double> getCarbonEmissionComparison(List<String> transportTypes, Double distance);

    /**
     * 智能交通方式推荐
     * @param distance 距离
     * @param travelerCount 人数
     * @param budget 预算
     * @param timeConstraint 时间限制
     * @return 推荐交通方式
     */
    Map<String, Object> recommendTransportMode(Double distance, Integer travelerCount, 
                                               BigDecimal budget, Integer timeConstraint);
}
