package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.travel_recommendation.ResourceFile;
import travel.service.travel_recommendation.ResourceFileService;
import travel.utils.Result;
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

    /**
     * 上传资源文件
     * POST /api/resource-file/upload
     */
    @PostMapping("/upload")
    public Result<ResourceFile> uploadResourceFile(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String description) {
        try {
            log.info("上传资源文件请求: filename={}", file.getOriginalFilename());
            ResourceFile result = resourceFileService.uploadResourceFile(file, category, description);
            return Result.success("上传成功", result);
        } catch (Exception e) {
            log.error("上传资源文件失败: error={}", e.getMessage());
            return Result.error("上传失败: " + e.getMessage());
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
            log.info("批量上传资源文件请求: count={}", files.length);
            List<ResourceFile> result = resourceFileService.batchUploadResourceFiles(files, category);
            return Result.success("批量上传成功", result);
        } catch (Exception e) {
            log.error("批量上传资源文件失败: error={}", e.getMessage());
            return Result.error("批量上传失败: " + e.getMessage());
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
            return Result.error("获取详情失败: " + e.getMessage());
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
            return Result.success("获取下载链接成功", downloadUrl);
        } catch (Exception e) {
            log.error("下载资源文件失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取下载链接失败: " + e.getMessage());
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
            return Result.error("删除失败: " + e.getMessage());
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
            return Result.error("更新失败: " + e.getMessage());
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
            return Result.error("获取列表失败: " + e.getMessage());
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
            return Result.error("搜索失败: " + e.getMessage());
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
            return Result.error("获取预览链接失败: " + e.getMessage());
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
            return Result.error("获取统计信息失败: " + e.getMessage());
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
            return Result.error("批量删除失败: " + e.getMessage());
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
            return Result.error("获取类型统计失败: " + e.getMessage());
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
            return Result.error("移动失败: " + e.getMessage());
        }
    }
}
