package travel.route.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import travel.common.utils.AICacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通义千问AI服务 - 带缓存优化
 */
@Service
public class QwenService {

    private static final Logger log = LoggerFactory.getLogger(QwenService.class);

    @Value("${ai.qwen.api-key}")
    private String apiKey;

    @Value("${ai.qwen.model:qwen-plus}")
    private String model;

    @Value("${ai.qwen.temperature:0.7}")
    private Double temperature;

    @Value("${ai.qwen.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${ai.qwen.enabled:true}")
    private Boolean enabled;

    private final AICacheManager cacheManager;

    public QwenService(AICacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 智能对话 - 带缓存
     */
    public String chatCompletion(String userMessage, String systemPrompt) {
        if (!enabled) {
            log.warn("通义千问服务未启用");
            return "AI服务暂未启用，请稍后再试";
        }

        // 使用缓存
        return cacheManager.getOrSetQACache(
                systemPrompt != null ? systemPrompt + "|" + userMessage : userMessage,
                () -> callQwenAPI(userMessage, systemPrompt)
        );
    }

    /**
     * 实际调用通义千问API
     */
    private String callQwenAPI(String userMessage, String systemPrompt) {
        try {
            Generation gen = new Generation();

            List<Message> messages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Message systemMsg = Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(systemPrompt)
                        .build();
                messages.add(systemMsg);
            }

            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build();
            messages.add(userMsg);

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(messages)
                    .temperature(temperature.floatValue())
                    .maxTokens(maxTokens)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = gen.call(param);
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();

            log.info("通义千问API调用成功，响应长度: {}", response.length());
            return response;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("通义千问API调用失败: {}", e.getMessage(), e);
            return "抱歉，AI服务暂时不可用，请稍后重试";
        }
    }

    /**
     * 旅行问答 - 带缓存
     */
    public String travelQA(String question) {
        String systemPrompt = "你是一个专业的旅游顾问，擅长提供旅行建议、景点介绍、行程规划等服务。请用中文回答，回答要简洁明了、实用性强。";
        return chatCompletion(question, systemPrompt);
    }

    /**
     * 推荐行程 - 带缓存
     */
    public String recommendItinerary(String preferences, int days, String budget) {
        return cacheManager.getOrSetItineraryCache(
                preferences, days, budget,
                () -> {
                    String prompt = String.format(
                            "请根据以下需求生成一个%d天的旅行行程：\n" +
                                    "用户偏好：%s\n" +
                                    "预算：%s\n\n" +
                                    "请以JSON格式返回，包含每天的行程安排、景点推荐、餐饮建议和交通方式。",
                            days, preferences, budget
                    );

                    String systemPrompt = "你是一个专业的旅行规划师，擅长制定详细的旅行行程。请以JSON格式输出结果。";
                    return callQwenAPI(prompt, systemPrompt);
                }
        );
    }

    /**
     * 景点介绍生成 - 带缓存
     */
    public String generateAttractionIntro(String attractionName, String location) {
        return cacheManager.getOrSetAttractionIntroCache(
                (attractionName + location).hashCode(),
                () -> {
                    String prompt = String.format(
                            "请详细介绍%s（位于%s），包括：\n" +
                                    "1. 景点历史和文化背景\n" +
                                    "2. 主要特色和亮点\n" +
                                    "3. 最佳游览时间\n" +
                                    "4. 游览建议时长\n" +
                                    "5. 注意事项",
                            attractionName, location
                    );

                    String systemPrompt = "你是一个专业的旅游讲解员，擅长生动有趣地介绍景点。";
                    return callQwenAPI(prompt, systemPrompt);
                }
        );
    }

    /**
     * 旅行建议 - 带缓存
     */
    public String getTravelAdvice(String destination, String travelType) {
        String cacheKey = destination + "_" + travelType;
        return cacheManager.getOrSetQACache(
                cacheKey,
                () -> {
                    String prompt = String.format(
                            "我要去%s进行%s旅行，请给我一些实用的建议，包括：\n" +
                                    "1. 必去景点推荐\n" +
                                    "2. 当地美食推荐\n" +
                                    "3. 住宿建议\n" +
                                    "4. 交通出行提示\n" +
                                    "5. 注意事项和安全提示",
                            destination, travelType
                    );

                    String systemPrompt = "你是一个经验丰富的旅行达人，乐于分享实用的旅行建议。";
                    return callQwenAPI(prompt, systemPrompt);
                }
        );
    }

