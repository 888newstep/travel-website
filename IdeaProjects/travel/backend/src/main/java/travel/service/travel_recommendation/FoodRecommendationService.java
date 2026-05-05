package travel.service.travel_recommendation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 美食推荐服务
 * 集成餐饮特色与美食体验
 */
public interface FoodRecommendationService {

    /**
     * 获取景点周边美食推荐
     * @param attractionId 景点ID
     * @param foodType 美食类型 (local/snack/restaurant)
     * @param budget 预算限制
     * @return 美食推荐列表
     */
    List<Map<String, Object>> getNearbyFood(Integer attractionId, String foodType, BigDecimal budget);

    /**
     * 获取城市特色美食
     * @param cityId 城市ID
     * @return 特色美食列表
     */
    List<Map<String, Object>> getCitySpecialties(Integer cityId);

    /**
     * 根据路线推荐餐厅
     * @param routeId 路线ID
     * @param mealTime 用餐时间 (breakfast/lunch/dinner)
     * @return 餐厅推荐列表
     */
    List<Map<String, Object>> recommendRestaurantsByRoute(Integer routeId, String mealTime);

    /**
     * 获取美食路线规划
     * @param cityId 城市ID
     * @param days 天数
     * @param foodPreference 美食偏好
     * @return 美食主题路线
     */
    Map<String, Object> planFoodRoute(Integer cityId, Integer days, List<String> foodPreference);

    /**
     * 获取餐厅详情
     * @param restaurantId 餐厅ID
     * @return 餐厅详情
     */
    Map<String, Object> getRestaurantDetail(Integer restaurantId);

    /**
     * 获取美食评分
     * @param restaurantId 餐厅ID
     * @return 评分信息
     */
    Map<String, Object> getFoodRating(Integer restaurantId);

    /**
     * 根据预算筛选餐厅
     * @param cityId 城市ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 餐厅列表
     */
    List<Map<String, Object>> filterRestaurantsByPrice(Integer cityId, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取必吃榜单
     * @param cityId 城市ID
     * @param limit 数量限制
     * @return 必吃美食列表
     */
    List<Map<String, Object>> getMustTryFoods(Integer cityId, Integer limit);

    /**
     * 获取美食地图数据
     * @param cityId 城市ID
     * @return 美食地图标记点
     */
    List<Map<String, Object>> getFoodMapData(Integer cityId);

    /**
     * 推荐美食搭配
     * @param attractionId 景点ID
     * @return 美食搭配建议
     */
    List<Map<String, Object>> recommendFoodPairing(Integer attractionId);

    /**
     * 获取用户美食偏好分析
     * @param userId 用户ID
     * @return 美食偏好
     */
    Map<String, Object> analyzeFoodPreference(Integer userId);
}
