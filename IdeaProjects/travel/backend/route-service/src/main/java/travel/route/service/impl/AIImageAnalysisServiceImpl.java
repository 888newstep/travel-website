package travel.route.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AIImageAnalysisResponse;
import travel.route.dto.ai.AIRecognizeAttractionResponse;
import travel.route.dto.ai.AISimilarAttractionItem;
import travel.route.service.AIImageAnalysisResponseSupport;
import travel.route.service.AIImageAnalysisService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AIImageAnalysisServiceImpl implements AIImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AIImageAnalysisServiceImpl.class);
    private static final String IMAGE_PREFIX = "ai:image:";

    private final CacheUtil cacheUtil;

    public AIImageAnalysisServiceImpl(CacheUtil cacheUtil) {
        this.cacheUtil = cacheUtil;
    }

    @Override
    public AIImageAnalysisResponse analyzeImage(MultipartFile file, String options) {
        try {
            byte[] imageData = file.getBytes();
            return AIImageAnalysisResponseSupport.toImageAnalysisResponse(analyzeImageInternal(imageData, null));
        } catch (Exception e) {
            log.error("Analyze image failed", e);
            return AIImageAnalysisResponseSupport.buildAnalysisFailure(e.getMessage());
        }
    }

    @Override
    public AIRecognizeAttractionResponse recognizeAttraction(MultipartFile file) {
        try {
            byte[] imageData = file.getBytes();
            return AIImageAnalysisResponseSupport.toRecognizeAttractionResponse(recognizeAttractionInternal(imageData));
        } catch (Exception e) {
            log.error("Recognize attraction failed", e);
            return AIImageAnalysisResponseSupport.buildRecognizeFailure(e.getMessage());
        }
    }

    @Override
    public List<AISimilarAttractionItem> getSimilarAttractions(MultipartFile file, int limit) {
        List<AISimilarAttractionItem> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.max(limit, 0); i++) {
            recommendations.add(AISimilarAttractionItem.builder()
                    .id(i + 1)
                    .name("Similar attraction " + (i + 1))
                    .description("Recommended by image similarity analysis")
                    .score(0.9 + (i * 0.05))
                    .build());
        }
        return recommendations;
    }

    @Override
    public List<String> analyzeImageTags(MultipartFile file) {
        return List.of("\u98ce\u666f", "\u81ea\u7136", "\u65c5\u6e38", "\u6444\u5f71", "\u6237\u5916");
    }

    @Override
    public String getImageDescription(MultipartFile file) {
        return "\u8fd9\u662f\u4e00\u5f20\u5c55\u793a\u81ea\u7136\u98ce\u5149\u7684\u65c5\u6e38\u7167\u7247\u3002";
    }

    private Map<String, Object> analyzeImageInternal(byte[] imageData, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        String cacheKey = IMAGE_PREFIX + "analysis:" + java.util.Arrays.hashCode(imageData);
        Object cached = cacheUtil.get(cacheKey, Object.class);
        if (cached instanceof Map<?, ?> cachedMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) cachedMap;
            return typedMap;
        }

        result.put("success", true);
        result.put("analysisType", "comprehensive");
        result.put("timestamp", LocalDateTime.now());

        Map<String, Object> contentAnalysis = new HashMap<>();
        contentAnalysis.put("scene", "\u81ea\u7136\u98ce\u666f");
        contentAnalysis.put("objects", List.of("\u5c71\u5cf0", "\u6e56\u6cca", "\u6811\u6728"));
        contentAnalysis.put("colors", List.of("\u84dd\u8272", "\u7eff\u8272", "\u767d\u8272"));
        contentAnalysis.put("features", List.of("\u5f00\u9626\u89c6\u91ce", "\u5149\u7ebf\u5145\u8db3", "\u7a7a\u6c14\u901a\u900f"));
        result.put("contentAnalysis", contentAnalysis);

        Map<String, Object> qualityAnalysis = new HashMap<>();
        qualityAnalysis.put("sharpness", 0.91);
        qualityAnalysis.put("brightness", 0.82);
        qualityAnalysis.put("contrast", 0.85);
        qualityAnalysis.put("composition", 0.88);
        qualityAnalysis.put("overallQuality", 0.87);
        result.put("qualityAnalysis", qualityAnalysis);

        List<Map<String, Object>> recommendations = new ArrayList<>();
        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("type", "route");
        rec1.put("name", "\u81ea\u7136\u89c2\u5149\u8def\u7ebf");
        rec1.put("items", List.of("\u5c71\u666f\u6b65\u9053", "\u89c2\u666f\u5e73\u53f0"));
        rec1.put("tips", List.of("\u9002\u5408\u4e0a\u5348\u524d\u5f80", "\u8bb0\u5f97\u643a\u5e26\u9632\u6652\u7528\u54c1"));
        recommendations.add(rec1);
        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("type", "photography");
        rec2.put("name", "\u6444\u5f71\u5efa\u8bae");
        rec2.put("items", List.of("\u5e7f\u89d2\u62cd\u6444", "\u4f7f\u7528\u524d\u666f\u589e\u5f3a\u5c42\u6b21"));
        rec2.put("tips", List.of("\u9ec4\u660f\u65f6\u5206\u5149\u7ebf\u66f4\u67d4\u548c"));
        recommendations.add(rec2);
        result.put("recommendations", recommendations);

        result.put("confidence", 0.92);
        cacheUtil.set(cacheKey, result, 24, TimeUnit.HOURS);
        return result;
    }

    private Map<String, Object> recognizeAttractionInternal(byte[] imageData) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("type", "landmark-recognition");
        result.put("timestamp", LocalDateTime.now());

        List<Map<String, Object>> attractions = new ArrayList<>();
        Map<String, Object> attraction1 = new HashMap<>();
        attraction1.put("name", "\u897f\u6e56");
        attraction1.put("confidence", 0.96);
        attraction1.put("location", "Hangzhou");
        attraction1.put("description", "\u4ee5\u6e56\u5149\u5c71\u8272\u95fb\u540d\u7684\u7ecf\u5178\u666f\u70b9");
        attraction1.put("rating", 4.9);
        attractions.add(attraction1);

        Map<String, Object> attraction2 = new HashMap<>();
        attraction2.put("name", "\u5343\u5c9b\u6e56");
        attraction2.put("confidence", 0.82);
        attraction2.put("location", "Hangzhou");
        attraction2.put("description", "\u9002\u5408\u5c71\u6c34\u89c2\u5149\u4e0e\u6e38\u8239\u4f53\u9a8c");
        attraction2.put("rating", 4.7);
        attractions.add(attraction2);

        result.put("attractions", attractions);
        result.put("topMatch", attraction1);
        return result;
    }
}