package travel.route.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import travel.common.config.AIConfig;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.route.service.RouteService;
import travel.route.service.AIAssistantService;
import travel.route.service.AttractionService;
import travel.common.utils.CacheUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealAIAssistantServiceImpl implements AIAssistantService {

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final CacheUtil cacheUtil;
    private final AIConfig aiConfig;

    private static final String AI_PREFIX = "ai:";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String answer = callOpenAI(question);

            result.put("question", question);
            result.put("answer", answer);
            result.put("confidence", 0.95);
            result.put("timestamp", LocalDateTime.now());
            result.put("source", "openai");

            cacheUtil.set(cacheKey, result, 60, TimeUnit.MINUTES);
            log.info("AI问答成功: question={}", question);

        } catch (Exception e) {
            log.error("AI问答失败，使用降级方案: error={}", e.getMessage());
            result = getFallbackAnswer(question);
        }

        return result;
    }

    private String callOpenAI(String question) throws IOException {
        if (!aiConfig.getEnabled()) {
            throw new RuntimeException("AI服务未启用");
        }

        String url = aiConfig.getApiUrl() + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getModel());

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的旅游助手，擅长提供旅游建议、景点介绍、行程规划等服务。请用中文回答，回答要简洁明了、实用性强。");
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("temperature", aiConfig.getTemperature());
        requestBody.put("max_tokens", aiConfig.getMaxTokens());

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            return rootNode.path("choices").get(0).path("message").path("content").asText();
        }
    }

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
                    "城市：" + route.getCity().getName();

            String suggestions = callOpenAI(prompt);

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("suggestions", suggestions);
            result.put("optimizedScore", 90);
            result.put("source", "openai");

        } catch (Exception e) {
            log.error("AI路线优化失败: error={}", e.getMessage());

            List<Map<String, Object>> fallbackSuggestions = new ArrayList<>();
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("type", "general");
            suggestion.put("description", "建议合理安排行程时间，避免过于紧凑");
            fallbackSuggestions.add(suggestion);

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("suggestions", fallbackSuggestions);
            result.put("optimizedScore", 75);
            result.put("source", "fallback");
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
            String prompt = "请详细介绍以下景点：" + attraction.getName() +
                    "，位于" + attraction.getCity().getName() +
                    "。包括历史背景、特色亮点、游览建议等。";

            String intro = callOpenAI(prompt);

            result.put("attractionId", attractionId);
            result.put("name", attraction.getName());
            result.put("detailedIntro", intro);
            result.put("bestVisitTime", "建议游览时间：2-3小时");
            result.put("source", "openai");

            cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("AI景点介绍失败: error={}", e.getMessage());

            result.put("attractionId", attractionId);
            result.put("name", attraction.getName());
            result.put("detailedIntro", attraction.getDescription() != null ?
                    attraction.getDescription() : "暂无详细介绍");
            result.put("source", "fallback");
        }

        return result;
    }

}
