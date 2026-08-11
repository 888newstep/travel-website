package travel.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import travel.common.utils.Result;
import travel.route.dto.ai.AIAnalyzeImageRequest;
import travel.route.dto.ai.AIAnalyzeImageResponse;
import travel.route.dto.ai.AIImageAnalysisType;
import travel.route.dto.ai.AISimilarAttractionItem;
import travel.route.service.AIImageAnalysisResponseSupport;
import travel.route.service.AIImageAnalysisService;
import travel.route.service.BaiduAIService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIImageController {

    private static final Logger log = LoggerFactory.getLogger(AIImageController.class);
    private final BaiduAIService baiduAIService;
    private final AIImageAnalysisService aiImageAnalysisService;
    private final ObjectMapper objectMapper;

    @GetMapping("/image-analysis/types")
    @Operation(summary = "获取图像分析类型", description = "返回可用的图像分析类型列表")
    public Result<List<AIImageAnalysisType>> getImageAnalysisTypes() {
        List<AIImageAnalysisType> types = List.of(
                new AIImageAnalysisType("scene", "场景识别"),
                new AIImageAnalysisType("dish", "菜品识别"),
                new AIImageAnalysisType("ocr", "文字识别")
        );
        return Result.success("获取图像分析类型成功", types);
    }

    @PostMapping("/image-analysis")
    @Operation(summary = "图像分析", description = "使用百度AI进行图像识别和分析")
    public Result<AIAnalyzeImageResponse> analyzeImage(@Valid @RequestBody AIAnalyzeImageRequest request) {
        try {
            log.info("图像分析请求: analysisType={}", request.getAnalysisType());

            byte[] imageData = downloadImageFromUrl(request.getImageUrl());
            if (imageData == null) {
                return Result.error("图片下载失败");
            }

            Map<String, Object> result;
            String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "scene";

            switch (analysisType.toLowerCase()) {
                case "dish":
                    result = baiduAIService.recognizeDish(imageData);
                    break;
                case "text":
                case "ocr":
                    result = baiduAIService.recognizeText(imageData);
                    break;
                default:
                    result = baiduAIService.recognizeScene(imageData);
            }

            AIAnalyzeImageResponse response = buildAnalyzeImageResponse(result, analysisType);
            return Result.success("图像分析成功", response);
        } catch (Exception e) {
            log.error("图像分析失败: {}", e.getMessage(), e);
            return Result.error("图像分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/image/analyze")
    @Operation(summary = "图像分析（统一入口）", description = "上传图片进行分析，type=analyze|recognize|tags|description")
    public Result<?> analyzeImageUpload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "analyze") String type) {
        try {
            log.info("图像分析请求: filename={}, type={}", file.getOriginalFilename(), type);
            Object result = switch (type) {
                case "recognize" -> aiImageAnalysisService.recognizeAttraction(file);
                case "tags" -> aiImageAnalysisService.analyzeImageTags(file);
                case "description" -> aiImageAnalysisService.getImageDescription(file);
                default -> aiImageAnalysisService.analyzeImage(file, null);
            };
            return Result.success("图像分析成功", result);
        } catch (Exception e) {
            log.error("图像分析失败: {}", e.getMessage(), e);
            return Result.error("图像分析失败: " + e.getMessage());
        }
    }

    @PostMapping("/image/similar")
    @Operation(summary = "相似景点推荐", description = "根据图片推荐相似景点")
    public Result<List<AISimilarAttractionItem>> getSimilarAttractions(@RequestParam("file") MultipartFile file,
                                                                       @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取相似景点推荐请求: filename={}, limit={}", file.getOriginalFilename(), limit);
            List<AISimilarAttractionItem> attractions = aiImageAnalysisService.getSimilarAttractions(file, limit);
            return Result.success("获取相似景点成功", attractions);
        } catch (Exception e) {
            log.error("获取相似景点推荐失败: {}", e.getMessage(), e);
            return Result.error("获取相似景点失败: " + e.getMessage());
        }
    }

    private AIAnalyzeImageResponse buildAnalyzeImageResponse(Map<String, Object> result, String analysisType) {
        Map<String, JsonNode> details = AIImageAnalysisResponseSupport.toDynamicDetails(result, objectMapper);
        return AIAnalyzeImageResponse.builder()
                .success(result != null && Boolean.TRUE.equals(result.get("success")))
                .analysisType(analysisType)
                .details(details)
                .error(result == null || result.get("error") == null
                        ? null : String.valueOf(result.get("error")))
                .build();
    }

    private byte[] downloadImageFromUrl(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.io.InputStream inputStream = url.openStream();
            byte[] imageData = inputStream.readAllBytes();
            inputStream.close();
            return imageData;
        } catch (Exception e) {
            log.error("下载图片失败: {}", e.getMessage());
            return null;
        }
    }
}
