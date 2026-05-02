package travel.service.impl.travel_recommendation;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.ResourceFile;
import travel.service.travel_recommendation.FileCategoryService;
import travel.service.travel_recommendation.ResourceFileService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileCategoryServiceImpl implements FileCategoryService {

    private static final Logger log = LoggerFactory.getLogger(FileCategoryServiceImpl.class);

    private final ResourceFileService resourceFileService;

    // 预定义的文件分类
    private static final List<String> PREDEFINED_CATEGORIES = List.of(
            "图片", "文档", "视频", "音频", "压缩包", "其他"
    );

    @Override
    public List<Map<String, Object>> getAllCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        try {
            for (String category : PREDEFINED_CATEGORIES) {
                Map<String, Object> categoryMap = new HashMap<>();
                categoryMap.put("categoryName", category);
                categoryMap.put("fileCount", getFilesByCategory(category).size());
                categories.add(categoryMap);
            }
        } catch (Exception e) {
            log.error("获取所有文件分类失败", e);
        }
        return categories;
    }

    @Override
    public List<ResourceFile> getFilesByCategory(String category) {
        return resourceFileService.getByCategory(category);
    }

    @Override
    public boolean addFileToCategory(Integer fileId, String category) {
        try {
            ResourceFile resourceFile = resourceFileService.getFileById(fileId);
            if (resourceFile == null) {
                return false;
            }
            resourceFile.setFileCategory(category);
            return resourceFileService.updateById(resourceFile);
        } catch (Exception e) {
            log.error("为文件添加分类失败", e);
            return false;
        }
    }

    @Override
    public boolean removeFileFromCategory(Integer fileId, String category) {
        try {
            ResourceFile resourceFile = resourceFileService.getFileById(fileId);
            if (resourceFile == null) {
                return false;
            }
            if (category.equals(resourceFile.getFileCategory())) {
                resourceFile.setFileCategory(null);
                return resourceFileService.updateById(resourceFile);
            }
            return true;
        } catch (Exception e) {
            log.error("从分类中移除文件失败", e);
            return false;
        }
    }

    @Override
    public List<String> getFileCategories(Integer fileId) {
        List<String> categories = new ArrayList<>();
        try {
            ResourceFile resourceFile = resourceFileService.getFileById(fileId);
            if (resourceFile != null && resourceFile.getFileCategory() != null) {
                categories.add(resourceFile.getFileCategory());
            }
        } catch (Exception e) {
            log.error("获取文件分类失败", e);
        }
        return categories;
    }

    @Override
    public boolean createCategory(String categoryName, String description) {
        try {
            // 这里可以扩展为从数据库中添加分类
            // 目前使用内存中的预定义分类
            return true;
        } catch (Exception e) {
            log.error("创建文件分类失败", e);
            return false;
        }
    }

    @Override
    public boolean deleteCategory(String categoryName) {
        try {
            // 这里可以扩展为从数据库中删除分类
            // 目前使用内存中的预定义分类
            return true;
        } catch (Exception e) {
            log.error("删除文件分类失败", e);
            return false;
        }
    }

    @Override
    public boolean updateCategory(String oldCategoryName, String newCategoryName, String description) {
        try {
            // 这里可以扩展为从数据库中更新分类
            // 目前使用内存中的预定义分类
            return true;
        } catch (Exception e) {
            log.error("更新文件分类失败", e);
            return false;
        }
    }
}
