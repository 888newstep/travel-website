package travel.route.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI图像分析服务接口
 */
public interface AIImageAnalysisService {

    /**
     * 分析图像（MultipartFile版本）
     * @param file 图像文件
     * @param options 分析选项
     * @return 图像分析结果
     */
    Map<String, Object> analyzeImage(MultipartFile file, String options);

    /**
     * 识别景点（MultipartFile版本）
     * @param file 图像文件
     * @return 景点识别结果
     */
    Map<String, Object> recognizeAttraction(MultipartFile file);

    /**
     * 获取相似景点推荐
     * @param file 图像文件
     * @param limit 推荐数量
     * @return 相似景点列表
     */
    List<Map<String, Object>> getSimilarAttractions(MultipartFile file, int limit);

    /**
     * 分析图像标签
     * @param file 图像文件
     * @return 图像标签列表
     */
    List<String> analyzeImageTags(MultipartFile file);

    /**
     * 获取图像描述
     * @param file 图像文件
     * @return 图像描述
     */
    String getImageDescription(MultipartFile file);
}
