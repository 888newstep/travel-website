package travel.service.travel_recommendation;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.travel_recommendation.FileTag;
import java.util.List;
import java.util.Map;

public interface FileTagService extends IService<FileTag> {

    List<FileTag> getByFileId(Integer fileId);

    List<FileTag> getByUserId(Integer userId);

    List<FileTag> getByTagName(String tagName);

    List<FileTag> getByTagType(String tagType);

    List<FileTag> getPopularTags(Integer limit);

    boolean addTag(Integer fileId, String tagName, String tagType, Integer userId);

    boolean removeTag(Integer fileId, String tagName);

    boolean batchAddTags(Integer fileId, List<String> tagNames, String tagType, Integer userId);

    boolean batchRemoveTags(Integer fileId, List<String> tagNames);

    boolean updateTagUsageCount(Integer tagId);

    FileTag createFileTag(FileTag fileTag);

    FileTag updateFileTag(Long id, FileTag fileTag);

    boolean deleteFileTag(Long id);

    FileTag getFileTag(Long id);

    List<FileTag> getAllCategories();

    List<Map<String, Object>> getCategoryTree();

    List<FileTag> getChildCategories(Long parentId);

    List<FileTag> searchCategories(String keyword);

    boolean moveCategory(Long categoryId, Long newParentId);

    Map<String, Object> getCategoryStatistics(Long id);

    List<FileTag> batchCreateCategories(List<FileTag> categories);
}