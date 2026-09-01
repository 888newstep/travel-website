package travel.file.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.travel_recommendation.FileTag;
import travel.common.entity.travel_recommendation.ResourceFile;
import travel.file.service.FileTagService;
import travel.file.service.ResourceFileService;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
        log.info("获取资源文件详情请求: id={}", id);
        ResourceFile file = resourceFileService.getResourceFile(id);
        return Result.success("获取详情成功", file);
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

    /** 返回文件二进制内容，避免向客户端暴露服务器本机路径。 */
    @GetMapping("/content/{id}")
    public ResponseEntity<Resource> downloadResourceContent(@PathVariable Long id) {
        Path path = resourceFileService.resolveDownloadPath(id);
        ResourceFile file = resourceFileService.getResourceFile(id);
        if (!resourceFileService.incrementDownloadCount(id)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.getFileSize() == null ? path.toFile().length() : file.getFileSize())
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(new FileSystemResource(path));
    }

    /**
     * 删除资源文件
     * DELETE /api/resource-file/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteResourceFile(@PathVariable Long id) {
        log.info("删除资源文件请求: id={}", id);
        boolean result = resourceFileService.deleteResourceFile(id);
        return Result.success("删除成功", result);
    }

    /**
     * 更新资源文件信息
     * PUT /api/resource-file/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<ResourceFile> updateResourceFile(@PathVariable Long id, @RequestBody ResourceFile file) {
        log.info("更新资源文件信息请求: id={}", id);
        ResourceFile result = resourceFileService.updateResourceFile(id, file);
        return Result.success("更新成功", result);
    }

    /**
     * 获取资源文件列表
     * GET /api/resource-file/list
     */
    @GetMapping("/list")
    public Result<List<ResourceFile>> getResourceFileList(@RequestParam(required = false) String category,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        log.info("获取资源文件列表请求: category={}, page={}, size={}", category, page, size);
        List<ResourceFile> files = resourceFileService.getResourceFileList(category, page, size);
        return Result.success("获取列表成功", files);
    }

    /**
     * 搜索资源文件
     * GET /api/resource-file/search
     */
    @GetMapping("/search")
    public Result<List<ResourceFile>> searchResourceFiles(@RequestParam String keyword,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        log.info("搜索资源文件请求: keyword={}", keyword);
        List<ResourceFile> files = resourceFileService.searchResourceFiles(keyword, page, size);
        return Result.success("搜索成功", files);
    }

    /**
     * 获取文件预览链接
     * GET /api/resource-file/preview/{id}
     */
    @GetMapping("/preview/{id}")
    public Result<String> getPreviewUrl(@PathVariable Long id) {
        log.info("获取文件预览链接请求: id={}", id);
        String previewUrl = resourceFileService.getPreviewUrl(id);
        return Result.success("获取预览链接成功", previewUrl);
    }

    /**
     * 获取文件统计信息
     * GET /api/resource-file/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFileStatistics() {
        log.info("获取文件统计信息请求");
        Map<String, Object> statistics = resourceFileService.getFileStatistics();
        return Result.success("获取统计信息成功", statistics);
    }

    /**
     * 批量删除资源文件
     * DELETE /api/resource-file/batch-delete
     */
    @DeleteMapping("/batch-delete")
    public Result<Integer> batchDeleteResourceFiles(@RequestBody List<Long> ids) {
        log.info("批量删除资源文件请求: count={}", ids.size());
        int count = resourceFileService.batchDeleteResourceFiles(ids);
        return Result.success("批量删除成功", count);
    }

    /**
     * 获取文件类型统计
     * GET /api/resource-file/type-statistics
     */
    @GetMapping("/type-statistics")
    public Result<Map<String, Object>> getFileTypeStatistics() {
        log.info("获取文件类型统计请求");
        Map<String, Object> statistics = resourceFileService.getFileTypeStatistics();
        return Result.success("获取类型统计成功", statistics);
    }

    /**
     * 移动文件到其他分类
     * POST /api/resource-file/move
     */
    @PostMapping("/move")
    public Result<Boolean> moveFileToCategory(@RequestParam Long fileId, @RequestParam String newCategory) {
        log.info("移动文件到其他分类请求: fileId={}, newCategory={}", fileId, newCategory);
        boolean result = resourceFileService.moveFileToCategory(fileId, newCategory);
        return Result.success("移动成功", result);
    }

    // ==================== 文件分类管理 ====================

    @PostMapping("/category/create")
    public Result<FileTag> createFileCategory(@RequestBody FileTag category) {
        log.info("创建文件分类请求: name={}", category.getTagName());
        FileTag result = fileTagService.createFileTag(category);
        return Result.success("创建分类成功", result);
    }

    @PutMapping("/category/update/{id}")
    public Result<FileTag> updateFileCategory(@PathVariable Long id, @RequestBody FileTag category) {
        log.info("更新文件分类请求: id={}", id);
        FileTag result = fileTagService.updateFileTag(id, category);
        return Result.success("更新分类成功", result);
    }

    @DeleteMapping("/category/delete/{id}")
    public Result<Boolean> deleteFileCategory(@PathVariable Long id) {
        log.info("删除文件分类请求: id={}", id);
        boolean result = fileTagService.deleteFileTag(id);
        return Result.success("删除分类成功", result);
    }

    @GetMapping("/category/{id}")
    public Result<FileTag> getFileCategory(@PathVariable Long id) {
        log.info("获取文件分类详情请求: id={}", id);
        FileTag category = fileTagService.getFileTag(id);
        return Result.success("获取分类详情成功", category);
    }

    @GetMapping("/category/list")
    public Result<List<FileTag>> getAllCategories() {
        log.info("获取所有分类请求");
        List<FileTag> categories = fileTagService.getAllCategories();
        return Result.success("获取分类列表成功", categories);
    }

    // ==================== 文件版本管理 ====================

    @GetMapping("/version/list/{fileId}")
    public Result<?> getFileVersions(@PathVariable Long fileId,
                                     @RequestParam(defaultValue = "false") boolean latestOnly) {
        log.info("获取文件版本列表请求: fileId={}, latestOnly={}", fileId, latestOnly);
        if (latestOnly) {
            ResourceFile version = resourceFileService.getLatestVersion(fileId);
            return Result.success("获取最新版本成功", version);
        }
        List<ResourceFile> versions = resourceFileService.getResourceFiles(fileId);
        return Result.success("获取版本列表成功", versions);
    }

    @PostMapping("/version/restore")
    public Result<Boolean> restoreToVersion(@RequestParam Long fileId, @RequestParam Long versionId) {
        log.info("恢复到指定版本请求: fileId={}, versionId={}", fileId, versionId);
        boolean result = resourceFileService.restoreToVersion(fileId, versionId);
        return Result.success("恢复版本成功", result);
    }

    @PostMapping("/version/compare")
    public Result<Map<String, Object>> compareVersions(@RequestParam Long version1Id, @RequestParam Long version2Id) {
        log.info("比较两个版本请求: version1Id={}, version2Id={}", version1Id, version2Id);
        Map<String, Object> comparison = resourceFileService.compareVersions(version1Id, version2Id);
        return Result.success("比较版本成功", comparison);
    }

    @GetMapping("/version/history/{fileId}")
    public Result<List<Map<String, Object>>> getVersionHistory(@PathVariable Long fileId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        log.info("获取版本历史请求: fileId={}", fileId);
        List<Map<String, Object>> history = resourceFileService.getVersionHistory(fileId, page, size);
        return Result.success("获取版本历史成功", history);
    }
}
