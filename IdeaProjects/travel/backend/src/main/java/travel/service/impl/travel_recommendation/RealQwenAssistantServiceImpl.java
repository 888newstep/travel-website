package travel.service.impl.travel_recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import travel.entity.route_planning.Route;
import travel.entity.travel_recommendation.Attraction;
import travel.service.route_planning.RouteService;
import travel.service.travel_recommendation.AIAssistantService;
import travel.service.travel_recommendation.AttractionService;
import travel.service.travel_recommendation.QwenService;
import travel.utils.CacheUtil;

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
    public List<Map<String, Object>> recommendByAI(String userInput, Integer userId) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            String prompt = "根据以下用户需求，推荐5个合适的旅游路线或景点，以JSON数组格式返回，每个包含id、name、description、matchScore字段：\n" + userInput;
            String aiResponse = qwenService.chatCompletion(prompt, "你是一个专业的旅游推荐助手");

            // 解析AI返回的JSON结果
            // TODO: 添加JSON解析逻辑

            Map<String, Object> recommendation = new HashMap<>();
            recommendation.put("id", 1);
            recommendation.put("name", "AI智能推荐路线");
            recommendation.put("description", aiResponse);
            recommendation.put("matchScore", 95);
            recommendation.put("source", "qwen");
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

    @Override
    public Map<String, Object> translate(String text, String targetLanguage) {
        Map<String, Object> result = new HashMap<>();

        try {
            String translated = qwenService.translate(text, targetLanguage);

            result.put("original", text);
            result.put("translated", translated);
            result.put("targetLanguage", targetLanguage);
            result.put("confidence", 0.95);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("翻译失败: error={}", e.getMessage());
            result.put("original", text);
            result.put("translated", text);
            result.put("targetLanguage", targetLanguage);
            result.put("confidence", 0.0);
            result.put("error", "翻译失败");
        }

        return result;
    }

    @Override
    public Map<String, Object> speechToText(byte[] audioData) {
        Map<String, Object> result = new HashMap<>();

        // TODO: 集成语音识别API（如百度语音识别、阿里云语音识别）
        result.put("text", "语音识别功能待集成");
        result.put("confidence", 0.0);
        result.put("language", "zh-CN");
        result.put("duration", 0.0);
        result.put("source", "mock");

        log.warn("语音转文字功能尚未集成真实API");
        return result;
    }

    @Override
    public byte[] textToSpeech(String text) {
        // TODO: 集成语音合成API（如百度语音合成、阿里云语音合成）
        log.warn("文字转语音功能尚未集成真实API");
        return new byte[0];
    }

    @Override
    public Map<String, Object> chatWithCustomerService(String message, String sessionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String reply = qwenService.customerServiceReply(message, sessionId);

            result.put("reply", reply);
            result.put("sessionId", sessionId);
            result.put("timestamp", LocalDateTime.now());
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("客服对话失败: error={}", e.getMessage());
            result.put("reply", "抱歉，客服系统暂时不可用");
            result.put("error", e.getMessage());
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
            String prompt = String.format(
                    "请根据以下旅行路线生成一篇旅行日记：\n" +
                            "路线名称：%s\n" +
                            "照片数量：%d张\n\n" +
                            "要求：生动有趣，包含个人感受和体验。",
                    route.getTitle(),
                    photos != null ? photos.size() : 0
            );

            String diary = qwenService.chatCompletion(prompt,
                    "你是一个擅长写旅行日记的作家，文风生动有趣。");

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("diary", diary);
            result.put("photoCount", photos != null ? photos.size() : 0);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("生成旅行日记失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "生成失败");
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
            String prompt = String.format(
                    "请为%s提供拍照建议，包括：\n" +
                            "1. 最佳拍摄角度\n" +
                            "2. 最佳拍摄时间\n" +
                            "3. 构图建议\n" +
                            "4. 注意事项",
                    attraction.getName()
            );

            String tips = qwenService.chatCompletion(prompt,
                    "你是一个专业的摄影指导，擅长旅游景点拍照建议。");

            result.put("success", true);
            result.put("attractionId", attractionId);
            result.put("tips", tips);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("获取拍照建议失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "获取失败");
        }

        return result;
    }

    @Override
    public Map<String, Object> getAudioGuide(Integer attractionId, Map<String, Double> userLocation) {
        Map<String, Object> result = new HashMap<>();

        Attraction attraction = attractionService.getById(attractionId);
        if (attraction == null) {
            result.put("success", false);
            result.put("message", "景点不存在");
            return result;
        }

        try {
            String prompt = String.format(
                    "请为%s生成一段语音导游词，包括：\n" +
                            "1. 景点简介\n" +
                            "2. 历史故事\n" +
                            "3. 特色亮点\n" +
                            "4. 参观建议\n\n" +
                            "要求：语言生动，适合朗读，时长约2-3分钟。",
                    attraction.getName()
            );

            String guideText = qwenService.chatCompletion(prompt,
                    "你是一个专业的导游，讲解生动有趣。");

            result.put("success", true);
            result.put("attractionId", attractionId);
            result.put("guideText", guideText);
            result.put("estimatedDuration", "2-3分钟");
            result.put("source", "qwen");

            // TODO: 调用文字转语音API生成音频文件

        } catch (Exception e) {
            log.error("获取语音导游失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "获取失败");
        }

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
            String prompt = String.format(
                    "请总结以下旅行路线的关键信息：\n" +
                            "路线名称：%s\n" +
                            "天数：%d天\n\n" +
                            "请提供：\n" +
                            "1. 行程亮点\n" +
                            "2. 总体评价\n" +
                            "3. 适合人群\n" +
                            "4. 注意事项",
                    route.getTitle(),
                    route.getDurationDays() != null ? route.getDurationDays() : 0
            );

            String summary = qwenService.chatCompletion(prompt,
                    "你是一个专业的旅行总结专家。");

            result.put("success", true);
            result.put("routeId", routeId);
            result.put("summary", summary);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("行程总结失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "总结失败");
        }

        return result;
    }

    @Override
    public Map<String, Object> predictBestTime(Integer cityId, Integer month) {
        Map<String, Object> result = new HashMap<>();

        try {
            String prompt = String.format(
                    "请预测%d月份去该城市旅行的最佳时间，包括：\n" +
                            "1. 天气状况\n" +
                            "2. 旅游旺季/淡季\n" +
                            "3. 推荐活动\n" +
                            "4. 注意事项",
                    month
            );

            String prediction = qwenService.chatCompletion(prompt,
                    "你是一个旅行时间规划专家。");

            result.put("success", true);
            result.put("cityId", cityId);
            result.put("month", month);
            result.put("prediction", prediction);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("预测最佳时间失败: error={}", e.getMessage());
            result.put("success", false);
            result.put("message", "预测失败");
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> generatePackingList(Integer routeId, Map<String, Object> weather) {
        List<Map<String, Object>> packingList = new ArrayList<>();

        try {
            String prompt = String.format(
                    "请根据以下天气信息生成旅行打包清单：\n" +
                            "天气：%s\n" +
                            "温度：%s\n\n" +
                            "请列出必备物品，分类整理。",
                    weather != null ? weather.get("weather") : "未知",
                    weather != null ? weather.get("temperature") : "未知"
            );

            String listText = qwenService.chatCompletion(prompt,
                    "你是一个旅行打包专家，擅长根据不同情况提供打包建议。");

            Map<String, Object> item = new HashMap<>();
            item.put("category", "综合清单");
            item.put("items", listText);
            packingList.add(item);

        } catch (Exception e) {
            log.error("生成打包清单失败: error={}", e.getMessage());
        }

        return packingList;
    }

    @Override
    public Map<String, Object> analyzeSentiment(String text) {
        Map<String, Object> result = new HashMap<>();

        try {
            result = qwenService.analyzeSentiment(text);
            result.put("source", "qwen");

        } catch (Exception e) {
            log.error("情感分析失败: error={}", e.getMessage());
            result.put("sentiment", "unknown");
            result.put("confidence", 0.0);
            result.put("error", "分析失败");
        }

        return result;
    }

    @Override
    public List<String> generateTags(String content) {
        List<String> tags = new ArrayList<>();

        try {
            String prompt = String.format(
                    "请为以下内容生成5-10个标签，用逗号分隔：\n\n%s",
                    content
            );

            String tagsText = qwenService.chatCompletion(prompt,
                    "你是一个标签生成专家，能够准确提取内容的关键词。");

            String[] tagArray = tagsText.split("[,，]");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    tags.add(trimmedTag);
                }
            }

        } catch (Exception e) {
            log.error("生成标签失败: error={}", e.getMessage());
        }

        return tags;
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
