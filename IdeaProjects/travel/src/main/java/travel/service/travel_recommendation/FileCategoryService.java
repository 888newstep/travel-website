package travel.service.travel_recommendation;

import travel.entity.travel_recommendation.ResourceFile;

import java.util.List;
import java.util.Map;

public interface FileCategoryService {

    /**
     * 获取所有文件分类
     */
    List<Map<String, Object>> getAllCategories();

    /**
     * 获取指定分类下的文件
     */
    List<ResourceFile> getFilesByCategory(String category);

    /**
     * 为文件添加分类
     */
    boolean addFileToCategory(Integer fileId, String category);

    /**
     * 从分类中移除文件
     */
    boolean removeFileFromCategory(Integer fileId, String category);

    /**
     * 获取文件的所有分类
     */
    List<String> getFileCategories(Integer fileId);

    /**
     * 创建新的文件分类
     */
    boolean createCategory(String categoryName, String description);

    /**
     * 删除文件分类
     */
    boolean deleteCategory(String categoryName);

    /**
     * 更新文件分类
     */
    boolean updateCategory(String oldCategoryName, String newCategoryName, String description);
}
