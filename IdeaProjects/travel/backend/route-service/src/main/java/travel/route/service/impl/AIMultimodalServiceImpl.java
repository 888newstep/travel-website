package travel.route.service.impl;

import lombok.extern.slf4j.Slf4j;
import travel.route.service.AIMultimodalService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIMultimodalServiceImpl implements AIMultimodalService {

    @Override
    public List<Map<String, Object>> getMultimodalRecommendations(String text, org.springframework.web.multipart.MultipartFile image, int limit) {
        log.info("获取多模态推荐: text={}, hasImage={}, limit={}", text, image != null, limit);
        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            Map<String, Object> recommendation = new HashMap<>();
            recommendation.put("id", i + 1);
            recommendation.put("title", "推荐景点 " + (i + 1));
            recommendation.put("description", "基于多模态分析的推荐结果");
            recommendation.put("score", 0.9 + (i * 0.05));
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    @Override
    public List<Map<String, Object>> multimodalSearch(String text, org.springframework.web.multipart.MultipartFile image, int page, int size) {
        log.info("多模态搜索: text={}, hasImage={}, page={}, size={}", text, image != null, page, size);
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", (page * size) + i + 1);
            result.put("title", "搜索结果 " + ((page * size) + i + 1));
            result.put("description", "多模态搜索结果");
            result.put("relevance", 0.9 - (i * 0.05));
            results.add(result);
        }

        return results;
    }
}
