package travel.controller.travel_recommendation_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.travel_recommendation.FileTag;
import travel.service.travel_recommendation.FileTagService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file-category")
@RequiredArgsConstructor
public class FileCategoryController {

    private static final Logger log = LoggerFactory.getLogger(FileCategoryController.class);

    private final FileTagService fileTagService;

    @PostMapping("/create")
    public Result<FileTag> createFileTag(@RequestBody FileTag category) {
        try {
            log.info("创建文件分类请求: name={}", category.getTagName());
            FileTag result = fileTagService.createFileTag(category);
            return Result.success("创建分类成功", result);
        } catch (Exception e) {
            log.error("创建文件分类失败: error={}", e.getMessage());
            return Result.error("创建分类失败: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public Result<FileTag> updateFileTag(@PathVariable Long id, @RequestBody FileTag category) {
        try {
            log.info("更新文件分类请求: id={}", id);
            FileTag result = fileTagService.updateFileTag(id, category);
            return Result.success("更新分类成功", result);
        } catch (Exception e) {
            log.error("更新文件分类失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新分类失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteFileTag(@PathVariable Long id) {
        try {
            log.info("删除文件分类请求: id={}", id);
            boolean result = fileTagService.deleteFileTag(id);
            return Result.success("删除分类成功", result);
        } catch (Exception e) {
            log.error("删除文件分类失败: id={}, error={}", id, e.getMessage());
            return Result.error("删除分类失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<FileTag> getFileTag(@PathVariable Long id) {
        try {
            log.info("获取文件分类详情请求: id={}", id);
            FileTag category = fileTagService.getFileTag(id);
            return Result.success("获取分类详情成功", category);
        } catch (Exception e) {
            log.error("获取文件分类详情失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取分类详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<FileTag>> getAllCategories() {
        try {
            log.info("获取所有分类请求");
            List<FileTag> categories = fileTagService.getAllCategories();
            return Result.success("获取分类列表成功", categories);
        } catch (Exception e) {
            log.error("获取所有分类失败: error={}", e.getMessage());
            return Result.error("获取分类列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getCategoryTree() {
        try {
            log.info("获取分类树请求");
            List<Map<String, Object>> tree = fileTagService.getCategoryTree();
            return Result.success("获取分类树成功", tree);
        } catch (Exception e) {
            log.error("获取分类树失败: error={}", e.getMessage());
            return Result.error("获取分类树失败: " + e.getMessage());
        }
    }

    @GetMapping("/children/{parentId}")
    public Result<List<FileTag>> getChildCategories(@PathVariable Long parentId) {
        try {
            log.info("获取子分类请求: parentId={}", parentId);
            List<FileTag> children = fileTagService.getChildCategories(parentId);
            return Result.success("获取子分类成功", children);
        } catch (Exception e) {
            log.error("获取子分类失败: parentId={}, error={}", parentId, e.getMessage());
            return Result.error("获取子分类失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public Result<List<FileTag>> searchCategories(@RequestParam String keyword) {
        try {
            log.info("搜索分类请求: keyword={}", keyword);
            List<FileTag> categories = fileTagService.searchCategories(keyword);
            return Result.success("搜索分类成功", categories);
        } catch (Exception e) {
            log.error("搜索分类失败: keyword={}, error={}", keyword, e.getMessage());
            return Result.error("搜索分类失败: " + e.getMessage());
        }
    }

    @PostMapping("/move")
    public Result<Boolean> moveCategory(@RequestParam Long categoryId, @RequestParam Long newParentId) {
        try {
            log.info("移动分类请求: categoryId={}, newParentId={}", categoryId, newParentId);
            boolean result = fileTagService.moveCategory(categoryId, newParentId);
            return Result.success("移动分类成功", result);
        } catch (Exception e) {
            log.error("移动分类失败: error={}", e.getMessage());
            return Result.error("移动分类失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics/{id}")
    public Result<Map<String, Object>> getCategoryStatistics(@PathVariable Long id) {
        try {
            log.info("获取分类统计信息请求: id={}", id);
            Map<String, Object> statistics = fileTagService.getCategoryStatistics(id);
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取分类统计信息失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-create")
    public Result<List<FileTag>> batchCreateCategories(@RequestBody List<FileTag> categories) {
        try {
            log.info("批量创建分类请求: count={}", categories.size());
            List<FileTag> result = fileTagService.batchCreateCategories(categories);
            return Result.success("批量创建成功", result);
        } catch (Exception e) {
            log.error("批量创建分类失败: error={}", e.getMessage());
            return Result.error("批量创建失败: " + e.getMessage());
        }
    }
}