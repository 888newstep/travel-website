package travel.service.route_planning;

import travel.entity.route_planning.Route;
import travel.entity.travel_recommendation.Attraction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 智能路线生成服务
 * 根据人数、时间、预算自动规划每日行程
 */
public interface SmartRouteGeneratorService {

    /**
     * 智能生成路线
     * @param cityId 城市ID
     * @param travelerCount 旅行人数
     * @param days 旅行天数
     * @param budget 预算
     * @param preferences 偏好设置
     * @return 生成的路线详情
     */
    Map<String, Object> generateSmartRoute(Integer cityId, Integer travelerCount, 
                                          Integer days, BigDecimal budget, 
                                          Map<String, Object> preferences);

    /**
     * 根据预算推荐最优方案
     * @param cityId 城市ID
     * @param days 天数
     * @param budget 预算
     * @param travelerCount 人数
     * @return 推荐方案列表
     */
    List<Map<String, Object>> recommendBudgetPlans(Integer cityId, Integer days, 
                                                   BigDecimal budget, Integer travelerCount);

    /**
     * 智能分配每日行程
     * @param attractions 景点列表
     * @param days 天数
     * @param dailyTimeLimit 每日时间限制（小时）
     * @return 每日行程分配
     */
    List<List<Attraction>> distributeDailySchedule(List<Attraction> attractions, 
                                                   Integer days, Double dailyTimeLimit);

    /**
     * 计算路线总费用
     * @param routeId 路线ID
     * @param travelerCount 人数
     * @return 费用明细
     */
    Map<String, BigDecimal> calculateRouteCost(Integer routeId, Integer travelerCount);

    /**
     * 优化路线时间分配
     * @param routeId 路线ID
     * @return 优化后的路线
     */
    Route optimizeTimeAllocation(Integer routeId);

    /**
     * 获取智能推荐参数
     * @param cityId 城市ID
     * @return 推荐参数配置
     */
    Map<String, Object> getSmartRecommendationParams(Integer cityId);
}
