package travel.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import travel.common.entity.travel_recommendation.ResourceFile;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_recommendation_mapper.ResourceFileMapper;
import travel.common.security.AuthenticatedUserSupport;
import travel.file.service.ResourceFileService;
import travel.file.storage.FileStoragePolicy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceFileServiceImpl extends ServiceImpl<ResourceFileMapper, ResourceFile>
        implements ResourceFileService {

    private final FileStoragePolicy fileStoragePolicy;

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
            resourceFiles.add(uploadResourceFile(file, category, null));
        }
        return resourceFiles;
    }

    @Override
    public ResourceFile getResourceFile(Long id) {
        return requireOwnedFile(id);
    }

    @Override
    public String downloadResourceFile(Long id) {
        ResourceFile file = requireOwnedFile(id);
        log.info("下载资源文件: id={}", id);
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
    public boolean deleteResourceFile(Long id) {
        return deleteOwnedFile(requireOwnedFile(id));
    }

    @Override
    public ResourceFile updateResourceFile(Long id, ResourceFile file) {
        if (file == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        log.info("更新资源文件: id={}", id);
        ResourceFile existingFile = requireOwnedFile(id);
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
        LambdaQueryWrapper<ResourceFile> query = ownedFilesQuery()
                .eq(category != null && !category.isBlank(), ResourceFile::getFileCategory, category)
                .orderByDesc(ResourceFile::getUpdatedAt)
                .orderByDesc(ResourceFile::getId);
        return page(pageRequest, query).getRecords();
    }

    @Override
    public List<ResourceFile> searchResourceFiles(String keyword, int page, int size) {
        log.info("搜索资源文件: keyword={}, page={}, size={}", keyword, page, size);
        Page<ResourceFile> pageRequest = createPage(page, size);
        LambdaQueryWrapper<ResourceFile> query = ownedFilesQuery()
                .like(keyword != null && !keyword.isBlank(), ResourceFile::getFileName, keyword)
                .orderByDesc(ResourceFile::getUpdatedAt)
                .orderByDesc(ResourceFile::getId);
        return page(pageRequest, query).getRecords();
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
        List<ResourceFile> ownedFiles = ids.stream().map(this::requireOwnedFile).toList();
        log.info("批量删除资源文件: count={}", ownedFiles.size());
        int count = 0;
        for (ResourceFile file : ownedFiles) {
            if (deleteOwnedFile(file)) {
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

    @Override
    public List<ResourceFile> getResourceFiles(Long fileId) {
        return loadFileVersions(fileId);
    }

    @Override
    public ResourceFile getLatestVersion(Long fileId) {
        List<ResourceFile> versions = loadFileVersions(fileId);
        return versions.isEmpty() ? null : versions.get(0);
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
        ResourceFile first = requireOwnedFile(version1Id);
        ResourceFile second = requireOwnedFile(version2Id);
        requireSameFileFamily(first, second);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("version1", first);
        result.put("version2", second);
        result.put("fileNameDiff", !Objects.equals(first.getFileName(), second.getFileName()));
        result.put("fileSizeDiff", !Objects.equals(first.getFileSize(), second.getFileSize()));
        result.put("descriptionDiff", !Objects.equals(first.getDescription(), second.getDescription()));
        return result;
    }

    @Override
    public List<Map<String, Object>> getVersionHistory(Long fileId, int page, int size) {
        validatePageBounds(page, size);
        List<ResourceFile> versions = loadFileVersions(fileId);
        int start = Math.min(page * size, versions.size());
        int end = Math.min(start + size, versions.size());
        List<Map<String, Object>> history = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            ResourceFile version = versions.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", version.getId());
            item.put("version", version.getVersion());
            item.put("fileName", version.getFileName());
            item.put("fileSize", version.getFileSize());
            item.put("createdAt", version.getCreatedAt());
            history.add(item);
        }
        return history;
    }

    private List<ResourceFile> loadFileVersions(Long fileId) {
        ResourceFile ownedFile = requireOwnedFile(fileId);
        Integer rootId = ownedFile.getParentFileId() == null ? ownedFile.getId() : ownedFile.getParentFileId();
        return list(ownedFilesQuery()
                .and(query -> query.eq(ResourceFile::getId, rootId)
                        .or()
                        .eq(ResourceFile::getParentFileId, rootId))
                .orderByDesc(ResourceFile::getVersion));
    }

    private boolean deleteOwnedFile(ResourceFile file) {
        try {
            Path storedPath = fileStoragePolicy.resolveStoredPath(file.getFilePath(), file.getFileName());
            boolean removed = removeById(file.getId());
            if (removed) {
                fileStoragePolicy.deleteQuietly(storedPath);
            }
            return removed;
        } catch (Exception e) {
            log.error("文件删除失败: id={}", file.getId(), e);
            return false;
        }
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
