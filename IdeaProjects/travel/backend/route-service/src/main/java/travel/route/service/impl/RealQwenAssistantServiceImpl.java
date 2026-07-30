package travel.route.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.route.service.RouteService;
import travel.route.service.AIAssistantService;
import travel.route.service.AttractionService;
import travel.route.service.QwenService;
import travel.common.utils.CacheUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于通义千问的真实AI助手服务实现
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class RealQwenAssistantServiceImpl implements AIAssistantService {

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final CacheUtil cacheUtil;
    private final QwenService qwenService;

    private static final String AI_PREFIX = "ai:";

    @Override
    public Map<String, Object> askQuestion(String question, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        String cacheKey = AI_PREFIX + "qa:" + question.hashCode();

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = cacheUtil.get(cacheKey, Map.class);
        if (cached != null) {
            log.info("从缓存获取AI回答: question={}", question);
            return cached;
        }

        try {
            String answer = qwenService.travelQA(question);

            result.put("question", question);
            result.put("answer", answer);
            result.put("confidence", 0.95);
            result.put("timestamp", LocalDateTime.now());
            result.put("source", "qwen");

            cacheUtil.set(cacheKey, result, 60, TimeUnit.MINUTES);
            log.info("AI问答成功: question={}", question);

        } catch (Exception e) {
            log.error("AI问答失败，使用降级方案: error={}", e.getMessage());
            result = getFallbackAnswer(question);
        }

        return result;
    }

    @Override
    public Map<String, Object> optimizeRouteByAI(Integer routeId) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        try {
            String prompt = "请优化以下旅游路线，给出具体的改进建议：\n" +
                    "路线名称：" + route.getTitle() + "\n" +
                    "城市：" + (route.getCity() != null ? route.getCity().getName() : "未知");

            String suggestions = qwenService.chatCompletion(prompt,
                    "你是一个专业的旅行规划师，擅长优化旅行路线。请给出具体的、可执行的优化建议。");

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("suggestions", suggestions);
            result.put("optimizedScore", 88);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("AI路线优化失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "优化失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getAttractionIntro(Integer attractionId) {
        Map<String, Object> result = new HashMap<>();

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            result.put("success", false);
            result.put("message", "景点不存在");
            return result;
        }

        String cacheKey = AI_PREFIX + "intro:" + attractionId;

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = cacheUtil.get(cacheKey, Map.class);
        if (cached != null) {
            return cached;
        }

        try {
            String intro = qwenService.generateAttractionIntro(
                    attraction.getName(),
                    attraction.getAddress() != null ? attraction.getAddress() : "未知"
            );

            result.put("attractionId", attractionId);
            result.put("name", attraction.getName());
            result.put("briefIntro", intro);
            result.put("detailedIntro", intro);
            result.put("funFacts", "暂无趣闻");
            result.put("bestVisitTime", "建议游览时间：上午9:00-11:00");
            result.put("estimatedDuration", "2-3小时");
            result.put("source", "qwen");

            cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("生成景点介绍失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "生成介绍失败");
        }

        return result;
    }

    /**
     * 降级方案
     */
    private Map<String, Object> getFallbackAnswer(String question) {
        Map<String, Object> result = new HashMap<>();
        String answer;

        if (question.contains("天气")) {
            answer = "建议您查看当地天气预报，或下载天气APP获取实时信息。一般来说，春秋季节是最适合旅游的时期。";
        } else if (question.contains("门票")) {
            answer = "大部分景点门票可以在官方网站或第三方平台（如携程、美团）预订，建议提前购买以避免排队。部分景点有学生票、老人票等优惠。";
        } else if (question.contains("交通")) {
            answer = "城市内建议使用地铁和公交出行，经济实惠且避免拥堵。跨城市可选择高铁或飞机，提前预订可享受优惠价格。";
        } else if (question.contains("住宿")) {
            answer = "推荐住在市中心或景区附近，交通便利。可以通过携程、Booking等平台预订，注意查看评价和位置。";
        } else if (question.contains("美食") || question.contains("吃")) {
            answer = "每个地方都有特色美食，建议尝试当地老字号餐厅。也可以询问当地人推荐，往往能找到地道的美食。";
        } else {
            answer = "感谢您的提问！作为旅游助手，我可以为您提供景点推荐、行程规划、交通住宿建议等服务。如果您能提供更具体的需求，我会给出更有针对性的建议。";
        }

        result.put("question", question);
        result.put("answer", answer);
        result.put("confidence", 0.70);
        result.put("timestamp", LocalDateTime.now());
        result.put("source", "fallback");

        return result;
    }
}
