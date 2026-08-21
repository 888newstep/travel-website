package travel.file.controller;

import travel.common.exception.ExceptionPropagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.travel_recommendation.FileTag;
import travel.common.entity.travel_recommendation.ResourceFile;
import travel.file.service.FileTagService;
import travel.file.service.ResourceFileService;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 资源文件控制器
 * 处理资源文件的上传、下载和管理
 */
@Slf4j
@RestController
@RequestMapping("/resource-file")
@RequiredArgsConstructor
public class ResourceFileController {

    private final ResourceFileService resourceFileService;
    private final FileTagService fileTagService;

    /**
     * 上传资源文件
     * POST /api/resource-file/upload
     */
    @PostMapping("/upload")
    public Result<ResourceFile> uploadResourceFile(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String description) {
        try {
            log.info("上传资源文件请求: filename={}", file == null ? null : file.getOriginalFilename());
            ResourceFile result = resourceFileService.uploadResourceFile(file, category, description);
            if (result == null) {
                return Result.error("上传失败：文件参数不合法或存储失败");
            }
            return Result.success("上传成功", result);
        } catch (Exception e) {
            log.error("上传资源文件失败", e);
            return Result.error("上传失败");
        }
    }

    /**
     * 批量上传资源文件
     * POST /api/resource-file/batch-upload
     */
    @PostMapping("/batch-upload")
    public Result<List<ResourceFile>> batchUploadResourceFiles(@RequestParam("files") MultipartFile[] files,
                                                                 @RequestParam(required = false) String category) {
        try {
            log.info("批量上传资源文件请求: count={}", files == null ? 0 : files.length);
            List<ResourceFile> result = resourceFileService.batchUploadResourceFiles(files, category);
            if (result == null || result.stream().anyMatch(file -> file == null)) {
                return Result.error("批量上传失败：文件参数不合法或存储失败");
            }
            return Result.success("批量上传成功", result);
        } catch (Exception e) {
            log.error("批量上传资源文件失败", e);
            return Result.error("批量上传失败");
        }
    }

