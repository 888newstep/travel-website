package travel.service.travel_recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import travel.config.AIConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 智能对话
     */
    public String chatCompletion(String userMessage, String systemPrompt) {
        if (!aiConfig.getEnabled()) {
            log.warn("OpenAI服务未启用");
            return "AI服务暂未启用，请稍后再试";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("temperature", aiConfig.getTemperature());
            requestBody.put("max_tokens", aiConfig.getMaxTokens());

            // 构建消息列表
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();

            // 系统提示词
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
                messages.add(systemMsg);
            }

            // 用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request request = new Request.Builder()
                    .url(aiConfig.getApiUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OpenAI API调用失败: {}", response.code());
                    throw new RuntimeException("AI服务调用失败: " + response.code());
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // 提取回复内容
                return jsonNode.get("choices").get(0).get("message").get("content").asText();
            }
        } catch (IOException e) {
            log.error("OpenAI调用异常: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用异常: " + e.getMessage());
        }
    }

    /**
     * 行程推荐
     */
    public String recommendItinerary(String preferences, int days, String budget) {
        String systemPrompt = "你是一个专业的旅行规划师，擅长根据用户需求制定个性化的旅行计划。" +
                "请考虑景点分布、交通方式、时间安排等因素，给出合理的建议。";

        String userMessage = String.format(
                "请为我制定一个%d天的旅行计划。\n" +
                        "我的偏好：%s\n" +
                        "预算范围：%s\n" +
                        "请以JSON格式返回，包含每天的行程安排、推荐景点、交通方式和预计费用。",
                days, preferences, budget
        );

        return chatCompletion(userMessage, systemPrompt);
    }

    /**
     * 景点介绍生成
     */
    public String generateAttractionDescription(String attractionName, String location) {
        String systemPrompt = "你是一个旅游专家，擅长撰写生动有趣的景点介绍。";

        String userMessage = String.format(
                "请为%s（位于%s）写一段吸引人的景点介绍，包括历史背景、特色亮点和游览建议，字数控制在200字以内。",
                attractionName, location
        );

        return chatCompletion(userMessage, systemPrompt);
    }

    /**
     * 旅行问答
     */
    public String travelQA(String question) {
        String systemPrompt = "你是一个旅行顾问，专门回答各种旅行相关的问题，包括签证、交通、住宿、美食等。" +
                "请给出专业、实用且简洁的回答。";

        return chatCompletion(question, systemPrompt);
    }
}
