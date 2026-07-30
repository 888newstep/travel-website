package travel.route.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface AIMultimodalService {

    List<Map<String, Object>> getMultimodalRecommendations(String text, MultipartFile image, int limit);

    List<Map<String, Object>> multimodalSearch(String text, MultipartFile image, int page, int size);
}
