package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.ResourceFile;
import travel.service.travel_recommendation.ResourceFileService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文件版本控制器
 * 处理文件版本管理和历史记录
 */
@RestController
@RequestMapping("/file-version")
@RequiredArgsConstructor
public class FileVersionController {

    private static final Logger log = LoggerFactory.getLogger(FileVersionController.class);

    private final ResourceFileService resourceFileService;

    /**
     * 创建文件版本
     * POST /api/file-version/create
     */
    @PostMapping("/create")
    public Result<ResourceFile> createResourceFile(@RequestBody ResourceFile version) {
        try {
            log.info("创建文件版本请求: fileId={}", version.getFileId());
            ResourceFile result = resourceFileService.createResourceFile(version);
            return Result.success("创建版本成功", result);
        } catch (Exception e) {
            log.error("创建文件版本失败: error={}", e.getMessage());
            return Result.error("创建版本失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件版本详情
     * GET /api/file-version/{id}
     */
    @GetMapping("/{id}")
    public Result<ResourceFile> getResourceFile(@PathVariable Long id) {
        try {
            log.info("获取文件版本详情请求: id={}", id);
            ResourceFile version = resourceFileService.getResourceFile(id);
            return Result.success("获取版本详情成功", version);
        } catch (Exception e) {
            log.error("获取文件版本详情失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取版本详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的所有版本
     * GET /api/file-version/file/{fileId}
     */
    @GetMapping("/file/{fileId}")
    public Result<List<ResourceFile>> getResourceFiles(@PathVariable Long fileId) {
        try {
            log.info("获取文件的所有版本请求: fileId={}", fileId);
            List<ResourceFile> versions = resourceFileService.getResourceFiles(fileId);
            return Result.success("获取版本列表成功", versions);
        } catch (Exception e) {
            log.error("获取文件的所有版本失败: fileId={}, error={}", fileId, e.getMessage());
            return Result.error("获取版本列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的最新版本
     * GET /api/file-version/latest/{fileId}
     */
    @GetMapping("/latest/{fileId}")
    public Result<ResourceFile> getLatestVersion(@PathVariable Long fileId) {
        try {
            log.info("获取文件的最新版本请求: fileId={}", fileId);
            ResourceFile version = resourceFileService.getLatestVersion(fileId);
            return Result.success("获取最新版本成功", version);
        } catch (Exception e) {
            log.error("获取文件的最新版本失败: fileId={}, error={}", fileId, e.getMessage());
            return Result.error("获取最新版本失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件版本
     * DELETE /api/file-version/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteResourceFile(@PathVariable Long id) {
        try {
            log.info("删除文件版本请求: id={}", id);
            boolean result = resourceFileService.deleteResourceFile(id);
            return Result.success("删除版本成功", result);
        } catch (Exception e) {
            log.error("删除文件版本失败: id={}, error={}", id, e.getMessage());
            return Result.error("删除版本失败: " + e.getMessage());
        }
    }

    /**
     * 恢复到指定版本
     * POST /api/file-version/restore
     */
    @PostMapping("/restore")
    public Result<Boolean> restoreToVersion(@RequestParam Long fileId, @RequestParam Long versionId) {
        try {
            log.info("恢复到指定版本请求: fileId={}, versionId={}", fileId, versionId);
            boolean result = resourceFileService.restoreToVersion(fileId, versionId);
            return Result.success("恢复版本成功", result);
        } catch (Exception e) {
            log.error("恢复到指定版本失败: error={}", e.getMessage());
            return Result.error("恢复版本失败: " + e.getMessage());
        }
    }

    /**
     * 比较两个版本
     * POST /api/file-version/compare
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> compareVersions(@RequestParam Long version1Id, @RequestParam Long version2Id) {
        try {
            log.info("比较两个版本请求: version1Id={}, version2Id={}", version1Id, version2Id);
            Map<String, Object> comparison = resourceFileService.compareVersions(version1Id, version2Id);
            return Result.success("比较版本成功", comparison);
        } catch (Exception e) {
            log.error("比较两个版本失败: error={}", e.getMessage());
            return Result.error("比较版本失败: " + e.getMessage());
        }
    }

    /**
     * 获取版本历史
     * GET /api/file-version/history/{fileId}
     */
    @GetMapping("/history/{fileId}")
    public Result<List<Map<String, Object>>> getVersionHistory(@PathVariable Long fileId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取版本历史请求: fileId={}", fileId);
            List<Map<String, Object>> history = resourceFileService.getVersionHistory(fileId, page, size);
            return Result.success("获取版本历史成功", history);
        } catch (Exception e) {
            log.error("获取版本历史失败: fileId={}, error={}", fileId, e.getMessage());
            return Result.error("获取版本历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取版本统计信息
     * GET /api/file-version/statistics/{fileId}
     */
    @GetMapping("/statistics/{fileId}")
    public Result<Map<String, Object>> getVersionStatistics(@PathVariable Long fileId) {
        try {
            log.info("获取版本统计信息请求: fileId={}", fileId);
            Map<String, Object> statistics = resourceFileService.getVersionStatistics(fileId);
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取版本统计信息失败: fileId={}, error={}", fileId, e.getMessage());
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新版本备注
     * PUT /api/file-version/update-note/{id}
     */
    @PutMapping("/update-note/{id}")
    public Result<Boolean> updateVersionNote(@PathVariable Long id, @RequestParam String note) {
        try {
            log.info("更新版本备注请求: id={}", id);
            boolean result = resourceFileService.updateVersionNote(id, note);
            return Result.success("更新备注成功", result);
        } catch (Exception e) {
            log.error("更新版本备注失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新备注失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除版本
     * DELETE /api/file-version/batch-delete
     */
    @DeleteMapping("/batch-delete")
    public Result<Integer> batchDeleteVersions(@RequestBody List<Long> ids) {
        try {
            log.info("批量删除版本请求: count={}", ids.size());
            int count = resourceFileService.batchDeleteVersions(ids);
            return Result.success("批量删除成功", count);
        } catch (Exception e) {
            log.error("批量删除版本失败: error={}", e.getMessage());
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }
}