package travel.route.service;

import java.util.List;
import java.util.Map;

/**
 * 高级AI功能服务接口
 */
public interface AIAdvancedService {

    /**
     * 个性化推荐
     * @param userId 用户ID
     * @param recommendationType 推荐类型
     * @param limit 推荐数量
     * @return 推荐结果
     */
    List<Map<String, Object>> getPersonalizedRecommendations(Integer userId, String recommendationType, int limit);

    /**
     * 智能路线规划
     * @param preferences 用户偏好
     * @param constraints 约束条件
     * @return 路线规划结果
     */
    Map<String, Object> planRoute(Map<String, Object> preferences, Map<String, Object> constraints);

    /**
     * 生成旅游攻略
     * @param cityId 城市ID
     * @param days 天数
     * @param preferences 用户偏好
     * @return 旅游攻略
     */
    Map<String, Object> generateTravelGuide(Integer cityId, int days, Map<String, Object> preferences);

    /**
     * 旅游预算估算
     * @param cityId 城市ID
     * @param days 天数
     * @param preferences 用户偏好
     * @return 预算估算结果
     */
    Map<String, Object> estimateBudget(Integer cityId, int days, Map<String, Object> preferences);

    /**
     * 获取旅游安全建议
     * @param cityId 城市ID
     * @return 安全建议
     */
    Map<String, Object> getSafetyAdvice(Integer cityId);
}
