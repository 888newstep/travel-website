package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.service.travel_recommendation.AIImageAnalysisService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * AI图像分析控制器
 * 处理基于AI的图像识别、分析和推荐
 */
@RestController
@RequestMapping("/ai-image-analysis")
@RequiredArgsConstructor
public class AIImageAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AIImageAnalysisController.class);

    private final AIImageAnalysisService aiImageAnalysisService;

    /**
     * 分析景点图像
     * POST /api/ai-image-analysis/analyze
     */
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyzeImage(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String options) {
        try {
            log.info("分析景点图像请求: filename={}", file.getOriginalFilename());
            Map<String, Object> analysis = aiImageAnalysisService.analyzeImage(file, options);
            return Result.success("图像分析成功", analysis);
        } catch (Exception e) {
            log.error("分析景点图像失败: error={}", e.getMessage());
            return Result.error("图像分析失败: " + e.getMessage());
        }
    }

    /**
     * 识别景点
     * POST /api/ai-image-analysis/recognize
     */
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognizeAttraction(@RequestParam("file") MultipartFile file) {
        try {
            log.info("识别景点请求: filename={}", file.getOriginalFilename());
            Map<String, Object> recognition = aiImageAnalysisService.recognizeAttraction(file);
            return Result.success("景点识别成功", recognition);
        } catch (Exception e) {
            log.error("识别景点失败: error={}", e.getMessage());
            return Result.error("景点识别失败: " + e.getMessage());
        }
    }

    /**
     * 获取相似景点推荐
     * POST /api/ai-image-analysis/similar-attractions
     */
    @PostMapping("/similar-attractions")
    public Result<List<Map<String, Object>>> getSimilarAttractions(@RequestParam("file") MultipartFile file,
                                                                     @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取相似景点推荐请求: filename={}, limit={}", file.getOriginalFilename(), limit);
            List<Map<String, Object>> attractions = aiImageAnalysisService.getSimilarAttractions(file, limit);
            return Result.success("获取相似景点成功", attractions);
        } catch (Exception e) {
            log.error("获取相似景点推荐失败: error={}", e.getMessage());
            return Result.error("获取相似景点失败: " + e.getMessage());
        }
    }

    /**
     * 分析图像标签
     * POST /api/ai-image-analysis/tags
     */
    @PostMapping("/tags")
    public Result<List<String>> analyzeImageTags(@RequestParam("file") MultipartFile file) {
        try {
            log.info("分析图像标签请求: filename={}", file.getOriginalFilename());
            List<String> tags = aiImageAnalysisService.analyzeImageTags(file);
            return Result.success("获取标签成功", tags);
        } catch (Exception e) {
            log.error("分析图像标签失败: error={}", e.getMessage());
            return Result.error("获取标签失败: " + e.getMessage());
        }
    }

    /**
     * 获取图像描述
     * POST /api/ai-image-analysis/description
     */
    @PostMapping("/description")
    public Result<String> getImageDescription(@RequestParam("file") MultipartFile file) {
        try {
            log.info("获取图像描述请求: filename={}", file.getOriginalFilename());
            String description = aiImageAnalysisService.getImageDescription(file);
            return Result.success("获取描述成功", description);
        } catch (Exception e) {
            log.error("获取图像描述失败: error={}", e.getMessage());
            return Result.error("获取描述失败: " + e.getMessage());
        }
    }

    /**
     * 批量分析图像
     * POST /api/ai-image-analysis/batch-analyze
     */
    @PostMapping("/batch-analyze")
    public Result<List<Map<String, Object>>> batchAnalyzeImages(@RequestParam("files") MultipartFile[] files,
                                                                  @RequestParam(required = false) String options) {
        try {
            log.info("批量分析图像请求: count={}", files.length);
            List<Map<String, Object>> analysisList = aiImageAnalysisService.batchAnalyzeImages(files, options);
            return Result.success("批量分析成功", analysisList);
        } catch (Exception e) {
            log.error("批量分析图像失败: error={}", e.getMessage());
            return Result.error("批量分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取图像质量评估
     * POST /api/ai-image-analysis/quality
     */
    @PostMapping("/quality")
    public Result<Map<String, Object>> assessImageQuality(@RequestParam("file") MultipartFile file) {
        try {
            log.info("获取图像质量评估请求: filename={}", file.getOriginalFilename());
            Map<String, Object> quality = aiImageAnalysisService.assessImageQuality(file);
            return Result.success("质量评估成功", quality);
        } catch (Exception e) {
            log.error("获取图像质量评估失败: error={}", e.getMessage());
            return Result.error("质量评估失败: " + e.getMessage());
        }
    }

    /**
     * 获取图像色彩分析
     * POST /api/ai-image-analysis/colors
     */
    @PostMapping("/colors")
    public Result<Map<String, Object>> analyzeImageColors(@RequestParam("file") MultipartFile file) {
        try {
            log.info("获取图像色彩分析请求: filename={}", file.getOriginalFilename());
            Map<String, Object> colors = aiImageAnalysisService.analyzeImageColors(file);
            return Result.success("色彩分析成功", colors);
        } catch (Exception e) {
            log.error("获取图像色彩分析失败: error={}", e.getMessage());
            return Result.error("色彩分析失败: " + e.getMessage());
        }
    }

    /**
     * 识别图像中的物体
     * POST /api/ai-image-analysis/objects
     */
    @PostMapping("/objects")
    public Result<List<Map<String, Object>>> detectObjects(@RequestParam("file") MultipartFile file) {
        try {
            log.info("识别图像中的物体请求: filename={}", file.getOriginalFilename());
            List<Map<String, Object>> objects = aiImageAnalysisService.detectObjects(file);
            return Result.success("物体识别成功", objects);
        } catch (Exception e) {
            log.error("识别图像中的物体失败: error={}", e.getMessage());
            return Result.error("物体识别失败: " + e.getMessage());
        }
    }

    /**
     * 获取图像情感分析
     * POST /api/ai-image-analysis/sentiment
     */
    @PostMapping("/sentiment")
    public Result<Map<String, Object>> analyzeImageSentiment(@RequestParam("file") MultipartFile file) {
        try {
            log.info("获取图像情感分析请求: filename={}", file.getOriginalFilename());
            Map<String, Object> sentiment = aiImageAnalysisService.analyzeImageSentiment(file);
            return Result.success("情感分析成功", sentiment);
        } catch (Exception e) {
            log.error("获取图像情感分析失败: error={}", e.getMessage());
            return Result.error("情感分析失败: " + e.getMessage());
        }
    }
}
