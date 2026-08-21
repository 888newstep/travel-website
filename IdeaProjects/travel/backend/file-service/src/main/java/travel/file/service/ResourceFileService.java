package travel.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import travel.common.entity.travel_recommendation.ResourceFile;

import java.util.List;
import java.util.Map;

public interface ResourceFileService extends IService<ResourceFile> {

    ResourceFile uploadResourceFile(MultipartFile file, String category, String description);

    List<ResourceFile> batchUploadResourceFiles(MultipartFile[] files, String category);

    ResourceFile getResourceFile(Long id);

    String downloadResourceFile(Long id);

    boolean deleteResourceFile(Long id);

    ResourceFile updateResourceFile(Long id, ResourceFile file);

    List<ResourceFile> getResourceFileList(String category, int page, int size);

    List<ResourceFile> searchResourceFiles(String keyword, int page, int size);

    String getPreviewUrl(Long id);

    Map<String, Object> getFileStatistics();

    int batchDeleteResourceFiles(List<Long> ids);

    Map<String, Object> getFileTypeStatistics();

    boolean moveFileToCategory(Long fileId, String newCategory);

    List<ResourceFile> getResourceFiles(Long fileId);

    ResourceFile getLatestVersion(Long fileId);

    boolean restoreToVersion(Long fileId, Long versionId);

    Map<String, Object> compareVersions(Long version1Id, Long version2Id);

    List<Map<String, Object>> getVersionHistory(Long fileId, int page, int size);
}
