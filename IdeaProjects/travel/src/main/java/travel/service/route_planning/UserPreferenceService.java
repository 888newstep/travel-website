package travel.service.route_planning;

import java.util.List;
import java.util.Map;

/**
 * 用户偏好服务
 * 基于NLP和机器学习构建用户偏好知识库
 */
public interface UserPreferenceService {

    /**
     * 记录用户偏好
     * @param userId 用户ID
     * @param preferenceType 偏好类型 (transport/budget/comfort/culture/food)
     * @param preferenceValue 偏好值
     */
    void recordPreference(Integer userId, String preferenceType, String preferenceValue);

    /**
     * 分析用户历史行为生成偏好画像
     * @param userId 用户ID
     * @return 用户偏好画像
     */
    Map<String, Object> analyzeUserProfile(Integer userId);

    /**
     * 基于NLP分析用户评价提取偏好
     * @param reviewContent 评价内容
     * @return 提取的偏好关键词
     */
    List<String> extractPreferencesFromReview(String reviewContent);

    /**
     * 获取用户交通方式偏好
     * @param userId 用户ID
     * @return 交通偏好权重 Map<transportType, weight>
     */
    Map<String, Double> getTransportPreference(Integer userId);

    /**
     * 获取用户预算敏感度
     * @param userId 用户ID
     * @return 预算敏感度 (0-1, 越高越敏感)
     */
    Double getBudgetSensitivity(Integer userId);

    /**
     * 获取用户舒适度要求
     * @param userId 用户ID
     * @return 舒适度要求等级 (economy/standard/luxury)
     */
    String getComfortLevel(Integer userId);

    /**
     * 获取用户文化偏好
     * @param userId 用户ID
     * @return 文化偏好标签列表
     */
    List<String> getCulturePreferences(Integer userId);

    /**
     * 获取用户美食偏好
     * @param userId 用户ID
     * @return 美食偏好标签列表
     */
    List<String> getFoodPreferences(Integer userId);

    /**
     * 基于用户画像推荐路线
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param days 天数
     * @return 个性化推荐路线列表
     */
    List<Map<String, Object>> recommendRoutesByProfile(Integer userId, Integer cityId, Integer days);

    /**
     * 更新用户偏好模型
     * @param userId 用户ID
     * @param routeId 路线ID
     * @param rating 评分
     * @param review 评价内容
     */
    void updatePreferenceModel(Integer userId, Integer routeId, Integer rating, String review);

    /**
     * 获取相似用户群体
     * @param userId 用户ID
     * @return 相似用户ID列表
     */
    List<Integer> getSimilarUsers(Integer userId);

    /**
     * 基于协同过滤推荐
     * @param userId 用户ID
     * @return 推荐路线列表
     */
    List<Map<String, Object>> collaborativeFilteringRecommend(Integer userId);

    /**
     * 获取用户旅程类型偏好
     * @param userId 用户ID
     * @return 旅程类型 (leisure/business/family/couple/adventure)
     */
    String getTripTypePreference(Integer userId);

    /**
     * 计算路线与用户偏好匹配度
     * @param routeId 路线ID
     * @param userId 用户ID
     * @return 匹配度分数 (0-100)
     */
    Integer calculateMatchScore(Integer routeId, Integer userId);
}
