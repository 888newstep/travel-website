package travel.service.travel_recommendation;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface AIMultimodalService {

    Map<String, Object> multimodalChat(Map<String, Object> request, String sessionId);

    Map<String, Object> voiceInteraction(byte[] audioData, String sessionId);

    Map<String, Object> textImageInteraction(String text, byte[] imageData, String sessionId);

    Map<String, Object> getSessionHistory(String sessionId);

    Map<String, Object> endSession(String sessionId);

    List<Map<String, Object>> getMultimodalRecommendations(String text, MultipartFile image, MultipartFile audio, int limit);

    Map<String, Object> understandContent(String text, MultipartFile image, MultipartFile audio);

    List<Map<String, Object>> getTextImageRecommendations(String text, MultipartFile image, int limit);

    List<Map<String, Object>> multimodalSearch(String text, MultipartFile image, int page, int size);

    Map<String, Object> generateContent(Map<String, Object> generateRequest);

    Map<String, Object> compareContent(Map<String, Object> compareRequest);

    String summarizeContent(String text, MultipartFile image, MultipartFile audio);

    Map<String, Object> analyzeSentiment(String text, MultipartFile image, MultipartFile audio);

    Map<String, Object> multimodalQA(Map<String, Object> qaRequest);

    Map<String, Object> getMultimodalReport(String text, MultipartFile image, MultipartFile audio);
}