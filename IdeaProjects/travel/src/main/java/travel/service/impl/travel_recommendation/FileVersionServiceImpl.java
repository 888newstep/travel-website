package travel.service.impl.travel_recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_recommendation.ResourceFile;
import travel.service.travel_recommendation.FileVersionService;
import travel.service.travel_recommendation.ResourceFileService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileVersionServiceImpl implements FileVersionService {

    private final ResourceFileService resourceFileService;

    @Override
    public List<ResourceFile> getFileVersions(Integer fileId) {
        return resourceFileService.getFileVersions(fileId);
    }

    @Override
    public ResourceFile getFileVersion(Integer fileId, Integer version) {
        List<ResourceFile> versions = getFileVersions(fileId);
        for (ResourceFile versionFile : versions) {
            if (versionFile.getVersion().equals(version)) {
                return versionFile;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> createNewVersion(Integer fileId, String fileName, String description) {
        Map<String, Object> result = new HashMap<>();
        try {
            ResourceFile originalFile = resourceFileService.getFileById(fileId);
            if (originalFile == null) {
                result.put("success", false);
                result.put("msg", "原文件不存在");
                return result;
            }

            // 创建新版本文件
            ResourceFile newVersion = new ResourceFile();
            newVersion.setFileName(fileName != null ? fileName : originalFile.getFileName());
            newVersion.setFilePath(originalFile.getFilePath());
            newVersion.setFileSize(originalFile.getFileSize());
            newVersion.setFileType(originalFile.getFileType());
            newVersion.setUploadTime(LocalDateTime.now());
            newVersion.setUploadUserId(originalFile.getUploadUserId());
            newVersion.setDescription(description != null ? description : originalFile.getDescription());
            newVersion.setStatus(originalFile.getStatus());
            newVersion.setRouteId(originalFile.getRouteId());
            newVersion.setTags(originalFile.getTags());
            newVersion.setPreviewUrl(originalFile.getPreviewUrl());
            newVersion.setDownloadCount(0);
            newVersion.setCommentCount(0);
            newVersion.setRating(0.0);
            newVersion.setLastAccessTime(LocalDateTime.now());
            newVersion.setParentFileId(originalFile.getParentFileId() != null ? originalFile.getParentFileId() : fileId);
            newVersion.setFileCategory(originalFile.getFileCategory());
            newVersion.setViewCount(0);
            newVersion.setVersion(originalFile.getVersion() + 1);

            boolean saved = resourceFileService.save(newVersion);
            if (saved) {
                // 更新原文件的版本号
                originalFile.setVersion(originalFile.getVersion() + 1);
                resourceFileService.updateById(originalFile);

                result.put("success", true);
                result.put("versionId", newVersion.getId());
                result.put("version", newVersion.getVersion());
                result.put("fileName", newVersion.getFileName());
            } else {
                result.put("success", false);
                result.put("msg", "创建新版本失败");
            }
        } catch (Exception e) {
            log.error("创建文件新版本失败", e);
            result.put("success", false);
            result.put("msg", "创建新版本失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public boolean revertToVersion(Integer fileId, Integer version) {
        return resourceFileService.revertToVersion(fileId, version);
    }

    @Override
    public boolean deleteVersion(Integer fileId, Integer version) {
        try {
            ResourceFile versionFile = getFileVersion(fileId, version);
            if (versionFile == null) {
                return false;
            }
            return resourceFileService.removeById(versionFile.getId());
        } catch (Exception e) {
            log.error("删除文件版本失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getVersionDiff(Integer fileId, Integer oldVersion, Integer newVersion) {
        Map<String, Object> result = new HashMap<>();
        try {
            ResourceFile oldVersionFile = getFileVersion(fileId, oldVersion);
            ResourceFile newVersionFile = getFileVersion(fileId, newVersion);

            if (oldVersionFile == null || newVersionFile == null) {
                result.put("success", false);
                result.put("msg", "版本不存在");
                return result;
            }

            Map<String, Object> diff = new HashMap<>();
            diff.put("oldVersion", oldVersion);
            diff.put("newVersion", newVersion);
            diff.put("fileNameChanged", !oldVersionFile.getFileName().equals(newVersionFile.getFileName()));
            diff.put("descriptionChanged", !oldVersionFile.getDescription().equals(newVersionFile.getDescription()));
            diff.put("fileSizeChanged", !oldVersionFile.getFileSize().equals(newVersionFile.getFileSize()));

            result.put("success", true);
            result.put("diff", diff);
        } catch (Exception e) {
            log.error("获取版本差异失败", e);
            result.put("success", false);
            result.put("msg", "获取版本差异失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> batchOperateVersions(List<Integer> fileIds, String operation, Integer version) {
        Map<String, Object> result = new HashMap<>();
        try {
            int successCount = 0;
            int failCount = 0;

            for (Integer fileId : fileIds) {
                boolean success = false;
                switch (operation) {
                    case "revert":
                        success = revertToVersion(fileId, version);
                        break;
                    case "delete":
                        success = deleteVersion(fileId, version);
                        break;
                    default:
                        failCount++;
                        continue;
                }
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            result.put("success", true);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("msg", "批量操作完成");
        } catch (Exception e) {
            log.error("批量操作版本失败", e);
            result.put("success", false);
            result.put("msg", "批量操作失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getVersionStatistics(Integer fileId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ResourceFile> versions = getFileVersions(fileId);
            result.put("totalVersions", versions.size());
            result.put("latestVersion", versions.isEmpty() ? 0 : versions.get(versions.size() - 1).getVersion());
            result.put("versions", versions);
            result.put("success", true);
        } catch (Exception e) {
            log.error("获取版本统计信息失败", e);
            result.put("success", false);
            result.put("msg", "获取统计信息失败: " + e.getMessage());
        }
        return result;
    }
}
