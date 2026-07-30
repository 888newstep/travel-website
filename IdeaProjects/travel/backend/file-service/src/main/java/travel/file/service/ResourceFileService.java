package travel.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.travel_recommendation.ResourceFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResourceFileService extends IService<ResourceFile> {

    Map<String, Object> uploadFile(MultipartFile file, Integer userId, String description);

    List<Map<String, Object>> batchUploadFiles(List<MultipartFile> files, Integer userId, String description);

    List<ResourceFile> getByUserId(Integer userId);

    List<ResourceFile> getByFileType(String fileType);

    List<ResourceFile> getByStatus(Integer status);

    List<ResourceFile> getByRouteId(Integer routeId);

    List<ResourceFile> getByTags(String tags);

    List<ResourceFile> getByCategory(String category);

    List<ResourceFile> searchByFileName(String fileName);

    List<ResourceFile> searchByMultipleConditions(String fileName, String fileType, String tags, Integer userId, Integer routeId);

    List<ResourceFile> getHotFiles(Integer limit);

    List<ResourceFile> getNewFiles(Integer limit);

    boolean deleteFile(Integer fileId);

    boolean batchDeleteFiles(List<Integer> fileIds);

    ResourceFile getFileById(Integer fileId);

    boolean associateWithRoute(Integer fileId, Integer routeId);

    boolean dissociateFromRoute(Integer fileId);

    List<Map<String, Object>> getFileTags(Integer fileId);

    boolean addFileTag(Integer fileId, String tagName, Integer userId);

    boolean removeFileTag(Integer fileId, String tagName);

    List<Map<String, Object>> getFileComments(Integer fileId);

    boolean addFileComment(Integer fileId, Integer userId, String userName, String content, Integer rating, Integer parentId);

    boolean likeComment(Integer commentId);

    Double getFileAverageRating(Integer fileId);

    Map<String, Object> getFileStatistics(Integer fileId);

    boolean updateFileMetadata(Integer fileId, String fileName, String description, String tags);

    boolean incrementDownloadCount(Integer fileId);

    Map<String, Object> generateFileShareUrl(Integer fileId, Integer expireHours);

    ResourceFile getFileByShareUrl(String shareUrl);

    boolean cancelFileShare(Integer fileId);

    Map<String, Object> getFilePreviewUrl(Integer fileId);

    List<ResourceFile> getFileVersions(Integer fileId);

    boolean revertToVersion(Integer fileId, Integer version);

    boolean incrementViewCount(Integer fileId);

    List<ResourceFile> getByParentFileId(Integer parentFileId);

    ResourceFile createResourceFile(ResourceFile version);

    ResourceFile getResourceFile(Long id);

    List<ResourceFile> getResourceFiles(Long fileId);

    ResourceFile getLatestVersion(Long fileId);

    boolean deleteResourceFile(Long id);

    boolean restoreToVersion(Long fileId, Long versionId);

    Map<String, Object> compareVersions(Long version1Id, Long version2Id);

    List<Map<String, Object>> getVersionHistory(Long fileId, int page, int size);

    Map<String, Object> getVersionStatistics(Long fileId);

    boolean updateVersionNote(Long id, String note);

    int batchDeleteVersions(List<Long> ids);

    // 以下是Controller中使用的方法

    ResourceFile uploadResourceFile(MultipartFile file, String category, String description);

    List<ResourceFile> batchUploadResourceFiles(MultipartFile[] files, String category);

    String downloadResourceFile(Long id);

    ResourceFile updateResourceFile(Long id, ResourceFile file);

    List<ResourceFile> getResourceFileList(String category, int page, int size);

    List<ResourceFile> searchResourceFiles(String keyword, int page, int size);

    String getPreviewUrl(Long id);

    Map<String, Object> getFileStatistics();

    int batchDeleteResourceFiles(List<Long> ids);

    Map<String, Object> getFileTypeStatistics();

    boolean moveFileToCategory(Long fileId, String newCategory);
}