    /**
     * 获取资源文件详情
     * GET /api/resource-file/{id}
     */
    @GetMapping("/{id}")
    public Result<ResourceFile> getResourceFile(@PathVariable Long id) {
        try {
            log.info("获取资源文件详情请求: id={}", id);
            ResourceFile file = resourceFileService.getResourceFile(id);
            return Result.success("获取详情成功", file);
        } catch (Exception e) {
            log.error("获取资源文件详情失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 下载资源文件
     * GET /api/resource-file/download/{id}
     */
    @GetMapping("/download/{id}")
    public Result<String> downloadResourceFile(@PathVariable Long id) {
        try {
            log.info("下载资源文件请求: id={}", id);
            String downloadUrl = resourceFileService.downloadResourceFile(id);
            if (downloadUrl == null) {
                return Result.error("文件不存在或已失效");
            }
            return Result.success("获取下载链接成功", downloadUrl);
        } catch (Exception e) {
            log.error("下载资源文件失败: id={}", id, e);
            return Result.error("获取下载链接失败");
        }
    }

    /**
     * 删除资源文件
     * DELETE /api/resource-file/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteResourceFile(@PathVariable Long id) {
        try {
            log.info("删除资源文件请求: id={}", id);
            boolean result = resourceFileService.deleteResourceFile(id);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除资源文件失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 更新资源文件信息
     * PUT /api/resource-file/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<ResourceFile> updateResourceFile(@PathVariable Long id, @RequestBody ResourceFile file) {
        try {
            log.info("更新资源文件信息请求: id={}", id);
            ResourceFile result = resourceFileService.updateResourceFile(id, file);
            return Result.success("更新成功", result);
        } catch (Exception e) {
            log.error("更新资源文件信息失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 获取资源文件列表
     * GET /api/resource-file/list
     */
    @GetMapping("/list")
    public Result<List<ResourceFile>> getResourceFileList(@RequestParam(required = false) String category,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取资源文件列表请求: category={}, page={}, size={}", category, page, size);
            List<ResourceFile> files = resourceFileService.getResourceFileList(category, page, size);
            return Result.success("获取列表成功", files);
        } catch (Exception e) {
            log.error("获取资源文件列表失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 搜索资源文件
     * GET /api/resource-file/search
     */
    @GetMapping("/search")
    public Result<List<ResourceFile>> searchResourceFiles(@RequestParam String keyword,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("搜索资源文件请求: keyword={}", keyword);
            List<ResourceFile> files = resourceFileService.searchResourceFiles(keyword, page, size);
            return Result.success("搜索成功", files);
        } catch (Exception e) {
            log.error("搜索资源文件失败: keyword={}, error={}", keyword, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 获取文件预览链接
     * GET /api/resource-file/preview/{id}
     */
    @GetMapping("/preview/{id}")
    public Result<String> getPreviewUrl(@PathVariable Long id) {
        try {
            log.info("获取文件预览链接请求: id={}", id);
            String previewUrl = resourceFileService.getPreviewUrl(id);
            return Result.success("获取预览链接成功", previewUrl);
        } catch (Exception e) {
            log.error("获取文件预览链接失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 获取文件统计信息
     * GET /api/resource-file/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFileStatistics() {
        try {
            log.info("获取文件统计信息请求");
            Map<String, Object> statistics = resourceFileService.getFileStatistics();
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取文件统计信息失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 批量删除资源文件
     * DELETE /api/resource-file/batch-delete
     */
    @DeleteMapping("/batch-delete")
    public Result<Integer> batchDeleteResourceFiles(@RequestBody List<Long> ids) {
        try {
            log.info("批量删除资源文件请求: count={}", ids.size());
            int count = resourceFileService.batchDeleteResourceFiles(ids);
            return Result.success("批量删除成功", count);
        } catch (Exception e) {
            log.error("批量删除资源文件失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 获取文件类型统计
     * GET /api/resource-file/type-statistics
     */
    @GetMapping("/type-statistics")
    public Result<Map<String, Object>> getFileTypeStatistics() {
        try {
            log.info("获取文件类型统计请求");
            Map<String, Object> statistics = resourceFileService.getFileTypeStatistics();
            return Result.success("获取类型统计成功", statistics);
        } catch (Exception e) {
            log.error("获取文件类型统计失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 移动文件到其他分类
     * POST /api/resource-file/move
     */
    @PostMapping("/move")
    public Result<Boolean> moveFileToCategory(@RequestParam Long fileId, @RequestParam String newCategory) {
        try {
            log.info("移动文件到其他分类请求: fileId={}, newCategory={}", fileId, newCategory);
            boolean result = resourceFileService.moveFileToCategory(fileId, newCategory);
            return Result.success("移动成功", result);
        } catch (Exception e) {
            log.error("移动文件到其他分类失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    // ==================== 文件分类管理 ====================

    @PostMapping("/category/create")
    public Result<FileTag> createFileCategory(@RequestBody FileTag category) {
        try {
            log.info("创建文件分类请求: name={}", category.getTagName());
            FileTag result = fileTagService.createFileTag(category);
            return Result.success("创建分类成功", result);
        } catch (Exception e) {
            log.error("创建文件分类失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @PutMapping("/category/update/{id}")
    public Result<FileTag> updateFileCategory(@PathVariable Long id, @RequestBody FileTag category) {
        try {
            log.info("更新文件分类请求: id={}", id);
            FileTag result = fileTagService.updateFileTag(id, category);
            return Result.success("更新分类成功", result);
        } catch (Exception e) {
            log.error("更新文件分类失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @DeleteMapping("/category/delete/{id}")
    public Result<Boolean> deleteFileCategory(@PathVariable Long id) {
        try {
            log.info("删除文件分类请求: id={}", id);
            boolean result = fileTagService.deleteFileTag(id);
            return Result.success("删除分类成功", result);
        } catch (Exception e) {
            log.error("删除文件分类失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @GetMapping("/category/{id}")
    public Result<FileTag> getFileCategory(@PathVariable Long id) {
        try {
            log.info("获取文件分类详情请求: id={}", id);
            FileTag category = fileTagService.getFileTag(id);
            return Result.success("获取分类详情成功", category);
        } catch (Exception e) {
            log.error("获取文件分类详情失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @GetMapping("/category/list")
    public Result<List<FileTag>> getAllCategories() {
        try {
            log.info("获取所有分类请求");
            List<FileTag> categories = fileTagService.getAllCategories();
            return Result.success("获取分类列表成功", categories);
        } catch (Exception e) {
            log.error("获取所有分类失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @GetMapping("/category/tree")
    public Result<List<Map<String, Object>>> getCategoryTree() {
        try {
            log.info("获取分类树请求");
            List<Map<String, Object>> tree = fileTagService.getCategoryTree();
            return Result.success("获取分类树成功", tree);
        } catch (Exception e) {
            log.error("获取分类树失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    // ==================== 文件版本管理 ====================

    @GetMapping("/version/list/{fileId}")
    public Result<?> getFileVersions(@PathVariable Long fileId,
                                     @RequestParam(defaultValue = "false") boolean latestOnly) {
        try {
            log.info("获取文件版本列表请求: fileId={}, latestOnly={}", fileId, latestOnly);
            if (latestOnly) {
                ResourceFile version = resourceFileService.getLatestVersion(fileId);
                return Result.success("获取最新版本成功", version);
            }
            List<ResourceFile> versions = resourceFileService.getResourceFiles(fileId);
            return Result.success("获取版本列表成功", versions);
        } catch (Exception e) {
            log.error("获取文件版本列表失败: fileId={}, error={}", fileId, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @PostMapping("/version/restore")
    public Result<Boolean> restoreToVersion(@RequestParam Long fileId, @RequestParam Long versionId) {
        try {
            log.info("恢复到指定版本请求: fileId={}, versionId={}", fileId, versionId);
            boolean result = resourceFileService.restoreToVersion(fileId, versionId);
            return Result.success("恢复版本成功", result);
        } catch (Exception e) {
            log.error("恢复到指定版本失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @PostMapping("/version/compare")
    public Result<Map<String, Object>> compareVersions(@RequestParam Long version1Id, @RequestParam Long version2Id) {
        try {
            log.info("比较两个版本请求: version1Id={}, version2Id={}", version1Id, version2Id);
            Map<String, Object> comparison = resourceFileService.compareVersions(version1Id, version2Id);
            return Result.success("比较版本成功", comparison);
        } catch (Exception e) {
            log.error("比较两个版本失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @GetMapping("/version/history/{fileId}")
    public Result<List<Map<String, Object>>> getVersionHistory(@PathVariable Long fileId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取版本历史请求: fileId={}", fileId);
            List<Map<String, Object>> history = resourceFileService.getVersionHistory(fileId, page, size);
            return Result.success("获取版本历史成功", history);
        } catch (Exception e) {
            log.error("获取版本历史失败: fileId={}, error={}", fileId, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }
}
