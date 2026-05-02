package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.service.travel_recommendation.AIAssistantService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI智能助手控制器
 * 提供智能问答、路线推荐、行程优化等功能
 */
@RestController
@RequestMapping("/ai-assistant")
@Slf4j
@RequiredArgsConstructor
public class AIAssistantController {

    private final AIAssistantService aiAssistantService;

    /**
     * 智能问答
     * POST /api/ai-assistant/ask
     */
    @PostMapping("/ask")
    public Result<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            Integer userId = (Integer) request.get("userId");
            log.info("智能问答请求: userId={}, question={}", userId, question);
            Map<String, Object> answer = aiAssistantService.askQuestion(question, userId);
            return Result.success("智能问答成功", answer);
        } catch (Exception e) {
            log.error("智能问答失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能路线推荐
     * POST /api/ai-assistant/recommend
     */
    @PostMapping("/recommend")
    public Result<List<Map<String, Object>>> recommendByAI(@RequestBody Map<String, Object> request) {
        try {
            String userInput = (String) request.get("userInput");
            Integer userId = (Integer) request.get("userId");
            log.info("智能路线推荐请求: userId={}, userInput={}", userId, userInput);
            List<Map<String, Object>> routes = aiAssistantService.recommendByAI(userInput, userId);
            return Result.success("智能路线推荐成功", routes);
        } catch (Exception e) {
            log.error("智能路线推荐失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 行程优化建议
     * POST /api/ai-assistant/optimize/{routeId}
     */
    @PostMapping("/optimize/{routeId}")
    public Result<Map<String, Object>> optimizeRouteByAI(@PathVariable Integer routeId) {
        try {
            log.info("行程优化建议请求: routeId={}", routeId);
            Map<String, Object> optimization = aiAssistantService.optimizeRouteByAI(routeId);
            return Result.success("行程优化建议成功", optimization);
        } catch (Exception e) {
            log.error("行程优化建议失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 景点智能介绍
     * GET /api/ai-assistant/attraction-intro/{attractionId}
     */
    @GetMapping("/attraction-intro/{attractionId}")
    public Result<Map<String, Object>> getAttractionIntro(@PathVariable Integer attractionId) {
        try {
            log.info("获取景点智能介绍请求: attractionId={}", attractionId);
            Map<String, Object> intro = aiAssistantService.getAttractionIntro(attractionId);
            return Result.success("获取景点智能介绍成功", intro);
        } catch (Exception e) {
            log.error("获取景点智能介绍失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能翻译
     * POST /api/ai-assistant/translate
     */
    @PostMapping("/translate")
    public Result<Map<String, Object>> translate(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            String targetLanguage = (String) request.get("targetLanguage");
            log.info("智能翻译请求: text={}, targetLanguage={}", text, targetLanguage);
            Map<String, Object> result = aiAssistantService.translate(text, targetLanguage);
            return Result.success("智能翻译成功", result);
        } catch (Exception e) {
            log.error("智能翻译失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 语音转文字
     * POST /api/ai-assistant/speech-to-text
     */
    @PostMapping("/speech-to-text")
    public Result<Map<String, Object>> speechToText(@RequestBody Map<String, Object> request) {
        try {
            byte[] audioData = (byte[]) request.get("audioData");
            log.info("语音转文字请求: audioData length={}", audioData != null ? audioData.length : 0);
            Map<String, Object> result = aiAssistantService.speechToText(audioData);
            return Result.success("语音转文字成功", result);
        } catch (Exception e) {
            log.error("语音转文字失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 文字转语音
     * POST /api/ai-assistant/text-to-speech
     */
    @PostMapping("/text-to-speech")
    public Result<byte[]> textToSpeech(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            log.info("文字转语音请求: text length={}", text != null ? text.length() : 0);
            byte[] audioData = aiAssistantService.textToSpeech(text);
            return Result.success("文字转语音成功", audioData);
        } catch (Exception e) {
            log.error("文字转语音失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能客服
     * POST /api/ai-assistant/chat
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chatWithCustomerService(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            String sessionId = (String) request.get("sessionId");
            log.info("智能客服请求: sessionId={}, message={}", sessionId, message);
            Map<String, Object> response = aiAssistantService.chatWithCustomerService(message, sessionId);
            return Result.success("智能客服回复成功", response);
        } catch (Exception e) {
            log.error("智能客服失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成旅行日记
     * POST /api/ai-assistant/diary/{routeId}
     */
    @PostMapping("/diary/{routeId}")
    public Result<Map<String, Object>> generateTravelDiary(@PathVariable Integer routeId,
                                                             @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> photos = (List<String>) request.get("photos");
            log.info("生成旅行日记请求: routeId={}", routeId);
            Map<String, Object> diary = aiAssistantService.generateTravelDiary(routeId, photos);
            return Result.success("生成旅行日记成功", diary);
        } catch (Exception e) {
            log.error("生成旅行日记失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能拍照建议
     * GET /api/ai-assistant/photo-tips/{attractionId}
     */
    @GetMapping("/photo-tips/{attractionId}")
    public Result<Map<String, Object>> getPhotoTips(@PathVariable Integer attractionId) {
        try {
            log.info("获取智能拍照建议请求: attractionId={}", attractionId);
            Map<String, Object> tips = aiAssistantService.getPhotoTips(attractionId);
            return Result.success("获取智能拍照建议成功", tips);
        } catch (Exception e) {
            log.error("获取智能拍照建议失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实时语音导游
     * POST /api/ai-assistant/audio-guide/{attractionId}
     */
    @PostMapping("/audio-guide/{attractionId}")
    public Result<Map<String, Object>> getAudioGuide(@PathVariable Integer attractionId,
                                                     @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Double> userLocation = (Map<String, Double>) request.get("userLocation");
            log.info("获取实时语音导游请求: attractionId={}", attractionId);
            Map<String, Object> audioGuide = aiAssistantService.getAudioGuide(attractionId, userLocation);
            return Result.success("获取实时语音导游成功", audioGuide);
        } catch (Exception e) {
            log.error("获取实时语音导游失败: attractionId={}, error={}", attractionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能行程总结
     * GET /api/ai-assistant/summarize/{routeId}
     */
    @GetMapping("/summarize/{routeId}")
    public Result<Map<String, Object>> summarizeTrip(@PathVariable Integer routeId) {
        try {
            log.info("智能行程总结请求: routeId={}", routeId);
            Map<String, Object> summary = aiAssistantService.summarizeTrip(routeId);
            return Result.success("智能行程总结成功", summary);
        } catch (Exception e) {
            log.error("智能行程总结失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 预测最佳出行时间
     * GET /api/ai-assistant/best-time
     */
    @GetMapping("/best-time")
    public Result<Map<String, Object>> predictBestTime(@RequestParam Integer cityId,
                                                       @RequestParam Integer month) {
        try {
            log.info("预测最佳出行时间请求: cityId={}, month={}", cityId, month);
            Map<String, Object> prediction = aiAssistantService.predictBestTime(cityId, month);
            return Result.success("预测最佳出行时间成功", prediction);
        } catch (Exception e) {
            log.error("预测最佳出行时间失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能打包清单
     * POST /api/ai-assistant/packing-list/{routeId}
     */
    @PostMapping("/packing-list/{routeId}")
    public Result<List<Map<String, Object>>> generatePackingList(@PathVariable Integer routeId,
                                                                 @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> weather = (Map<String, Object>) request.get("weather");
            log.info("生成智能打包清单请求: routeId={}", routeId);
            List<Map<String, Object>> packingList = aiAssistantService.generatePackingList(routeId, weather);
            return Result.success("生成智能打包清单成功", packingList);
        } catch (Exception e) {
            log.error("生成智能打包清单失败: routeId={}, error={}", routeId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 情感分析
     * POST /api/ai-assistant/sentiment
     */
    @PostMapping("/sentiment")
    public Result<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            log.info("情感分析请求: text length={}", text != null ? text.length() : 0);
            Map<String, Object> result = aiAssistantService.analyzeSentiment(text);
            return Result.success("情感分析成功", result);
        } catch (Exception e) {
            log.error("情感分析失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 智能标签生成
     * POST /api/ai-assistant/tags
     */
    @PostMapping("/tags")
    public Result<List<String>> generateTags(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            log.info("智能标签生成请求: content length={}", content != null ? content.length() : 0);
            List<String> tags = aiAssistantService.generateTags(content);
            return Result.success("智能标签生成成功", tags);
        } catch (Exception e) {
            log.error("智能标签生成失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
