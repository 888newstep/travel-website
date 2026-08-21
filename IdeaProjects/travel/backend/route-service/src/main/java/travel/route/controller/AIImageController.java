package travel.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.utils.Result;
import travel.route.dto.ai.AIAnalyzeImageRequest;
import travel.route.dto.ai.AIAnalyzeImageResponse;
import travel.route.dto.ai.AIImageAnalysisType;
import travel.route.service.AIImageAnalysisResponseSupport;
import travel.route.service.BaiduAIService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIImageController {

    private static final Logger log = LoggerFactory.getLogger(AIImageController.class);
    private final BaiduAIService baiduAIService;
    private final ObjectMapper objectMapper;

    @Value("${travel.ai.image.allowed-hosts:${AI_IMAGE_ALLOWED_HOSTS:}}")
    private String allowedImageHosts;

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

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
        log.info("图像分析请求: analysisType={}", request.getAnalysisType());
        byte[] imageData = downloadImageFromUrl(request.getImageUrl());
        String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "scene";
        Map<String, Object> result = switch (analysisType.toLowerCase()) {
            case "dish" -> baiduAIService.recognizeDish(imageData);
            case "text", "ocr" -> baiduAIService.recognizeText(imageData);
            default -> baiduAIService.recognizeScene(imageData);
        };
        if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR);
        }
        AIAnalyzeImageResponse response = buildAnalyzeImageResponse(result, analysisType);
        return Result.success("图像分析成功", response);
    }

    private AIAnalyzeImageResponse buildAnalyzeImageResponse(Map<String, Object> result, String analysisType) {
        Map<String, JsonNode> details = AIImageAnalysisResponseSupport.toDynamicDetails(result, objectMapper);
        return AIAnalyzeImageResponse.builder()
                .success(result != null && Boolean.TRUE.equals(result.get("success")))
                .analysisType(analysisType)
                .details(details)
                .error(null)
                .build();
    }

    private byte[] downloadImageFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            if (!"http".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(url.getProtocol())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_FORMAT_ERROR);
            }
            Set<String> allowedHosts = Arrays.stream(allowedImageHosts.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            String host = url.getHost().toLowerCase();
            if (allowedHosts.isEmpty() || !allowedHosts.contains(host)) {
                throw new BusinessException(4006, "图片地址不在允许的域名列表中");
            }
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new BusinessException(4006, "图片地址不可访问内网资源");
                }
            }
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Accept", "image/*");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR);
            }
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new BusinessException(4006, "远程资源不是图片");
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_IMAGE_BYTES) {
                throw new BusinessException(ErrorCodeEnum.PARAM_LENGTH_ERROR);
            }
            try (java.io.InputStream inputStream = connection.getInputStream()) {
                byte[] imageData = inputStream.readNBytes(MAX_IMAGE_BYTES + 1);
                if (imageData.length > MAX_IMAGE_BYTES) {
                    throw new BusinessException(ErrorCodeEnum.PARAM_LENGTH_ERROR);
                }
                return imageData;
            } finally {
                connection.disconnect();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR);
        }
    }
}
