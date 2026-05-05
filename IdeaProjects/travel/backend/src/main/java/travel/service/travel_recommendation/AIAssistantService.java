package travel.service.travel_recommendation;

import java.util.List;
import java.util.Map;

/**
 * AI智能助手服务
 * 提供智能问答、路线推荐、行程优化等功能
 */
public interface AIAssistantService {

    /**
     * 智能问答
     * @param question 用户问题
     * @param userId 用户ID
     * @return AI回答
     */
    Map<String, Object> askQuestion(String question, Integer userId);

    /**
     * 智能路线推荐
     * @param userInput 用户输入（需求描述）
     * @param userId 用户ID
     * @return 推荐路线
     */
    List<Map<String, Object>> recommendByAI(String userInput, Integer userId);

    /**
     * 行程优化建议
     * @param routeId 路线ID
     * @return 优化建议
     */
    Map<String, Object> optimizeRouteByAI(Integer routeId);

    /**
     * 景点智能介绍
     * @param attractionId 景点ID
     * @return 智能介绍内容
     */
    Map<String, Object> getAttractionIntro(Integer attractionId);

    /**
     * 智能翻译
     * @param text 待翻译文本
     * @param targetLanguage 目标语言
     * @return 翻译结果
     */
    Map<String, Object> translate(String text, String targetLanguage);

    /**
     * 语音转文字
     * @param audioData 音频数据
     * @return 文字内容
     */
    Map<String, Object> speechToText(byte[] audioData);

    /**
     * 文字转语音
     * @param text 文字内容
     * @return 音频数据
     */
    byte[] textToSpeech(String text);

    /**
     * 智能客服
     * @param message 用户消息
     * @param sessionId 会话ID
     * @return 客服回复
     */
    Map<String, Object> chatWithCustomerService(String message, String sessionId);

    /**
     * 生成旅行日记
     * @param routeId 路线ID
     * @param photos 照片列表
     * @return 生成的日记内容
     */
    Map<String, Object> generateTravelDiary(Integer routeId, List<String> photos);

    /**
     * 智能拍照建议
     * @param attractionId 景点ID
     * @return 拍照建议
     */
    Map<String, Object> getPhotoTips(Integer attractionId);

    /**
     * 实时语音导游
     * @param attractionId 景点ID
     * @param userLocation 用户位置
     * @return 语音讲解
     */
    Map<String, Object> getAudioGuide(Integer attractionId, Map<String, Double> userLocation);

    /**
     * 智能行程总结
     * @param routeId 路线ID
     * @return 行程总结
     */
    Map<String, Object> summarizeTrip(Integer routeId);

    /**
     * 预测最佳出行时间
     * @param cityId 城市ID
     * @param month 月份
     * @return 预测结果
     */
    Map<String, Object> predictBestTime(Integer cityId, Integer month);

    /**
     * 智能 packing 清单
     * @param routeId 路线ID
     * @param weather 天气预报
     * @return packing 清单
     */
    List<Map<String, Object>> generatePackingList(Integer routeId, Map<String, Object> weather);

    /**
     * 情感分析
     * @param text 用户评论或反馈
     * @return 情感分析结果
     */
    Map<String, Object> analyzeSentiment(String text);

    /**
     * 智能标签生成
     * @param content 内容
     * @return 生成的标签
     */
    List<String> generateTags(String content);
}
