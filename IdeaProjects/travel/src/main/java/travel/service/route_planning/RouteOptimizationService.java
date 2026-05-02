package travel.service.route_planning;

import travel.service.route_planning.RoutePlanAlgorithm.OptimalRoute;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RouteOptimizationService {

    /**
     * 规划最优路线
     * @param attractionIds 景点ID列表
     * @param maxDays 最大天数
     * @param budget 预算
     * @param preference 偏好类型
     * @return 最优路线
     */
    OptimalRoute planOptimalRoute(List<Integer> attractionIds, int maxDays, BigDecimal budget, String preference);

    /**
     * 调整现有路线
     * @param routeId 路线ID
     * @param adjustmentType 调整类型
     * @param adjustmentParams 调整参数
     * @return 调整后的路线
     */
    Map<String, Object> adjustRoute(Integer routeId, String adjustmentType, Map<String, Object> adjustmentParams);

    /**
     * 获取路线推荐
     * @param cityId 城市ID
     * @param days 天数
     * @param interests 兴趣标签
     * @param budget 预算
     * @return 推荐路线列表
     */
    List<Map<String, Object>> getRouteRecommendations(Integer cityId, int days, List<String> interests, BigDecimal budget);

    /**
     * 计算路线相似度
     * @param routeId1 路线1 ID
     * @param routeId2 路线2 ID
     * @return 相似度分数
     */
    double calculateRouteSimilarity(Integer routeId1, Integer routeId2);

    /**
     * 评估路线质量
     * @param routeId 路线ID
     * @return 路线评估结果
     */
    Map<String, Object> evaluateRouteQuality(Integer routeId);

    /**
     * 生成路线备选方案
     * @param routeId 原始路线ID
     * @param alternativeCount 备选方案数量
     * @return 备选路线列表
     */
    List<Map<String, Object>> generateRouteAlternatives(Integer routeId, int alternativeCount);

    /**
     * 优化路线交通方式
     * @param routeId 路线ID
     * @param transportPreference 交通方式偏好
     * @return 优化后的路线
     */
    Map<String, Object> optimizeRouteTransport(Integer routeId, String transportPreference);

    /**
     * 预测路线人流量
     * @param routeId 路线ID
     * @param date 日期
     * @return 人流量预测结果
     */
    Map<String, Object> predictRouteCrowd(Integer routeId, String date);

    /**
     * 获取路线详细分析
     * @param routeId 路线ID
     * @return 路线分析结果
     */
    Map<String, Object> getRouteAnalysis(Integer routeId);

    /**
     * 保存用户路线偏好
     * @param userId 用户ID
     * @param preferences 偏好设置
     * @return 保存结果
     */
    boolean saveUserRoutePreferences(Integer userId, Map<String, Object> preferences);

    /**
     * 获取用户个性化路线推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param days 天数
     * @return 个性化推荐路线
     */
    List<Map<String, Object>> getPersonalizedRouteRecommendations(Integer userId, Integer cityId, int days);

    Map<String, Object> optimizeRoute(Map<String, Object> routeData);

    List<Map<String, Object>> getOptimizationSuggestions(Integer routeId);

    Map<String, Object> optimizeTime(Map<String, Object> routeData);

    Map<String, Object> optimizeCost(Map<String, Object> routeData);

    Map<String, Object> optimizeDistance(Map<String, Object> routeData);

    Map<String, Object> getOptimizationComparison(Integer routeId);

    boolean applyOptimization(Map<String, Object> optimizationData);

    List<Map<String, Object>> getOptimizationHistory(Integer routeId);

    Map<String, Object> batchOptimizeRoutes(List<Integer> routeIds);
}