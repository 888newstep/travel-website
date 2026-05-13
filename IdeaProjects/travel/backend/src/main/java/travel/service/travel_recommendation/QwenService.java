package travel.service.travel_recommendation;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.config.AIConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenService {

    private final AIConfig aiConfig;

    /**
     * 智能对话 - 基础方法
     */
    public String chatCompletion(String userMessage, String systemPrompt) {
        if (!isQwenEnabled()) {
            log.warn("通义千问服务未启用");
            return "AI服务暂未启用，请稍后再试";
        }

        try {
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
                    .apiKey(aiConfig.getQwen().getApiKey())
                    .model(aiConfig.getQwen().getModel())
                    .messages(messages)
                    .temperature(aiConfig.getQwen().getTemperature().floatValue())
                    .maxTokens(aiConfig.getQwen().getMaxTokens())
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            Generation generation = new Generation();
            GenerationResult result = generation.call(param);

            if (result != null && result.getOutput() != null
                    && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                return result.getOutput().getChoices().get(0).getMessage().getContent();
            }

            throw new RuntimeException("通义千问API返回为空");

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("通义千问API调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 多轮对话 - 支持对话历史
     */
    public String chatWithHistory(List<Map<String, String>> messages) {
        if (!isQwenEnabled()) {
            log.warn("通义千问服务未启用");
            return "AI服务暂未启用，请稍后再试";
        }

        try {
            List<Message> dashScopeMessages = new ArrayList<>();

            for (Map<String, String> msg : messages) {
                String role = msg.get("role");
                String content = msg.get("content");

                Role dashScopeRole;
                switch (role.toLowerCase()) {
                    case "system":
                        dashScopeRole = Role.SYSTEM;
                        break;
                    case "assistant":
                        dashScopeRole = Role.ASSISTANT;
                        break;
                    default:
                        dashScopeRole = Role.USER;
                }

                Message message = Message.builder()
                        .role(dashScopeRole.getValue())
                        .content(content)
                        .build();
                dashScopeMessages.add(message);
            }

            GenerationParam param = GenerationParam.builder()
                    .apiKey(aiConfig.getQwen().getApiKey())
                    .model(aiConfig.getQwen().getModel())
                    .messages(dashScopeMessages)
                    .temperature(aiConfig.getQwen().getTemperature().floatValue())
                    .maxTokens(aiConfig.getQwen().getMaxTokens())
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            Generation generation = new Generation();
            GenerationResult result = generation.call(param);

            if (result != null && result.getOutput() != null
                    && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                return result.getOutput().getChoices().get(0).getMessage().getContent();
            }

            throw new RuntimeException("通义千问API返回为空");

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("通义千问多轮对话失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
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

    /**
     * 智能客服回复
     */
    public String customerServiceReply(String userMessage, String conversationContext) {
        String systemPrompt = "你是智慧旅游平台的智能客服助手，态度友好、专业。" +
                "你的任务是解答用户关于旅游产品、订单、退款等问题。" +
                "如果问题超出你的能力范围，请礼貌地引导用户联系人工客服。";

        String fullMessage = conversationContext != null && !conversationContext.isEmpty()
                ? "对话历史：\n" + conversationContext + "\n\n用户当前问题：" + userMessage
                : userMessage;

        return chatCompletion(fullMessage, systemPrompt);
    }

    /**
     * 检查服务是否可用
     */
    public boolean isQwenEnabled() {
        return aiConfig.getQwen() != null
                && aiConfig.getQwen().getEnabled()
                && aiConfig.getQwen().getApiKey() != null
                && !aiConfig.getQwen().getApiKey().isEmpty();
    }
}
