package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.service.travel_recommendation.AIMultimodalService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai-multimodal")
@RequiredArgsConstructor
public class AIMultimodalController {

    private static final Logger log = LoggerFactory.getLogger(AIMultimodalController.class);

    private final AIMultimodalService aiMultimodalService;

    @PostMapping("/recommend")
    public Result<List<Map<String, Object>>> getMultimodalRecommendations(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取多模态推荐请求");
            List<Map<String, Object>> recommendations = aiMultimodalService.getMultimodalRecommendations(text, image, audio, limit);
            return Result.success("获取推荐成功", recommendations);
        } catch (Exception e) {
            log.error("获取多模态推荐失败: error={}", e.getMessage());
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }

    @PostMapping("/understand")
    public Result<Map<String, Object>> understandContent(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio) {
        try {
            log.info("多模态内容理解请求");
            Map<String, Object> understanding = aiMultimodalService.understandContent(text, image, audio);
            return Result.success("内容理解成功", understanding);
        } catch (Exception e) {
            log.error("多模态内容理解失败: error={}", e.getMessage());
            return Result.error("内容理解失败: " + e.getMessage());
        }
    }

    @PostMapping("/text-image-recommend")
    public Result<List<Map<String, Object>>> getTextImageRecommendations(
            @RequestParam String text,
            @RequestParam("image") MultipartFile image,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("文本图像联合推荐请求: textLength={}, imageFilename={}", text.length(), image.getOriginalFilename());
            List<Map<String, Object>> recommendations = aiMultimodalService.getTextImageRecommendations(text, image, limit);
            return Result.success("获取推荐成功", recommendations);
        } catch (Exception e) {
            log.error("文本图像联合推荐失败: error={}", e.getMessage());
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    public Result<List<Map<String, Object>>> multimodalSearch(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("多模态搜索请求");
            List<Map<String, Object>> results = aiMultimodalService.multimodalSearch(text, image, page, size);
            return Result.success("搜索成功", results);
        } catch (Exception e) {
            log.error("多模态搜索失败: error={}", e.getMessage());
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generateContent(@RequestBody Map<String, Object> generateRequest) {
        try {
            log.info("多模态内容生成请求: type={}", generateRequest.get("type"));
            Map<String, Object> content = aiMultimodalService.generateContent(generateRequest);
            return Result.success("内容生成成功", content);
        } catch (Exception e) {
            log.error("多模态内容生成失败: error={}", e.getMessage());
            return Result.error("内容生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/compare")
    public Result<Map<String, Object>> compareContent(@RequestBody Map<String, Object> compareRequest) {
        try {
            log.info("多模态内容对比请求");
            Map<String, Object> comparison = aiMultimodalService.compareContent(compareRequest);
            return Result.success("内容对比成功", comparison);
        } catch (Exception e) {
            log.error("多模态内容对比失败: error={}", e.getMessage());
            return Result.error("内容对比失败: " + e.getMessage());
        }
    }

    @PostMapping("/summarize")
    public Result<String> summarizeContent(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio) {
        try {
            log.info("多模态内容摘要请求");
            String summary = aiMultimodalService.summarizeContent(text, image, audio);
            return Result.success("内容摘要成功", summary);
        } catch (Exception e) {
            log.error("多模态内容摘要失败: error={}", e.getMessage());
            return Result.error("内容摘要失败: " + e.getMessage());
        }
    }

    @PostMapping("/sentiment")
    public Result<Map<String, Object>> analyzeSentiment(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio) {
        try {
            log.info("多模态情感分析请求");
            Map<String, Object> sentiment = aiMultimodalService.analyzeSentiment(text, image, audio);
            return Result.success("情感分析成功", sentiment);
        } catch (Exception e) {
            log.error("多模态情感分析失败: error={}", e.getMessage());
            return Result.error("情感分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/qa")
    public Result<Map<String, Object>> multimodalQA(@RequestBody Map<String, Object> qaRequest) {
        try {
            log.info("多模态问答请求: question={}", qaRequest.get("question"));
            Map<String, Object> answer = aiMultimodalService.multimodalQA(qaRequest);
            return Result.success("问答成功", answer);
        } catch (Exception e) {
            log.error("多模态问答失败: error={}", e.getMessage());
            return Result.error("问答失败: " + e.getMessage());
        }
    }

    @PostMapping("/report")
    public Result<Map<String, Object>> getMultimodalReport(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio) {
        try {
            log.info("获取多模态分析报告请求");
            Map<String, Object> report = aiMultimodalService.getMultimodalReport(text, image, audio);
            return Result.success("获取报告成功", report);
        } catch (Exception e) {
            log.error("获取多模态分析报告失败: error={}", e.getMessage());
            return Result.error("获取报告失败: " + e.getMessage());
        }
    }
}