package travel.service.travel_recommendation;

import java.util.List;
import java.util.Map;

/**
 * AI智能行程规划服务接口
 */
public interface AISmartItineraryService {

    /**
     * 生成智能行程
     * @param userPreferences 用户偏好
     * @param budget 预算
     * @param days 天数
     * @param cityId 城市ID
     * @param userId 用户ID
     * @return 智能行程计划
     */
    Map<String, Object> generateItinerary(Map<String, Object> userPreferences, double budget, int days, Integer cityId, Integer userId);

    /**
     * 优化现有行程
     * @param routeId 路线ID
     * @param userPreferences 用户偏好
     * @return 优化后的行程
     */
    Map<String, Object> optimizeItinerary(Integer routeId, Map<String, Object> userPreferences);

    /**
     * 根据实时数据调整行程
     * @param routeId 路线ID
     * @param realTimeData 实时数据
     * @return 调整后的行程
     */
    Map<String, Object> adjustItinerary(Integer routeId, Map<String, Object> realTimeData);

    /**
     * 生成备选行程
     * @param routeId 路线ID
     * @param count 备选数量
     * @return 备选行程列表
     */
    List<Map<String, Object>> generateAlternatives(Integer routeId, int count);

    /**
     * 预测行程满意度
     * @param itinerary 行程计划
     * @param userId 用户ID
     * @return 满意度预测
     */
    Map<String, Object> predictSatisfaction(Map<String, Object> itinerary, Integer userId);
}
