package travel.service.travel_recommendation;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI图像分析服务接口
 */
public interface AIImageAnalysisService {

    /**
     * 分析图像内容
     * @param imageData 图像数据
     * @param userId 用户ID
     * @return 图像分析结果
     */
    Map<String, Object> analyzeImage(byte[] imageData, Integer userId);

    /**
     * 识别景点
     * @param imageData 图像数据
     * @return 景点识别结果
     */
    Map<String, Object> recognizeAttraction(byte[] imageData);

    /**
     * 识别美食
     * @param imageData 图像数据
     * @return 美食识别结果
     */
    Map<String, Object> recognizeFood(byte[] imageData);

    /**
     * 生成图像描述
     * @param imageData 图像数据
     * @param language 语言
     * @return 图像描述
     */
    Map<String, Object> generateImageDescription(byte[] imageData, String language);

    /**
     * 分析图像质量
     * @param imageData 图像数据
     * @return 图像质量分析
     */
    Map<String, Object> analyzeImageQuality(byte[] imageData);

    /**
     * 批量分析图像
     * @param imageDataList 图像数据列表
     * @param userId 用户ID
     * @return 批量分析结果
     */
    List<Map<String, Object>> batchAnalyzeImages(List<byte[]> imageDataList, Integer userId);

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

    /**
     * 批量分析图像（MultipartFile版本）
     * @param files 图像文件数组
     * @param options 分析选项
     * @return 批量分析结果
     */
    List<Map<String, Object>> batchAnalyzeImages(MultipartFile[] files, String options);

    /**
     * 评估图像质量
     * @param file 图像文件
     * @return 图像质量评估
     */
    Map<String, Object> assessImageQuality(MultipartFile file);

    /**
     * 分析图像色彩
     * @param file 图像文件
     * @return 色彩分析结果
     */
    Map<String, Object> analyzeImageColors(MultipartFile file);

    /**
     * 识别图像中的物体
     * @param file 图像文件
     * @return 物体识别结果
     */
    List<Map<String, Object>> detectObjects(MultipartFile file);

    /**
     * 分析图像情感
     * @param file 图像文件
     * @return 情感分析结果
     */
    Map<String, Object> analyzeImageSentiment(MultipartFile file);
}
