package travel.service.route_planning;

import java.util.List;
import java.util.Map;

/**
 * 舒适度评分服务
 * 多维度评估路线舒适度
 */
public interface ComfortRatingService {

    /**
     * 计算路线综合舒适度评分
     * @param routeId 路线ID
     * @return 舒适度评分 (0-100)
     */
    Integer calculateComfortScore(Integer routeId);

    /**
     * 获取路线舒适度详情
     * @param routeId 路线ID
     * @return 各维度舒适度详情
     */
    Map<String, Object> getComfortDetails(Integer routeId);

    /**
     * 评估交通舒适度
     * @param routeId 路线ID
     * @return 交通舒适度评分 (0-100)
     */
    Integer evaluateTransportComfort(Integer routeId);

    /**
     * 评估景点拥挤度
     * @param routeId 路线ID
     * @return 拥挤度评分 (0-100, 越高越不拥挤)
     */
    Integer evaluateCrowdLevel(Integer routeId);

    /**
     * 评估行程节奏
     * @param routeId 路线ID
     * @return 行程节奏评分 (0-100)
     */
    Integer evaluatePaceComfort(Integer routeId);

    /**
     * 评估住宿舒适度
     * @param routeId 路线ID
     * @return 住宿舒适度评分 (0-100)
     */
    Integer evaluateAccommodationComfort(Integer routeId);

    /**
     * 评估餐饮质量
     * @param routeId 路线ID
     * @return 餐饮质量评分 (0-100)
     */
    Integer evaluateDiningQuality(Integer routeId);

    /**
     * 获取行李友好度评分
     * @param routeId 路线ID
     * @return 行李友好度 (0-100)
     */
    Integer getLuggageFriendliness(Integer routeId);

    /**
     * 获取无障碍设施评分
     * @param routeId 路线ID
     * @return 无障碍评分 (0-100)
     */
    Integer getAccessibilityScore(Integer routeId);

    /**
     * 比较多条路线舒适度
     * @param routeIds 路线ID列表
     * @return 舒适度对比结果
     */
    List<Map<String, Object>> compareComfort(List<Integer> routeIds);

    /**
     * 获取舒适度优化建议
     * @param routeId 路线ID
     * @return 优化建议列表
     */
    List<Map<String, Object>> getComfortOptimizationSuggestions(Integer routeId);

    /**
     * 根据舒适度筛选路线
     * @param cityId 城市ID
     * @param minScore 最低舒适度分数
     * @return 符合条件的路线列表
     */
    List<Map<String, Object>> filterByComfort(Integer cityId, Integer minScore);

    /**
     * 获取舒适度维度权重配置
     * @return 各维度权重
     */
    Map<String, Double> getComfortDimensionWeights();

    /**
     * 更新舒适度维度权重
     * @param weights 权重配置
     */
    void updateComfortDimensionWeights(Map<String, Double> weights);
}
