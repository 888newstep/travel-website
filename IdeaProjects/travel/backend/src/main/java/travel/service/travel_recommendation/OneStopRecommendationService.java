package travel.service.travel_recommendation;

import java.util.List;
import java.util.Map;

/**
 * 一站式推荐服务
 * 提供综合性的旅游推荐服务
 */
public interface OneStopRecommendationService {

    /**
     * 获取一站式推荐
     * @param request 请求参数
     * @return 推荐结果
     */
    Map<String, Object> getOneStopRecommendation(Map<String, Object> request);

    /**
     * 获取目的地推荐
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 目的地推荐列表
     */
    List<Map<String, Object>> getDestinationRecommendations(Integer userId, int limit);

    /**
     * 获取景点推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param limit 数量限制
     * @return 景点推荐列表
     */
    List<Map<String, Object>> getAttractionRecommendations(Integer userId, Integer cityId, int limit);

    /**
     * 获取路线推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param days 天数
     * @param limit 数量限制
     * @return 路线推荐列表
     */
    List<Map<String, Object>> getRouteRecommendations(Integer userId, Integer cityId, int days, int limit);

    /**
     * 获取住宿推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param limit 数量限制
     * @return 住宿推荐列表
     */
    List<Map<String, Object>> getAccommodationRecommendations(Integer userId, Integer cityId, int limit);

    /**
     * 获取美食推荐
     * @param userId 用户ID
     * @param cityId 城市ID
     * @param limit 数量限制
     * @return 美食推荐列表
     */
    List<Map<String, Object>> getFoodRecommendations(Integer userId, Integer cityId, int limit);

    /**
     * 获取个性化推荐
     * @param preferences 用户偏好
     * @return 个性化推荐结果
     */
    Map<String, Object> getPersonalizedRecommendations(Map<String, Object> preferences);

    /**
     * 获取热门推荐
     * @param cityId 城市ID
     * @param limit 数量限制
     * @return 热门推荐列表
     */
    Map<String, Object> getTrendingRecommendations(Integer cityId, int limit);

    /**
     * 获取季节推荐
     * @param cityId 城市ID
     * @param season 季节
     * @param limit 数量限制
     * @return 季节推荐列表
     */
    Map<String, Object> getSeasonalRecommendations(Integer cityId, String season, int limit);

    /**
     * 获取推荐理由
     * @param request 请求参数
     * @return 推荐理由
     */
    Map<String, String> getRecommendationReason(Map<String, Object> request);

    /**
     * 提交推荐反馈
     * @param feedback 反馈信息
     * @return 是否成功
     */
    boolean submitRecommendationFeedback(Map<String, Object> feedback);

    /**
     * 保存推荐结果
     * @param saveRequest 保存请求
     * @return 是否成功
     */
    boolean saveRecommendations(Map<String, Object> saveRequest);
}
