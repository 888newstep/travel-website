package travel.route.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import travel.common.config.AIConfig;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.utils.BoundedHttpBodyReader;
import travel.common.utils.CacheUtil;
import travel.common.utils.ExternalCallBulkhead;
import travel.common.utils.ExternalCallBulkheadRegistry;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.service.AIAssistantResponseSupport;
import travel.route.service.AIAssistantService;
import travel.route.service.AttractionService;
import travel.route.service.RouteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RealAIAssistantServiceImpl implements AIAssistantService {

    private static final Logger log = LoggerFactory.getLogger(RealAIAssistantServiceImpl.class);
    private static final String AI_PREFIX = "ai:assistant:v2:openai:";
    private static final String SOURCE_OPENAI = "openai";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final RouteService routeService;
    private final AttractionService attractionService;
    private final CacheUtil cacheUtil;
    private final AIConfig aiConfig;
    private final ExternalCallBulkheadRegistry bulkheadRegistry;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${travel.external.max-response-bytes:1048576}")
    private long maxResponseBytes = 1_048_576L;

    @Override
    public AIAskQuestionResponse askQuestion(String question, Integer userId) {
        ensureOpenAIAvailable();
        String cacheKey = AI_PREFIX + "qa:" + question.hashCode();

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = cacheUtil.get(cacheKey, Map.class);
        if (cached != null) {
            log.info("Return cached OpenAI answer: question={}", question);
            return AIAssistantResponseSupport.toAskQuestionResponse(cached);
        }

        try {
            String answer = callOpenAI(question);
            Map<String, Object> result = AIAssistantResponseSupport.buildAskQuestionPayload(
                    question, answer, SOURCE_OPENAI);
            cacheUtil.set(cacheKey, result, 60, TimeUnit.MINUTES);
            log.info("OpenAI question answered: question={}", question);
            return AIAssistantResponseSupport.toAskQuestionResponse(result);
        } catch (Exception e) {
            log.error("OpenAI question failed: question={}", question, e);
            throw dependencyFailure(e);
        }
    }

    @Override
    public AIOptimizeRouteResponse optimizeRouteByAI(Integer routeId) {
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        try {
            String prompt = String.join("\n",
                    "Please optimize the following travel route and reply in Chinese.",
                    "Route title: " + defaultIfBlank(route.getTitle(), "N/A"),
                    "City: " + routeCityName(route),
                    "Give concrete and practical suggestions."
            );
            String suggestion = callOpenAI(prompt);
            return AIAssistantResponseSupport.buildOptimizationSuccess(routeId, suggestion, SOURCE_OPENAI);
        } catch (Exception e) {
            log.error("OpenAI route optimization failed: routeId={}", routeId, e);
            throw dependencyFailure(e);
        }
    }

    @Override
    public AIAttractionIntroResponse getAttractionIntro(Integer attractionId) {
        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            throw new BusinessException(ErrorCodeEnum.ATTRACTION_NOT_EXIST);
        }

        ensureOpenAIAvailable();
        String cacheKey = AI_PREFIX + "intro:" + attractionId;
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = cacheUtil.get(cacheKey, Map.class);
        if (cached != null) {
            return AIAssistantResponseSupport.toAttractionIntroResponse(cached);
        }

        try {
            String prompt = String.join("\n",
                    "Please introduce the following attraction in Chinese.",
                    "Attraction: " + defaultIfBlank(attraction.getName(), "N/A"),
                    "City: " + attractionCityName(attraction),
                    "Include highlights, travel tips, best visit time, and suggested duration."
            );
            String intro = callOpenAI(prompt);
            Map<String, Object> result = AIAssistantResponseSupport.buildAttractionIntroPayload(
                    attractionId,
                    attraction.getName(),
                    intro,
                    SOURCE_OPENAI
            );
            cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);
            return AIAssistantResponseSupport.toAttractionIntroResponse(result);
        } catch (Exception e) {
            log.error("OpenAI attraction intro failed: attractionId={}", attractionId, e);
            throw dependencyFailure(e);
        }
    }

    private String callOpenAI(String question) throws IOException {
        ensureOpenAIAvailable();

        String url = aiConfig.getApiUrl() + "/chat/completions";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getModel());
        requestBody.put("messages", buildMessages(question));
        requestBody.put("temperature", aiConfig.getTemperature());
        requestBody.put("max_tokens", aiConfig.getMaxTokens());

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (ExternalCallBulkhead.Permit ignored = bulkheadRegistry.get(ExternalCallBulkheadRegistry.OPENAI).acquire();
             Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI request failed: status=" + response.code());
            }
            if (response.body() == null) {
                throw new IOException("OpenAI response body is empty");
            }

            String responseBody = BoundedHttpBodyReader.readUtf8(response.body(), maxResponseBytes);
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choicesNode = rootNode.path("choices");
            if (!choicesNode.isArray() || choicesNode.size() == 0) {
                throw new IOException("OpenAI response has no choices");
            }

            String content = choicesNode.get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new IOException("OpenAI response content is empty");
            }
            return content;
        }
    }

    private List<Map<String, String>> buildMessages(String question) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a professional travel assistant. Reply in Chinese with concise and practical suggestions.");
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        messages.add(userMessage);

        return messages;
    }

    private void ensureOpenAIAvailable() {
        if (!Boolean.TRUE.equals(aiConfig.getEnabled())
                || aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()
                || aiConfig.getApiUrl() == null || aiConfig.getApiUrl().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR);
        }
    }

    private RuntimeException dependencyFailure(Exception exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException;
        }
        return new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR);
    }

    private String routeCityName(Route route) {
        if (route.getCity() != null && route.getCity().getName() != null && !route.getCity().getName().isBlank()) {
            return route.getCity().getName();
        }
        return "unknown";
    }

    private String attractionCityName(Attraction attraction) {
        if (attraction.getCity() != null && attraction.getCity().getName() != null && !attraction.getCity().getName().isBlank()) {
            return attraction.getCity().getName();
        }
        return "unknown";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
