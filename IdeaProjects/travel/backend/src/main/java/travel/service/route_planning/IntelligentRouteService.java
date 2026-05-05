package travel.service.route_planning;

import java.util.List;
import java.util.Map;

public interface IntelligentRouteService {

    /**
     * 基于用户偏好的智能路线推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param days 天数
     * @param preferences 用户偏好
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesByUserPreference(Integer userId, Integer cityId, int days, Map<String, Object> preferences);

    /**
     * 多维度路线比较
     * @param routeIds 路线ID列表
     * @return 比较结果
     */
    Map<String, Object> compareRoutes(List<Integer> routeIds);

    /**
     * 实时路线调整建议
     * @param routeId 路线ID
     * @param currentLocation 当前位置
     * @param realTimeFactors 实时因素
     * @return 调整建议
     */
    Map<String, Object> getRealTimeAdjustment(Integer routeId, Map<String, Double> currentLocation, Map<String, Object> realTimeFactors);

    /**
     * 路线质量评估
     * @param routeId 路线ID
     * @param evaluationParams 评估参数
     * @return 评估结果
     */
    Map<String, Object> evaluateRouteQuality(Integer routeId, Map<String, Object> evaluationParams);

    /**
     * 个性化路线生成
     * @param userPreferences 用户偏好
     * @param constraints 约束条件
     * @return 生成的路线
     */
    Map<String, Object> generatePersonalizedRoute(Map<String, Object> userPreferences, Map<String, Object> constraints);

    /**
     * 热门路线推荐
     * @param cityId 城市ID
     * @param days 天数
     * @param limit 数量限制
     * @return 热门路线列表
     */
    List<Map<String, Object>> getPopularRoutes(Integer cityId, int days, int limit);

    /**
     * 相似路线推荐
     * @param routeId 路线ID
     * @param limit 数量限制
     * @return 相似路线列表
     */
    List<Map<String, Object>> getSimilarRoutes(Integer routeId, int limit);

    /**
     * 季节性路线推荐
     * @param cityId 城市ID
     * @param season 季节
     * @param days 天数
     * @return 季节性路线列表
     */
    List<Map<String, Object>> getSeasonalRoutes(Integer cityId, String season, int days);

    /**
     * 主题路线推荐
     * @param theme 主题
     * @param cityId 城市ID
     * @param days 天数
     * @return 主题路线列表
     */
    List<Map<String, Object>> getThemeRoutes(String theme, Integer cityId, int days);

    /**
     * 路线优化建议
     * @param routeId 路线ID
     * @param optimizationType 优化类型
     * @return 优化建议
     */
    Map<String, Object> getRouteOptimizationSuggestions(Integer routeId, String optimizationType);

    /**
     * 基于用户历史行为的路线推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param days 天数
     * @param limit 数量限制
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesByUserHistory(Integer userId, Integer cityId, int days, int limit);

    /**
     * 基于热门景点的路线推荐
     * @param cityId 城市ID
     * @param days 天数
     * @param limit 数量限制
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesByPopularAttractions(Integer cityId, int days, int limit);

    /**
     * 基于季节和天气的路线推荐
     * @param cityId 城市ID
     * @param days 天数
     * @param season 季节
     * @param weather 天气
     * @param limit 数量限制
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesBySeasonAndWeather(Integer cityId, int days, String season, String weather, int limit);

    /**
     * 基于用户社交网络的路线推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param days 天数
     * @param limit 数量限制
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesBySocialNetwork(Integer userId, Integer cityId, int days, int limit);

    /**
     * 基于路线评分的推荐
     * @param cityId 城市ID
     * @param days 天数
     * @param limit 数量限制
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesByRating(Integer cityId, int days, int limit);

    /**
     * 基于路线相似度的推荐
     * @param routeId 路线ID
     * @param limit 数量限制
     * @return 推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesBySimilarity(Integer routeId, int limit);

    /**
     * 生成多日游路线
     * @param cityId 城市ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param userPreferences 用户偏好
     * @return 多日游路线
     */
    Map<String, Object> generateMultiDayRoute(Integer cityId, String startDate, String endDate, Map<String, Object> userPreferences);

    /**
     * 获取路线推荐理由
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 推荐理由
     */
    Map<String, Object> getRouteRecommendationReason(Integer routeId, Integer userId);
}
