package travel.route.service;

import travel.route.dto.ai.AIImageAnalysisResponse;
import travel.route.dto.ai.AIRecognizeAttractionResponse;
import travel.route.dto.ai.AISimilarAttractionItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AIImageAnalysisService {

    AIImageAnalysisResponse analyzeImage(MultipartFile file, String options);

    AIRecognizeAttractionResponse recognizeAttraction(MultipartFile file);

    List<AISimilarAttractionItem> getSimilarAttractions(MultipartFile file, int limit);

    List<String> analyzeImageTags(MultipartFile file);

    String getImageDescription(MultipartFile file);
}