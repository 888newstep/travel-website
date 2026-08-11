package travel.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import travel.common.utils.Result;
import travel.route.dto.ai.AIMultimodalItem;
import travel.route.dto.ai.AIMultimodalQueryRequest;
import travel.route.dto.ai.AIMultimodalQueryResponse;
import travel.route.service.AIMultimodalService;
import travel.route.service.QwenService;

import java.util.List;

@RestController
@RequestMapping("/ai/multimodal")
@RequiredArgsConstructor
public class AIMultimodalController {

    private static final Logger log = LoggerFactory.getLogger(AIMultimodalController.class);
    private final AIMultimodalService aiMultimodalService;
    private final QwenService qwenService;

    @PostMapping("/query")
    @Operation(summary = "多模态查询", description = "支持文本、图像的多模态查询")
    public Result<AIMultimodalQueryResponse> multimodalQuery(@Valid @RequestBody AIMultimodalQueryRequest request) {
        try {
            log.info("多模态查询请求: text={}, hasImage={}", request.getText(), request.getImage() != null);

            StringBuilder query = new StringBuilder();
            if (request.getText() != null && !request.getText().isEmpty()) {
                query.append(request.getText());
            }
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                query.append(" [附带图片]");
            }

            String response = qwenService.chatCompletion(query.toString(),
                    "你是一个多模态旅游助手，可以处理文本和图片信息。");

            AIMultimodalQueryResponse result = AIMultimodalQueryResponse.builder()
                    .response(response)
                    .queryType("multimodal")
                    .source("qwen")
                    .build();

            return Result.success("多模态查询成功", result);
        } catch (Exception e) {
            log.error("多模态查询失败: {}", e.getMessage(), e);
            return Result.error("多模态查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/recommend")
    @Operation(summary = "多模态推荐", description = "基于文本、图像的推荐")
    public Result<List<AIMultimodalItem>> getMultimodalRecommendations(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("获取多模态推荐请求");
            List<AIMultimodalItem> recommendations = aiMultimodalService.getMultimodalRecommendations(text, image, limit);
            return Result.success("获取推荐成功", recommendations);
        } catch (Exception e) {
            log.error("获取多模态推荐失败: {}", e.getMessage(), e);
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    @Operation(summary = "多模态搜索", description = "基于文本和图像的搜索")
    public Result<List<AIMultimodalItem>> multimodalSearch(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("多模态搜索请求");
            List<AIMultimodalItem> results = aiMultimodalService.multimodalSearch(text, image, page, size);
            return Result.success("搜索成功", results);
        } catch (Exception e) {
            log.error("多模态搜索失败: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}
