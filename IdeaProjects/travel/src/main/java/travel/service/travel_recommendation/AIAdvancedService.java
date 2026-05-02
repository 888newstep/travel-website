package travel.service.travel_recommendation;

import java.util.List;
import java.util.Map;

/**
 * 高级AI功能服务接口
 */
public interface AIAdvancedService {

    /**
     * AI聊天功能
     * @param message 用户消息
     * @param sessionId 会话ID
     * @return 聊天响应
     */
    Map<String, Object> chatWithAI(String message, String sessionId);

    /**
     * 个性化推荐
     * @param userId 用户ID
     * @param recommendationType 推荐类型
     * @param limit 推荐数量
     * @return 推荐结果
     */
    List<Map<String, Object>> getPersonalizedRecommendations(Integer userId, String recommendationType, int limit);

    /**
     * 处理语音请求
     * @param audioData 音频数据
     * @return 语音处理结果
     */
    Map<String, Object> processVoiceRequest(byte[] audioData);

    /**
     * 分析图像
     * @param imageData 图像数据
     * @param analysisType 分析类型
     * @return 图像分析结果
     */
    Map<String, Object> analyzeImage(byte[] imageData, String analysisType);

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
     * 多语言翻译
     * @param text 文本
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 翻译结果
     */
    Map<String, Object> translate(String text, String sourceLanguage, String targetLanguage);

    /**
     * 情感分析
     * @param text 文本
     * @return 情感分析结果
     */
    Map<String, Object> analyzeSentiment(String text);

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

    /**
     * 回答旅游相关问题
     * @param question 问题
     * @return 回答
     */
    Map<String, Object> answerQuestion(String question);

    /**
     * 增强景点识别
     * @param imageData 图像数据
     * @param location 位置信息
     * @return 景点识别结果
     */
    Map<String, Object> enhancedAttractionRecognition(byte[] imageData, Map<String, Double> location);

    /**
     * 智能行程优化
     * @param itinerary 现有行程
     * @param preferences 用户偏好
     * @param constraints 约束条件
     * @return 优化后的行程
     */
    Map<String, Object> optimizeItinerary(Map<String, Object> itinerary, Map<String, Object> preferences, Map<String, Object> constraints);

    /**
     * 增强智能问答
     * @param question 问题
     * @param context 上下文信息
     * @param userId 用户ID
     * @return 增强的回答结果
     */
    Map<String, Object> enhancedQuestionAnswering(String question, Map<String, Object> context, Integer userId);

    /**
     * 个性化旅游建议
     * @param userId 用户ID
     * @param tripType 旅行类型
     * @param duration 旅行时长
     * @param budget 预算
     * @return 个性化建议
     */
    Map<String, Object> getPersonalizedTravelAdvice(Integer userId, String tripType, int duration, double budget);

    /**
     * 旅游热点分析
     * @param region 区域
     * @param timeRange 时间范围
     * @return 热点分析结果
     */
    Map<String, Object> analyzeTravelHotspots(String region, Map<String, String> timeRange);

    /**
     * 多模态交互
     * @param requestData 多模态请求数据
     * @param sessionId 会话ID
     * @return 多模态响应
     */
    Map<String, Object> multimodalInteraction(Map<String, Object> requestData, String sessionId);
}
