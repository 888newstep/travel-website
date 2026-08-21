package travel.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.FileComment;
import travel.common.entity.travel_recommendation.FileTag;
import travel.common.entity.travel_recommendation.ResourceFile;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.user_community_mapper.FileCommentMapper;
import travel.common.mapper.user_community_mapper.FileTagMapper;
import travel.common.mapper.travel_recommendation_mapper.ResourceFileMapper;
import travel.common.security.AuthenticatedUserSupport;
import travel.file.service.ResourceFileService;
import travel.file.storage.FileStoragePolicy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
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
    private final FileStoragePolicy fileStoragePolicy;

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, Integer userId, String description) {
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        Map<String, Object> result = new HashMap<>();
        Path storedPath = null;
        boolean saved = false;
        try {
            FileStoragePolicy.StoredFile storedFile = fileStoragePolicy.store(file);
            storedPath = storedFile.path();

            ResourceFile resourceFile = new ResourceFile();
            resourceFile.setFileId(UUID.randomUUID().toString());
            resourceFile.setFileName(storedFile.originalFilename());
            resourceFile.setFilePath(fileStoragePolicy.toPublicPath(storedFile.storedFilename()));
            resourceFile.setFileSize(storedFile.size());
            resourceFile.setFileType(storedFile.fileType());
            resourceFile.setUploadTime(LocalDateTime.now());
            resourceFile.setUploadUserId(currentUserId);
            resourceFile.setDescription(description);
            resourceFile.setStatus(1);
            resourceFile.setDownloadCount(0);
            resourceFile.setCommentCount(0);
            resourceFile.setRating(0.0);
            resourceFile.setVersion(1);
            resourceFile.setCreatedAt(LocalDateTime.now());
            resourceFile.setUpdatedAt(LocalDateTime.now());

            saved = save(resourceFile);
            if (saved) {
                result.put("success", true);
                result.put("fileId", resourceFile.getId());
                result.put("fileName", storedFile.originalFilename());
                result.put("filePath", resourceFile.getFilePath());
            } else {
                result.put("success", false);
                result.put("msg", "文件上传失败，数据库保存失败");
            }
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("msg", e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            result.put("success", false);
            result.put("msg", "文件上传失败");
        } finally {
            if (!saved) {
                fileStoragePolicy.deleteQuietly(storedPath);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> batchUploadFiles(List<MultipartFile> files, Integer userId, String description) {
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        fileStoragePolicy.validateBatchSize(files == null ? 0 : files.size());
        files.forEach(fileStoragePolicy::validate);
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadFile(file, currentUserId, description));
        }
        return results;
    }

    @Override
    public List<ResourceFile> getByUserId(Integer userId) {
        return resourceFileMapper.selectByUserId(AuthenticatedUserSupport.requireIntegerUserId());
    }

    @Override
    public List<ResourceFile> getByFileType(String fileType) {
        return list(ownedFilesQuery().eq(ResourceFile::getFileType, fileType));
    }

    @Override
    public List<ResourceFile> getByStatus(Integer status) {
        return list(ownedFilesQuery().eq(ResourceFile::getStatus, status));
    }

    @Override
    public List<ResourceFile> getByRouteId(Integer routeId) {
        return list(ownedFilesQuery().eq(ResourceFile::getRouteId, routeId));
    }

    @Override
    public List<ResourceFile> getByTags(String tags) {
        return list(ownedFilesQuery().like(ResourceFile::getTags, tags));
    }

    @Override
    public List<ResourceFile> searchByFileName(String fileName) {
        LambdaQueryWrapper<ResourceFile> queryWrapper = ownedFilesQuery();
        queryWrapper.like(fileName != null && !fileName.isBlank(), ResourceFile::getFileName, fileName)
                .orderByDesc(ResourceFile::getUpdatedAt)
                .orderByDesc(ResourceFile::getId);
        return list(queryWrapper);
    }

    @Override
    public List<ResourceFile> searchByMultipleConditions(String fileName, String fileType, String tags, Integer userId, Integer routeId) {
        LambdaQueryWrapper<ResourceFile> queryWrapper = ownedFilesQuery()
                .like(fileName != null && !fileName.isBlank(), ResourceFile::getFileName, fileName)
                .eq(fileType != null && !fileType.isBlank(), ResourceFile::getFileType, fileType)
                .like(tags != null && !tags.isBlank(), ResourceFile::getTags, tags)
                .eq(routeId != null, ResourceFile::getRouteId, routeId)
                .orderByDesc(ResourceFile::getUpdatedAt)
                .orderByDesc(ResourceFile::getId);
        return list(queryWrapper);
    }

    @Override
    public List<ResourceFile> getHotFiles(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return list(ownedFilesQuery()
                .orderByDesc(ResourceFile::getDownloadCount)
                .orderByDesc(ResourceFile::getViewCount)
                .last("LIMIT " + safeLimit));
    }

    @Override
    public List<ResourceFile> getNewFiles(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return list(ownedFilesQuery()
                .orderByDesc(ResourceFile::getCreatedAt)
                .last("LIMIT " + safeLimit));
    }

    @Override
    public boolean deleteFile(Integer fileId) {
        ResourceFile resourceFile = requireOwnedFile(fileId);
        try {
            Path storedPath = fileStoragePolicy.resolveStoredPath(
                    resourceFile.getFilePath(), resourceFile.getFileName());
            boolean removed = removeById(fileId);
            if (removed) {
                fileStoragePolicy.deleteQuietly(storedPath);
            }
            return removed;
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return false;
        }
    }

    @Override
    public boolean batchDeleteFiles(List<Integer> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        fileIds.forEach(this::requireOwnedFile);
        try {
            boolean allDeleted = true;
            for (Integer fileId : fileIds) {
                allDeleted &= deleteFile(fileId);
            }
            return allDeleted;
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            return false;
        }
    }

    @Override
    public ResourceFile getFileById(Integer fileId) {
        return requireOwnedFile(fileId);
    }

    @Override
    public boolean associateWithRoute(Integer fileId, Integer routeId) {
        ResourceFile resourceFile = requireOwnedFile(fileId);
        try {
            resourceFile.setRouteId(routeId);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("关联路线失败", e);
            return false;
        }
    }

    @Override
    public boolean dissociateFromRoute(Integer fileId) {
        ResourceFile resourceFile = requireOwnedFile(fileId);
        try {
            resourceFile.setRouteId(null);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("解除路线关联失败", e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getFileTags(Integer fileId) {
        requireOwnedFile(fileId);
        List<Map<String, Object>> tags = new ArrayList<>();
        try {
            List<FileTag> fileTags = findTagsByFileId(fileId);
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
        requireOwnedFile(fileId);
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        try {
            FileTag fileTag = new FileTag();
            fileTag.setTagName(tagName);
            fileTag.setTagType("general");
            fileTag.setFileId(fileId);
            fileTag.setUserId(currentUserId);
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
        requireOwnedFile(fileId);
        try {
            List<FileTag> tags = findTagsByFileId(fileId);
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
        requireOwnedFile(fileId);
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
        requireOwnedFile(fileId);
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        try {
            FileComment comment = new FileComment();
            comment.setFileId(fileId);
            comment.setUserId(currentUserId);
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
                    resourceFile.setCommentCount((resourceFile.getCommentCount() != null ? resourceFile.getCommentCount() : 0) + 1);
                    Double avgRating = fileCommentMapper.selectAverageRatingByFileId(fileId);
                    resourceFile.setRating(avgRating != null ? avgRating : 0.0);
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
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        FileComment comment = fileCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCodeEnum.FILE_NOT_EXIST);
        }
        requireOwnedFile(comment.getFileId());
        try {
            return fileCommentMapper.incrementLikes(commentId) > 0;
        } catch (Exception e) {
            log.error("点赞评论失败", e);
            return false;
        }
    }

    @Override
    public Double getFileAverageRating(Integer fileId) {
        requireOwnedFile(fileId);
        try {
            return fileCommentMapper.selectAverageRatingByFileId(fileId);
        } catch (Exception e) {
            log.error("获取文件平均评分失败", e);
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getFileStatistics(Integer fileId) {
        ResourceFile resourceFile = requireOwnedFile(fileId);
        Map<String, Object> statistics = new HashMap<>();
        try {
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
        ResourceFile resourceFile = requireOwnedFile(fileId);
        try {
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
        requireOwnedFile(fileId);
        try {
            return resourceFileMapper.incrementDownloadCount(fileId) > 0;
        } catch (Exception e) {
            log.error("增加下载次数失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> generateFileShareUrl(Integer fileId, Integer expireHours) {
        requireOwnedFile(fileId);
        int effectiveExpireHours = expireHours == null ? 24 : expireHours;
        if (effectiveExpireHours <= 0 || effectiveExpireHours > 720) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
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
            LocalDateTime expireTime = LocalDateTime.now().plusHours(effectiveExpireHours);

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
            ResourceFile file = lambdaQuery()
                    .eq(ResourceFile::getShareUrl, shareUrl)
                    .one();
            if (file != null && file.getShareExpireTime() != null
                    && file.getShareExpireTime().isAfter(LocalDateTime.now())) {
                return file;
            }
        } catch (Exception e) {
            log.error("通过分享链接获取文件失败", e);
        }
        return null;
    }

    @Override
    public boolean cancelFileShare(Integer fileId) {
        requireOwnedFile(fileId);
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
        requireOwnedFile(fileId);
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
        ResourceFile ownedFile = requireOwnedFile(fileId);
        try {
            Integer parentId = ownedFile.getParentFileId() != null ? ownedFile.getParentFileId() : fileId;
            return list(ownedFilesQuery()
                    .and(query -> query.eq(ResourceFile::getId, parentId)
                            .or()
                            .eq(ResourceFile::getParentFileId, parentId))
                    .orderByDesc(ResourceFile::getVersion));
        } catch (Exception e) {
            log.error("获取文件版本失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean revertToVersion(Integer fileId, Integer version) {
        ResourceFile currentFile = requireOwnedFile(fileId);
        try {
            List<ResourceFile> versions = getFileVersions(fileId);
            for (ResourceFile versionFile : versions) {
                if (versionFile.getVersion().equals(version)) {
                    currentFile.setFileName(versionFile.getFileName());
                    currentFile.setFilePath(versionFile.getFilePath());
                    currentFile.setFileSize(versionFile.getFileSize());
                    currentFile.setFileType(versionFile.getFileType());
                    currentFile.setDescription(versionFile.getDescription());
                    currentFile.setVersion((currentFile.getVersion() == null ? 0 : currentFile.getVersion()) + 1);
                    currentFile.setUpdatedAt(LocalDateTime.now());
                    return updateById(currentFile);
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
        ResourceFile resourceFile = requireOwnedFile(fileId);
        try {
            resourceFile.setViewCount((resourceFile.getViewCount() != null ? resourceFile.getViewCount() : 0) + 1);
            return updateById(resourceFile);
        } catch (Exception e) {
            log.error("增加文件浏览次数失败", e);
            return false;
        }
    }

    @Override
    public List<ResourceFile> getByCategory(String category) {
        try {
            return list(ownedFilesQuery()
                    .eq(category != null && !category.isBlank(), ResourceFile::getFileCategory, category)
                    .orderByDesc(ResourceFile::getUpdatedAt));
        } catch (Exception e) {
            log.error("按分类获取文件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ResourceFile> getByParentFileId(Integer parentFileId) {
        requireOwnedFile(parentFileId);
        try {
            return list(ownedFilesQuery().eq(ResourceFile::getParentFileId, parentFileId));
        } catch (Exception e) {
            log.error("按父文件ID获取文件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ResourceFile createResourceFile(ResourceFile version) {
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        if (version == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (version.getParentFileId() != null) {
            requireOwnedFile(version.getParentFileId());
        }
        try {
            version.setId(null);
            version.setFileId(UUID.randomUUID().toString());
            version.setUploadUserId(currentUserId);
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
        return requireOwnedFile(id);
    }

    @Override
    public List<ResourceFile> getResourceFiles(Long fileId) {
        return getFileVersions(fileId.intValue());
    }

    @Override
    public ResourceFile getLatestVersion(Long fileId) {
        requireOwnedFile(fileId);
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
        return deleteFile(requireOwnedFile(id).getId());
    }

    @Override
    public boolean restoreToVersion(Long fileId, Long versionId) {
        ResourceFile currentFile = requireOwnedFile(fileId);
        ResourceFile versionFile = requireOwnedFile(versionId);
        requireSameFileFamily(currentFile, versionFile);
        currentFile.setFileName(versionFile.getFileName());
        currentFile.setFilePath(versionFile.getFilePath());
        currentFile.setFileSize(versionFile.getFileSize());
        currentFile.setFileType(versionFile.getFileType());
        currentFile.setDescription(versionFile.getDescription());
        currentFile.setVersion((currentFile.getVersion() == null ? 0 : currentFile.getVersion()) + 1);
        currentFile.setUpdatedAt(LocalDateTime.now());
        return updateById(currentFile);
    }

    @Override
    public Map<String, Object> compareVersions(Long version1Id, Long version2Id) {
        ResourceFile ownedVersion1 = requireOwnedFile(version1Id);
        ResourceFile ownedVersion2 = requireOwnedFile(version2Id);
        requireSameFileFamily(ownedVersion1, ownedVersion2);
        Map<String, Object> result = new HashMap<>();
        try {
            ResourceFile v1 = ownedVersion1;
            ResourceFile v2 = ownedVersion2;
            if (v1 == null || v2 == null) {
                result.put("success", false);
                result.put("msg", "版本不存在");
                return result;
            }
            result.put("success", true);
            result.put("version1", v1);
            result.put("version2", v2);
            result.put("fileNameDiff", !java.util.Objects.equals(v1.getFileName(), v2.getFileName()));
            result.put("fileSizeDiff", !java.util.Objects.equals(v1.getFileSize(), v2.getFileSize()));
            result.put("descriptionDiff", !java.util.Objects.equals(v1.getDescription(), v2.getDescription()));
        } catch (Exception e) {
            log.error("比较版本失败", e);
            result.put("success", false);
            result.put("msg", "比较版本失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getVersionHistory(Long fileId, int page, int size) {
        requireOwnedFile(fileId);
        validatePageBounds(page, size);
        List<Map<String, Object>> history = new ArrayList<>();
        try {
            List<ResourceFile> versions = getFileVersions(fileId.intValue());
            int start = Math.min(Math.multiplyExact(page, size), versions.size());
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
        requireOwnedFile(fileId);
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
        ResourceFile ownedFile = requireOwnedFile(id);
        if (note != null && note.length() > 5000) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        try {
            ResourceFile file = ownedFile;
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
        validateIds(ids);
        ids.forEach(this::requireOwnedFile);
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
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("上传资源文件: category={}, description={}", category, description);
        Path storedPath = null;
        boolean saved = false;
        try {
            FileStoragePolicy.StoredFile storedFile = fileStoragePolicy.store(file);
            storedPath = storedFile.path();

            ResourceFile resourceFile = new ResourceFile();
            resourceFile.setFileId(UUID.randomUUID().toString());
            resourceFile.setFileName(storedFile.originalFilename());
            resourceFile.setFilePath(fileStoragePolicy.toPublicPath(storedFile.storedFilename()));
            resourceFile.setFileSize(storedFile.size());
            resourceFile.setFileType(storedFile.fileType());
            resourceFile.setUploadTime(LocalDateTime.now());
            resourceFile.setUploadUserId(currentUserId);
            resourceFile.setCategory(category);
            resourceFile.setDescription(description);
            resourceFile.setStatus(1);
            resourceFile.setDownloadCount(0);
            resourceFile.setCommentCount(0);
            resourceFile.setRating(0.0);
            resourceFile.setVersion(1);
            resourceFile.setViewCount(0);
            resourceFile.setCreatedAt(LocalDateTime.now());
            resourceFile.setUpdatedAt(LocalDateTime.now());

            saved = save(resourceFile);
            if (!saved) {
                log.error("文件上传失败，数据库保存失败");
                return null;
            }
            return resourceFile;
        } catch (IllegalArgumentException e) {
            log.warn("文件上传请求被拒绝: reason={}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("上传资源文件失败", e);
            return null;
        } finally {
            if (!saved) {
                fileStoragePolicy.deleteQuietly(storedPath);
            }
        }
    }

    @Override
    public List<ResourceFile> batchUploadResourceFiles(MultipartFile[] files, String category) {
        fileStoragePolicy.validateBatchSize(files == null ? 0 : files.length);
        for (MultipartFile file : files) {
            fileStoragePolicy.validate(file);
        }
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
        ResourceFile ownedFile = requireOwnedFile(id);
        log.info("下载资源文件: id={}", id);
        if (id == null || id <= 0) {
            return null;
        }
        ResourceFile file = ownedFile;
        if (file == null) {
            return null;
        }
        try {
            Path storedPath = fileStoragePolicy.resolveStoredPath(file.getFilePath(), file.getFileName());
            if (!Files.isRegularFile(storedPath)) {
                log.warn("资源文件不存在: id={}, path={}", id, storedPath);
                return null;
            }
            return storedPath.toString();
        } catch (IllegalArgumentException e) {
            log.warn("资源文件路径非法: id={}, reason={}", id, e.getMessage());
            return null;
        }
    }

    @Override
    public ResourceFile updateResourceFile(Long id, ResourceFile file) {
        if (file == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        log.info("更新资源文件: id={}", id);
        ResourceFile existingFile = requireOwnedFile(id);
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
        Page<ResourceFile> pageRequest = createPage(page, size);
        LambdaQueryWrapper<ResourceFile> queryWrapper = ownedFilesQuery()
                .eq(category != null && !category.isBlank(), ResourceFile::getFileCategory, category)
                .orderByDesc(ResourceFile::getUpdatedAt)
                .orderByDesc(ResourceFile::getId);
        return page(pageRequest, queryWrapper).getRecords();
    }

    @Override
    public List<ResourceFile> searchResourceFiles(String keyword, int page, int size) {
        log.info("搜索资源文件: keyword={}, page={}, size={}", keyword, page, size);
        Page<ResourceFile> pageRequest = createPage(page, size);
        LambdaQueryWrapper<ResourceFile> queryWrapper = ownedFilesQuery()
                .like(keyword != null && !keyword.isBlank(), ResourceFile::getFileName, keyword)
                .orderByDesc(ResourceFile::getUpdatedAt)
                .orderByDesc(ResourceFile::getId);
        return page(pageRequest, queryWrapper).getRecords();
    }

    private List<FileTag> findTagsByFileId(Integer fileId) {
        LambdaQueryWrapper<FileTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileTag::getFileId, fileId)
                .orderByDesc(FileTag::getUsageCount)
                .orderByAsc(FileTag::getTagName);
        return fileTagMapper.selectList(queryWrapper);
    }

    @Override
    public String getPreviewUrl(Long id) {
        requireOwnedFile(id);
        log.info("获取预览URL: id={}", id);
        return "/preview/" + id;
    }

    @Override
    public Map<String, Object> getFileStatistics() {
        List<ResourceFile> ownedFiles = list(ownedFilesQuery());
        Map<String, Long> byType = new HashMap<>();
        long totalSize = 0L;
        for (ResourceFile file : ownedFiles) {
            totalSize += file.getFileSize() == null ? 0L : file.getFileSize();
            String fileType = file.getFileType() == null || file.getFileType().isBlank()
                    ? "unknown" : file.getFileType();
            byType.merge(fileType, 1L, Long::sum);
        }
        log.info("获取文件统计");
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalFiles", ownedFiles.size());
        statistics.put("totalSize", totalSize);
        statistics.put("byType", byType);
        return statistics;
    }

    @Override
    public int batchDeleteResourceFiles(List<Long> ids) {
        validateIds(ids);
        ids.forEach(this::requireOwnedFile);
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
        statistics.put("image", countOwnedFilesByExtensions(List.of("jpg", "jpeg", "png", "gif", "webp")));
        statistics.put("video", countOwnedFilesByExtensions(List.of("mp4", "avi", "mov", "mkv", "webm")));
        statistics.put("document", countOwnedFilesByExtensions(
                List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md")));
        return statistics;
    }

    @Override
    public boolean moveFileToCategory(Long fileId, String newCategory) {
        log.info("移动文件到分类: fileId={}, category={}", fileId, newCategory);
        if (newCategory == null || newCategory.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        ResourceFile file = requireOwnedFile(fileId);
        file.setCategory(newCategory);
        file.setUpdatedAt(LocalDateTime.now());
        return updateById(file);
    }

    private LambdaQueryWrapper<ResourceFile> ownedFilesQuery() {
        return new LambdaQueryWrapper<ResourceFile>()
                .eq(ResourceFile::getUploadUserId, AuthenticatedUserSupport.requireIntegerUserId());
    }

    private ResourceFile requireOwnedFile(Integer fileId) {
        if (fileId == null || fileId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        ResourceFile file = getById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCodeEnum.FILE_NOT_EXIST);
        }
        Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
        if (!currentUserId.equals(file.getUploadUserId())) {
            throw new BusinessException(ErrorCodeEnum.FILE_PERMISSION_ERROR);
        }
        return file;
    }

    private ResourceFile requireOwnedFile(Long fileId) {
        if (fileId == null || fileId <= 0 || fileId > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return requireOwnedFile(fileId.intValue());
    }

    private void requireSameFileFamily(ResourceFile first, ResourceFile second) {
        Integer firstRootId = first.getParentFileId() == null ? first.getId() : first.getParentFileId();
        Integer secondRootId = second.getParentFileId() == null ? second.getId() : second.getParentFileId();
        if (!firstRootId.equals(secondRootId)) {
            throw new BusinessException(ErrorCodeEnum.FILE_PERMISSION_ERROR);
        }
    }

    private Page<ResourceFile> createPage(int page, int size) {
        validatePageBounds(page, size);
        return new Page<>((long) page + 1, size);
    }

    private void validatePageBounds(int page, int size) {
        if (page < 0 || page > 1_000_000 || size <= 0 || size > 100
                || page > Integer.MAX_VALUE / size) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0 || limit > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return limit;
    }

    private void validateIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100
                || ids.stream().anyMatch(id -> id == null || id <= 0 || id > Integer.MAX_VALUE)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private long countOwnedFilesByExtensions(List<String> extensions) {
        return count(ownedFilesQuery().in(ResourceFile::getFileType, extensions));
    }
}
