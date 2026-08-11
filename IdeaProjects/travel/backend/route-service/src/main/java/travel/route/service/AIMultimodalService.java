package travel.route.service;

import travel.route.dto.ai.AIMultimodalItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AIMultimodalService {

    List<AIMultimodalItem> getMultimodalRecommendations(String text, MultipartFile image, int limit);

    List<AIMultimodalItem> multimodalSearch(String text, MultipartFile image, int page, int size);
}