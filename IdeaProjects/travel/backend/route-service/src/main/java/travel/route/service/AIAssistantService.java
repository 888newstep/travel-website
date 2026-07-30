package travel.route.service;

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

}
