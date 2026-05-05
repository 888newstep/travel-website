package travel.service.impl.travel_recommendation;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.FileComment;
import travel.entity.travel_recommendation.FileTag;
import travel.entity.travel_recommendation.ResourceFile;
import travel.mapper.user_community_mapper.FileCommentMapper;
import travel.mapper.user_community_mapper.FileTagMapper;
import travel.mapper.travel_recommendation_mapper.ResourceFileMapper;
import travel.service.travel_recommendation.ResourceFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceFileServiceImpl extends ServiceImpl<ResourceFileMapper, ResourceFile> implements ResourceFileService {

    private final ResourceFileMapper resourceFileMapper;
    private final FileTagMapper fileTagMapper;
    private final FileCommentMapper fileCommentMapper;

    private static final String RESOURCE_DIR = "c:/resources/";
    private static final String RESOURCE_PATH = "/resources/";

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, Integer userId, String description) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("msg", "文件为空");
                return result;
            }

            File dir = new File(RESOURCE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                result.put("success", false);
                result.put("msg", "文件名不能为空");
                return result;
            }
            String fileName = UUID.randomUUID() + "__" + originalFilename;
            String filePath = RESOURCE_DIR + fileName;

            file.transferTo(new File(filePath));

            String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

            ResourceFile resourceFile = new ResourceFile();
            resourceFile.setFileId(UUID.randomUUID().toString());
            resourceFile.setFileName(originalFilename);
            resourceFile.setFilePath(RESOURCE_PATH + fileName);
            resourceFile.setFileSize(file.getSize());
            resourceFile.setFileType(fileType);
            resourceFile.setUploadTime(LocalDateTime.now());
            resourceFile.setUploadUserId(userId);
            resourceFile.setDescription(description);
            resourceFile.setStatus(1);
            resourceFile.setDownloadCount(0);
            resourceFile.setCommentCount(0);
            resourceFile.setRating(0.0);
            resourceFile.setVersion(1);
            resourceFile.setCreatedAt(LocalDateTime.now());
            resourceFile.setUpdatedAt(LocalDateTime.now());

            boolean saved = save(resourceFile);
            if (saved) {
                result.put("success", true);
                result.put("fileId", resourceFile.getId());
                result.put("fileName", originalFilename);
                result.put("filePath", resourceFile.getFilePath());
            } else {
                new File(filePath).delete();
                result.put("success", false);
                result.put("msg", "文件上传失败，数据库保存失败");
            }
        } catch (Exception e) {
            log.error("文件上传失败", e);
            result.put("success", false);
            result.put("msg", "文件上传失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> batchUploadFiles(List<MultipartFile> files, Integer userId, String description) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadFile(file, userId, description));
        }
        return results;
    }

    @Override
    public List<ResourceFile> getByUserId(Integer userId) {
        return resourceFileMapper.selectByUserId(userId);
    }

    @Override
    public List<ResourceFile> getByFileType(String fileType) {
        return resourceFileMapper.selectByFileType(fileType);
    }

    @Override
    public List<ResourceFile> getByStatus(Integer status) {
        return resourceFileMapper.selectByStatus(status);
    }

    @Override
    public List<ResourceFile> getByRouteId(Integer routeId) {
        return resourceFileMapper.selectByRouteId(routeId);
    }

    @Override
    public List<ResourceFile> getByTags(String tags) {
        return resourceFileMapper.selectByTags(tags);
    }

    @Override
    public List<ResourceFile> searchByFileName(String fileName) {
        return resourceFileMapper.selectByFileName(fileName);
    }

    @Override
    public List<ResourceFile> searchByMultipleConditions(String fileName, String fileType, String tags, Integer userId, Integer routeId) {
        Map<String, Object> conditions = new HashMap<>();
        if (fileName != null) conditions.put("fileName", fileName);
        if (fileType != null) conditions.put("fileType", fileType);
        if (tags != null) conditions.put("tags", tags);
        if (userId != null) conditions.put("userId", userId);
        if (routeId != null) conditions.put("routeId", routeId);
        return resourceFileMapper.selectByMultipleConditions(conditions);
    }

    @Override
    public List<ResourceFile> getHotFiles(Integer limit) {
        return resourceFileMapper.selectHotFiles(limit);
    }

    @Override
    public List<ResourceFile> getNewFiles(Integer limit) {
        return resourceFileMapper.selectNewFiles(limit);
    }

    @Override
    public boolean deleteFile(Integer fileId) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return false;
            }

            String filePath = RESOURCE_DIR + resourceFile.getFilePath().substring(RESOURCE_PATH.length());
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            return removeById(fileId);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return false;
        }
    }

    @Override
    public boolean batchDeleteFiles(List<Integer> fileIds) {
        try {
            for (Integer fileId : fileIds) {
                deleteFile(fileId);
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            return false;
        }
    }

    @Override
    public ResourceFile getFileById(Integer fileId) {
        return getById(fileId);
    }

    @Override
    public boolean associateWithRoute(Integer fileId, Integer routeId) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return false;
            }
            resourceFile.setRouteId(routeId);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("关联路线失败", e);
            return false;
        }
    }

    @Override
    public boolean dissociateFromRoute(Integer fileId) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return false;
            }
            resourceFile.setRouteId(null);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("解除路线关联失败", e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getFileTags(Integer fileId) {
        List<Map<String, Object>> tags = new ArrayList<>();
        try {
            List<FileTag> fileTags = fileTagMapper.selectByFileId(fileId);
            for (FileTag tag : fileTags) {
                Map<String, Object> tagMap = new HashMap<>();
                tagMap.put("id", tag.getId());
                tagMap.put("tagName", tag.getTagName());
                tagMap.put("tagType", tag.getTagType());
                tagMap.put("userId", tag.getUserId());
                tagMap.put("usageCount", tag.getUsageCount());
                tagMap.put("createTime", tag.getCreateTime());
                tags.add(tagMap);
            }
        } catch (Exception e) {
            log.error("获取文件标签失败", e);
        }
        return tags;
    }

    @Override
    public boolean addFileTag(Integer fileId, String tagName, Integer userId) {
        try {
            FileTag fileTag = new FileTag();
            fileTag.setTagName(tagName);
            fileTag.setTagType("general");
            fileTag.setFileId(fileId);
            fileTag.setUserId(userId);
            fileTag.setUsageCount(1);
            fileTag.setCreateTime(LocalDateTime.now());
            fileTag.setUpdateTime(LocalDateTime.now());
            return fileTagMapper.insert(fileTag) > 0;
        } catch (Exception e) {
            log.error("添加文件标签失败", e);
            return false;
        }
    }

    @Override
    public boolean removeFileTag(Integer fileId, String tagName) {
        try {
            List<FileTag> tags = fileTagMapper.selectByFileId(fileId);
            for (FileTag tag : tags) {
                if (tag.getTagName().equals(tagName)) {
                    return fileTagMapper.deleteById(tag.getId()) > 0;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("移除文件标签失败", e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getFileComments(Integer fileId) {
        List<Map<String, Object>> comments = new ArrayList<>();
        try {
            List<FileComment> fileComments = fileCommentMapper.selectByFileId(fileId);
            for (FileComment comment : fileComments) {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("content", comment.getContent());
                commentMap.put("userName", comment.getUserName());
                commentMap.put("rating", comment.getRating());
                commentMap.put("parentId", comment.getParentId());
                commentMap.put("likes", comment.getLikes());
                commentMap.put("createTime", comment.getCreateTime());
                comments.add(commentMap);
            }
        } catch (Exception e) {
            log.error("获取文件评论失败", e);
        }
        return comments;
    }

    @Override
    public boolean addFileComment(Integer fileId, Integer userId, String userName, String content, Integer rating, Integer parentId) {
        try {
            FileComment comment = new FileComment();
            comment.setFileId(fileId);
            comment.setUserId(userId);
            comment.setUserName(userName);
            comment.setContent(content);
            comment.setRating(rating);
            comment.setParentId(parentId != null ? parentId : 0);
            comment.setLikes(0);
            comment.setStatus(1);
            comment.setCreateTime(LocalDateTime.now());
            comment.setUpdateTime(LocalDateTime.now());
            boolean added = fileCommentMapper.insert(comment) > 0;
            if (added) {
                ResourceFile resourceFile = getById(fileId);
                if (resourceFile != null) {
                    resourceFile.setCommentCount(resourceFile.getCommentCount() + 1);
                    double avgRating = fileCommentMapper.selectAverageRatingByFileId(fileId);
                    resourceFile.setRating(avgRating);
                    updateById(resourceFile);
                }
            }
            return added;
        } catch (Exception e) {
            log.error("添加文件评论失败", e);
            return false;
        }
    }

    @Override
    public boolean likeComment(Integer commentId) {
        try {
            return fileCommentMapper.incrementLikes(commentId) > 0;
        } catch (Exception e) {
            log.error("点赞评论失败", e);
            return false;
        }
    }

    @Override
    public Double getFileAverageRating(Integer fileId) {
        try {
            return fileCommentMapper.selectAverageRatingByFileId(fileId);
        } catch (Exception e) {
            log.error("获取文件平均评分失败", e);
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getFileStatistics(Integer fileId) {
        Map<String, Object> statistics = new HashMap<>();
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return statistics;
            }
            statistics.put("downloadCount", resourceFile.getDownloadCount());
            statistics.put("commentCount", resourceFile.getCommentCount());
            statistics.put("rating", resourceFile.getRating());
            statistics.put("uploadTime", resourceFile.getUploadTime());
            statistics.put("lastAccessTime", resourceFile.getLastAccessTime());
            return statistics;
        } catch (Exception e) {
            log.error("获取文件统计信息失败", e);
            return statistics;
        }
    }

    @Override
    public boolean updateFileMetadata(Integer fileId, String fileName, String description, String tags) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return false;
            }
            if (fileName != null) {
                resourceFile.setFileName(fileName);
            }
            if (description != null) {
                resourceFile.setDescription(description);
            }
            if (tags != null) {
                resourceFile.setTags(tags);
            }
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("更新文件元数据失败", e);
            return false;
        }
    }

    @Override
    public boolean incrementDownloadCount(Integer fileId) {
        try {
            return resourceFileMapper.incrementDownloadCount(fileId) > 0;
        } catch (Exception e) {
            log.error("增加下载次数失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> generateFileShareUrl(Integer fileId, Integer expireHours) {
        Map<String, Object> result = new HashMap<>();
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                result.put("success", false);
                result.put("msg", "文件不存在");
                return result;
            }

            String shareToken = UUID.randomUUID().toString().replace("-", "");
            String shareUrl = "/api/resources/share/" + shareToken;
            LocalDateTime expireTime = LocalDateTime.now().plusHours(expireHours != null ? expireHours : 24);

            resourceFile.setShareUrl(shareUrl);
            resourceFile.setShareExpireTime(expireTime);
            boolean updated = updateById(resourceFile);

            if (updated) {
                result.put("success", true);
                result.put("shareUrl", shareUrl);
                result.put("expireTime", expireTime);
                result.put("fileName", resourceFile.getFileName());
            } else {
                result.put("success", false);
                result.put("msg", "生成分享链接失败");
            }
        } catch (Exception e) {
            log.error("生成文件分享链接失败", e);
            result.put("success", false);
            result.put("msg", "生成分享链接失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public ResourceFile getFileByShareUrl(String shareUrl) {
        try {
            List<ResourceFile> files = list();
            for (ResourceFile file : files) {
                if (shareUrl.equals(file.getShareUrl()) && file.getShareExpireTime() != null && file.getShareExpireTime().isAfter(LocalDateTime.now())) {
                    return file;
                }
            }
        } catch (Exception e) {
            log.error("通过分享链接获取文件失败", e);
        }
        return null;
    }

    @Override
    public boolean cancelFileShare(Integer fileId) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return false;
            }
            resourceFile.setShareUrl(null);
            resourceFile.setShareExpireTime(null);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("取消文件分享失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getFilePreviewUrl(Integer fileId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                result.put("success", false);
                result.put("msg", "文件不存在");
                return result;
            }

            String previewUrl = resourceFile.getPreviewUrl();
            if (previewUrl == null) {
                previewUrl = "/api/resources/preview/" + fileId;
                resourceFile.setPreviewUrl(previewUrl);
                updateById(resourceFile);
            }

            result.put("success", true);
            result.put("previewUrl", previewUrl);
            result.put("fileName", resourceFile.getFileName());
            result.put("fileType", resourceFile.getFileType());
        } catch (Exception e) {
            log.error("获取文件预览URL失败", e);
            result.put("success", false);
            result.put("msg", "获取预览URL失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<ResourceFile> getFileVersions(Integer fileId) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return new ArrayList<>();
            }

            Integer parentId = resourceFile.getParentFileId() != null ? resourceFile.getParentFileId() : fileId;
            return resourceFileMapper.selectByParentFileId(parentId);
        } catch (Exception e) {
            log.error("获取文件版本失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean revertToVersion(Integer fileId, Integer version) {
        try {
            List<ResourceFile> versions = getFileVersions(fileId);
            for (ResourceFile versionFile : versions) {
                if (versionFile.getVersion().equals(version)) {
                    ResourceFile currentFile = getById(fileId);
                    if (currentFile != null) {
                        currentFile.setFileName(versionFile.getFileName());
                        currentFile.setFilePath(versionFile.getFilePath());
                        currentFile.setFileSize(versionFile.getFileSize());
                        currentFile.setFileType(versionFile.getFileType());
                        currentFile.setDescription(versionFile.getDescription());
                        currentFile.setVersion(currentFile.getVersion() + 1);
                        return updateById(currentFile);
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("回滚到历史版本失败", e);
            return false;
        }
    }

    @Override
    public boolean incrementViewCount(Integer fileId) {
        try {
            ResourceFile resourceFile = getById(fileId);
            if (resourceFile == null) {
                return false;
            }
            resourceFile.setViewCount(resourceFile.getViewCount() + 1);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("增加文件浏览次数失败", e);
            return false;
        }
    }

    @Override
    public List<ResourceFile> getByCategory(String category) {
        try {
            return resourceFileMapper.selectByCategory(category);
        } catch (Exception e) {
            log.error("按分类获取文件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ResourceFile> getByParentFileId(Integer parentFileId) {
        try {
            return resourceFileMapper.selectByParentFileId(parentFileId);
        } catch (Exception e) {
            log.error("按父文件ID获取文件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ResourceFile createResourceFile(ResourceFile version) {
        try {
            version.setFileId(UUID.randomUUID().toString());
            version.setVersion(1);
            version.setCreatedAt(LocalDateTime.now());
            version.setUpdatedAt(LocalDateTime.now());
            save(version);
            return version;
        } catch (Exception e) {
            log.error("创建文件版本失败", e);
            return null;
        }
    }

    @Override
    public ResourceFile getResourceFile(Long id) {
        return getById(id.intValue());
    }

    @Override
    public List<ResourceFile> getResourceFiles(Long fileId) {
        return getFileVersions(fileId.intValue());
    }

    @Override
    public ResourceFile getLatestVersion(Long fileId) {
        try {
            List<ResourceFile> versions = getFileVersions(fileId.intValue());
            if (versions.isEmpty()) {
                return null;
            }
            ResourceFile latest = versions.get(0);
            for (ResourceFile v : versions) {
                if (v.getVersion() != null && latest.getVersion() != null && v.getVersion() > latest.getVersion()) {
                    latest = v;
                }
            }
            return latest;
        } catch (Exception e) {
            log.error("获取最新版本失败", e);
            return null;
        }
    }

    @Override
    public boolean deleteResourceFile(Long id) {
        return deleteFile(id.intValue());
    }

    @Override
    public boolean restoreToVersion(Long fileId, Long versionId) {
        return revertToVersion(fileId.intValue(), versionId.intValue());
    }

    @Override
    public Map<String, Object> compareVersions(Long version1Id, Long version2Id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ResourceFile v1 = getById(version1Id.intValue());
            ResourceFile v2 = getById(version2Id.intValue());
            if (v1 == null || v2 == null) {
                result.put("success", false);
                result.put("msg", "版本不存在");
                return result;
            }
            result.put("success", true);
            result.put("version1", v1);
            result.put("version2", v2);
            result.put("fileNameDiff", !v1.getFileName().equals(v2.getFileName()));
            result.put("fileSizeDiff", !v1.getFileSize().equals(v2.getFileSize()));
            result.put("descriptionDiff", !v1.getDescription().equals(v2.getDescription()));
        } catch (Exception e) {
            log.error("比较版本失败", e);
            result.put("success", false);
            result.put("msg", "比较版本失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getVersionHistory(Long fileId, int page, int size) {
        List<Map<String, Object>> history = new ArrayList<>();
        try {
            List<ResourceFile> versions = getFileVersions(fileId.intValue());
            int start = page * size;
            int end = Math.min(start + size, versions.size());
            for (int i = start; i < end; i++) {
                ResourceFile v = versions.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("id", v.getId());
                item.put("version", v.getVersion());
                item.put("fileName", v.getFileName());
                item.put("fileSize", v.getFileSize());
                item.put("createdAt", v.getCreatedAt());
                history.add(item);
            }
        } catch (Exception e) {
            log.error("获取版本历史失败", e);
        }
        return history;
    }

    @Override
    public Map<String, Object> getVersionStatistics(Long fileId) {
        Map<String, Object> statistics = new HashMap<>();
        try {
            List<ResourceFile> versions = getFileVersions(fileId.intValue());
            statistics.put("totalVersions", versions.size());
            statistics.put("latestVersion", versions.isEmpty() ? 0 : versions.get(0).getVersion());
            long totalSize = 0;
            for (ResourceFile v : versions) {
                totalSize += v.getFileSize() != null ? v.getFileSize() : 0;
            }
            statistics.put("totalSize", totalSize);
        } catch (Exception e) {
            log.error("获取版本统计失败", e);
        }
        return statistics;
    }

    @Override
    public boolean updateVersionNote(Long id, String note) {
        try {
            ResourceFile file = getById(id.intValue());
            if (file == null) {
                return false;
            }
            file.setDescription(note);
            file.setUpdatedAt(LocalDateTime.now());
            return updateById(file);
        } catch (Exception e) {
            log.error("更新版本备注失败", e);
            return false;
        }
    }

    @Override
    public int batchDeleteVersions(List<Long> ids) {
        int count = 0;
        try {
            for (Long id : ids) {
                if (deleteFile(id.intValue())) {
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("批量删除版本失败", e);
        }
        return count;
    }

    // 以下是Controller中使用的方法实现

    @Override
    public ResourceFile uploadResourceFile(MultipartFile file, String category, String description) {
        log.info("上传资源文件: category={}, description={}", category, description);
        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setFileName(file.getOriginalFilename());
        resourceFile.setFileSize(file.getSize());
        resourceFile.setCategory(category);
        resourceFile.setDescription(description);
        resourceFile.setStatus(1);
        resourceFile.setCreatedAt(LocalDateTime.now());
        resourceFile.setUpdatedAt(LocalDateTime.now());
        save(resourceFile);
        return resourceFile;
    }

    @Override
    public List<ResourceFile> batchUploadResourceFiles(MultipartFile[] files, String category) {
        log.info("批量上传资源文件: category={}, count={}", category, files.length);
        List<ResourceFile> resourceFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            ResourceFile resourceFile = uploadResourceFile(file, category, null);
            resourceFiles.add(resourceFile);
        }
        return resourceFiles;
    }

    @Override
    public String downloadResourceFile(Long id) {
        log.info("下载资源文件: id={}", id);
        ResourceFile file = getById(id.intValue());
        if (file == null) {
            return null;
        }
        return "/download/" + file.getFileName();
    }

    @Override
    public ResourceFile updateResourceFile(Long id, ResourceFile file) {
        log.info("更新资源文件: id={}", id);
        ResourceFile existingFile = getById(id.intValue());
        if (existingFile == null) {
            return null;
        }
        existingFile.setFileName(file.getFileName());
        existingFile.setDescription(file.getDescription());
        existingFile.setUpdatedAt(LocalDateTime.now());
        updateById(existingFile);
        return existingFile;
    }

    @Override
    public List<ResourceFile> getResourceFileList(String category, int page, int size) {
        log.info("获取资源文件列表: category={}, page={}, size={}", category, page, size);
        return getByCategory(category);
    }

    @Override
    public List<ResourceFile> searchResourceFiles(String keyword, int page, int size) {
        log.info("搜索资源文件: keyword={}, page={}, size={}", keyword, page, size);
        return searchByFileName(keyword);
    }

    @Override
    public String getPreviewUrl(Long id) {
        log.info("获取预览URL: id={}", id);
        return "/preview/" + id;
    }

    @Override
    public Map<String, Object> getFileStatistics() {
        log.info("获取文件统计");
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalFiles", count());
        statistics.put("totalSize", 0L);
        statistics.put("byType", Map.of());
        return statistics;
    }

    @Override
    public int batchDeleteResourceFiles(List<Long> ids) {
        log.info("批量删除资源文件: count={}", ids.size());
        int count = 0;
        for (Long id : ids) {
            if (deleteResourceFile(id)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Map<String, Object> getFileTypeStatistics() {
        log.info("获取文件类型统计");
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("image", 0);
        statistics.put("video", 0);
        statistics.put("document", 0);
        return statistics;
    }

    @Override
    public boolean moveFileToCategory(Long fileId, String newCategory) {
        log.info("移动文件到分类: fileId={}, category={}", fileId, newCategory);
        ResourceFile file = getById(fileId.intValue());
        if (file == null) {
            return false;
        }
        file.setCategory(newCategory);
        file.setUpdatedAt(LocalDateTime.now());
        return updateById(file);
    }
}