    /**
     * 客服回复生成 - 不缓存（每次对话上下文不同）
     */
    public String customerServiceReply(String userQuery, String context) {
        String prompt = String.format(
                "用户问题：%s\n" +
                        "上下文信息：%s\n\n" +
                        "请给出专业、友好的回复。",
                userQuery, context != null ? context : "无"
        );

        String systemPrompt = "你是一个专业的旅游客服代表，态度友好、专业，能够有效解决用户问题。";
        return callQwenAPI(prompt, systemPrompt);
    }

    /**
     * 多轮对话 - 不缓存（依赖对话历史）
     */
    public String multiTurnChat(List<Map<String, String>> conversationHistory, String newUserMessage) {
        if (!enabled) {
            return "AI服务暂未启用，请稍后再试";
        }

        try {
            Generation gen = new Generation();
            List<Message> messages = new ArrayList<>();

            for (Map<String, String> msg : conversationHistory) {
                Role role = "user".equals(msg.get("role")) ? Role.USER : Role.ASSISTANT;
                Message message = Message.builder()
                        .role(role.getValue())
                        .content(msg.get("content"))
                        .build();
                messages.add(message);
            }

            Message newMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(newUserMessage)
                    .build();
            messages.add(newMessage);

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(messages)
                    .temperature(temperature.floatValue())
                    .maxTokens(maxTokens)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            log.error("多轮对话失败: {}", e.getMessage(), e);
            return "抱歉，对话服务暂时不可用";
        }
    }

    /**
     * 文本摘要 - 带缓存
     */
    public String summarizeText(String text, int maxLength) {
        String cacheKey = "summary_" + text.hashCode() + "_" + maxLength;
        return cacheManager.getOrSetQACache(
                cacheKey,
                () -> {
                    String prompt = String.format(
                            "请将以下内容总结为%d字以内的摘要：\n\n%s",
                            maxLength, text
                    );

                    String systemPrompt = "你是一个专业的文本摘要生成器，能够准确提取关键信息。";
                    return callQwenAPI(prompt, systemPrompt);
                }
        );
    }

    /**
     * 情感分析 - 带缓存
     */
    /**
     * 情感分析 - 不缓存（结构化数据且需要实时性）
     */
    public Map<String, Object> analyzeSentiment(String text) {
        String prompt = String.format(
                "请分析以下文本的情感倾向，并以JSON格式返回结果，包含：\n" +
                        "1. sentiment: 情感类型（positive/negative/neutral）\n" +
                        "2. confidence: 置信度（0-1之间的小数）\n" +
                        "3. keywords: 关键词列表（数组格式）\n\n" +
                        "文本内容：%s",
                text
        );

        String systemPrompt = "你是一个情感分析专家，能够准确判断文本的情感倾向。请严格以JSON格式输出，不要包含其他文字。";
        String response = callQwenAPI(prompt, systemPrompt);

        // 解析 JSON 响应
        Map<String, Object> result = parseSentimentResponse(response);
        result.put("raw_response", response);
        result.put("source", "qwen");

        return result;
    }

    /**
     * 解析情感分析响应
     */
    private Map<String, Object> parseSentimentResponse(String jsonResponse) {
        Map<String, Object> result = new java.util.HashMap<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = mapper.readValue(jsonResponse, Map.class);

            result.put("sentiment", parsed.getOrDefault("sentiment", "unknown"));
            result.put("confidence", parsed.getOrDefault("confidence", 0.5));
            result.put("keywords", parsed.getOrDefault("keywords", new java.util.ArrayList<>()));
            result.put("success", true);

        } catch (Exception e) {
            log.warn("JSON解析失败，使用原始响应: {}", e.getMessage());
            result.put("sentiment", "neutral");
            result.put("confidence", 0.5);
            result.put("keywords", new java.util.ArrayList<>());
            result.put("success", false);
            result.put("parse_error", e.getMessage());
        }

        return result;
    }
}
