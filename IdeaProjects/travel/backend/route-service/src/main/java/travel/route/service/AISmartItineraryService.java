package travel.route.service;

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
}
