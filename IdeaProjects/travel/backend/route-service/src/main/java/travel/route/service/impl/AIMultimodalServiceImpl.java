
package travel.route.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import travel.route.dto.ai.AIMultimodalItem;
import travel.route.service.AIMultimodalService;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIMultimodalServiceImpl implements AIMultimodalService {

    private static final Logger log = LoggerFactory.getLogger(AIMultimodalServiceImpl.class);

    @Override
    public List<AIMultimodalItem> getMultimodalRecommendations(String text, MultipartFile image, int limit) {
        log.info("获取多模态推荐: text={}, hasImage={}, limit={}", text, image != null, limit);
        List<AIMultimodalItem> recommendations = new ArrayList<>();

        for (int i = 0; i < Math.max(limit, 0); i++) {
            recommendations.add(AIMultimodalItem.builder()
                    .id(i + 1)
                    .title("推荐景点 " + (i + 1))
                    .description("基于多模态分析的推荐结果")
                    .score(0.9 + (i * 0.05))
                    .relevance(0.95 - (i * 0.03))
                    .build());
        }

        return recommendations;
    }

    @Override
    public List<AIMultimodalItem> multimodalSearch(String text, MultipartFile image, int page, int size) {
        log.info("多模态搜索: text={}, hasImage={}, page={}, size={}", text, image != null, page, size);
        List<AIMultimodalItem> results = new ArrayList<>();

        for (int i = 0; i < Math.max(size, 0); i++) {
            int itemId = (page * size) + i + 1;
            results.add(AIMultimodalItem.builder()
                    .id(itemId)
                    .title("搜索结果 " + itemId)
                    .description("多模态搜索结果")
                    .score(0.9 - (i * 0.05))
                    .relevance(0.88 - (i * 0.04))
                    .build());
        }

        return results;
    }
}
