package travel.service.impl.travel_recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import travel.config.AIConfig;
import travel.entity.route_planning.Route;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AIAssistantService;
import travel.service.travel_recommendation.AttractionService;
import travel.utils.CacheUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.openai.enabled", havingValue = "true")
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
    public List<Map<String, Object>> recommendByAI(String userInput, Integer userId) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            String prompt = "根据以下用户需求，推荐3个合适的旅游路线或景点：\n" + userInput;
            String aiResponse = callOpenAI(prompt);

            Map<String, Object> recommendation = new HashMap<>();
            recommendation.put("id", 1);
            recommendation.put("name", "AI智能推荐路线");
            recommendation.put("description", aiResponse);
            recommendation.put("matchScore", 95);
            recommendation.put("source", "openai");
            recommendations.add(recommendation);

        } catch (Exception e) {
            log.error("AI推荐失败，使用降级方案: error={}", e.getMessage());

            for (int i = 1; i <= 3; i++) {
                Map<String, Object> route = new HashMap<>();
                route.put("id", i);
                route.put("name", "推荐路线 " + i);
                route.put("description", "基于您的需求: " + userInput);
                route.put("matchScore", 95 - i * 5);
                route.put("estimatedCost", 2000 + i * 500);
                route.put("duration", 3 + i);
                route.put("source", "fallback");
                recommendations.add(route);
            }
        }

        return recommendations;
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

    @Override
    public Map<String, Object> translate(String text, String targetLanguage) {
        Map<String, Object> result = new HashMap<>();

        try {
            String prompt = "请将以下文本翻译成" + targetLanguage + "：\n" + text;
            String translated = callOpenAI(prompt);

            result.put("original", text);
            result.put("translated", translated);
            result.put("targetLanguage", targetLanguage);
            result.put("confidence", 0.95);
            result.put("source", "openai");

        } catch (Exception e) {
            log.error("AI翻译失败: error={}", e.getMessage());
            result.put("original", text);
            result.put("translated", "[翻译功能暂时不可用] " + text);
            result.put("targetLanguage", targetLanguage);
            result.put("confidence", 0.50);
            result.put("source", "fallback");
        }

        return result;
    }

    @Override
    public Map<String, Object> speechToText(byte[] audioData) {
        Map<String, Object> result = new HashMap<>();
        result.put("text", "语音识别功能需要集成专门的语音API（如百度语音、讯飞语音）");
        result.put("confidence", 0.0);
        result.put("note", "此功能暂未实现");
        return result;
    }

    @Override
    public byte[] textToSpeech(String text) {
        log.warn("文字转语音功能需要集成TTS服务，当前返回空数组");
        return new byte[0];
    }

    @Override
    public Map<String, Object> chatWithCustomerService(String message, String sessionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String prompt = "你是智能客服助手，请友好地回答用户问题：\n" + message;
            String response = callOpenAI(prompt);

            result.put("message", message);
            result.put("response", response);
            result.put("sessionId", sessionId);
            result.put("isResolved", true);
            result.put("timestamp", LocalDateTime.now());
            result.put("source", "openai");

        } catch (Exception e) {
            log.error("智能客服失败: error={}", e.getMessage());
            result.put("message", message);
            result.put("response", "抱歉，智能客服暂时不可用。如需帮助，请联系人工客服。");
            result.put("sessionId", sessionId);
            result.put("isResolved", false);
            result.put("timestamp", LocalDateTime.now());
            result.put("source", "fallback");
        }

        return result;
    }

    @Override
    public Map<String, Object> generateTravelDiary(Integer routeId, List<String> photos) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        try {
            String prompt = "请根据以下旅行路线生成一篇生动的旅行日记：\n" +
                    "路线：" + route.getTitle() + "\n" +
                    "城市：" + route.getCity().getName();

            String diary = callOpenAI(prompt);

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("title", route.getTitle() + "之旅");
            result.put("content", diary);
            result.put("photoCount", photos.size());
            result.put("generatedAt", LocalDateTime.now());
            result.put("source", "openai");

        } catch (Exception e) {
            log.error("生成旅行日记失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "生成失败，请稍后重试");
        }

        return result;
    }

    @Override
    public Map<String, Object> getPhotoTips(Integer attractionId) {
        Map<String, Object> result = new HashMap<>();

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            result.put("success", false);
            result.put("message", "景点不存在");
            return result;
        }

        try {
            String prompt = "请为景点'" + attraction.getName() + "'提供拍照建议，包括最佳拍摄角度、时间、构图技巧等。";
            String tips = callOpenAI(prompt);

            result.put("success", true);
            result.put("attractionId", attractionId);
            result.put("tips", tips);
            result.put("source", "openai");

        } catch (Exception e) {
            result.put("success", true);
            result.put("attractionId", attractionId);
            result.put("tips", "建议选择光线充足的时间段拍摄，注意构图和背景。");
            result.put("source", "fallback");
        }

        return result;
    }

    @Override
    public Map<String, Object> getAudioGuide(Integer attractionId, Map<String, Double> userLocation) {
        Map<String, Object> result = new HashMap<>();
        result.put("attractionId", attractionId);
        result.put("audioUrl", "");
        result.put("note", "语音导游功能需要集成TTS服务和音频存储");
        return result;
    }

    @Override
    public Map<String, Object> summarizeTrip(Integer routeId) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        try {
            String prompt = "请总结以下旅行路线的亮点和建议：\n" + route.getTitle();
            String summary = callOpenAI(prompt);

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("summary", summary);
            result.put("source", "openai");

        } catch (Exception e) {
            result.put("success", true);
            result.put("routeId", routeId);
            result.put("summary", "这是一次精彩的旅行，希望您玩得开心！");
            result.put("source", "fallback");
        }

        return result;
    }

    @Override
    public Map<String, Object> predictBestTime(Integer cityId, Integer month) {
        Map<String, Object> result = new HashMap<>();
        result.put("cityId", cityId);
        result.put("month", month);
        result.put("weatherScore", 80);
        result.put("crowdScore", 70);
        result.put("priceScore", 75);
        result.put("overallScore", 75);
        result.put("recommendation", "该月份出行较为适宜");
        return result;
    }

    @Override
    public List<Map<String, Object>> generatePackingList(Integer routeId, Map<String, Object> weather) {
        List<Map<String, Object>> packingList = new ArrayList<>();

        Map<String, Object> essential = new HashMap<>();
        essential.put("category", "必备物品");
        essential.put("items", Arrays.asList(
                Map.of("name", "身份证", "isChecked", false),
                Map.of("name", "手机充电器", "isChecked", false),
                Map.of("name", "常用药品", "isChecked", false)
        ));
        packingList.add(essential);

        return packingList;
    }

    @Override
    public Map<String, Object> analyzeSentiment(String text) {
        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        result.put("sentiment", "neutral");
        result.put("confidence", 0.50);
        result.put("note", "情感分析需要集成NLP服务");
        return result;
    }

    @Override
    public List<String> generateTags(String content) {
        List<String> tags = new ArrayList<>();

        if (content.contains("美食") || content.contains("吃")) {
            tags.add("美食");
        }
        if (content.contains("风景") || content.contains("拍照")) {
            tags.add("风景");
        }
        if (content.contains("历史") || content.contains("文化")) {
            tags.add("文化");
        }

        if (tags.isEmpty()) {
            tags.add("旅行");
        }

        return tags;
    }
}
