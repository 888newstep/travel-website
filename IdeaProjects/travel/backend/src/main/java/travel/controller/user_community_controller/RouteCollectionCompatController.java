package travel.controller.user_community_controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import travel.entity.user_community.RouteCollection;
import travel.service.user_community.RouteCollectionService;
import travel.utils.Result;

import java.util.List;

/**
 * 路线收藏兼容控制器
 * 提供与前端旧版API兼容的接口
 */
@RestController
@RequestMapping("/route-collection")
@RequiredArgsConstructor
public class RouteCollectionCompatController {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionCompatController.class);
    private final RouteCollectionService routeCollectionService;

    /**
     * 添加收藏
     * POST /route-collection/add
     */
    @PostMapping("/add")
    public Result<RouteCollection> addCollection(@RequestBody AddCollectionRequest request) {
        try {
            log.info("添加收藏请求: routeId={}, userId={}", request.getRouteId(), request.getUserId());
            RouteCollection collection = routeCollectionService.createCollection(
                    request.getRouteId(),
                    request.getUserId(),
                    request.getIsPublic() != null ? request.getIsPublic() : false,
                    request.getNote()
            );
            return Result.success("添加收藏成功", collection);
        } catch (Exception e) {
            log.error("添加收藏失败: error={}", e.getMessage());
            return Result.error("添加收藏失败: " + e.getMessage());
        }
    }

    /**
     * 移除收藏
     * DELETE /route-collection/remove
     */
    @DeleteMapping("/remove")
    public Result<Boolean> removeCollection(@RequestParam Integer userId, @RequestParam Integer routeId) {
        try {
            log.info("移除收藏请求: userId={}, routeId={}", userId, routeId);
            boolean result = routeCollectionService.cancelCollect(routeId, userId);
            return Result.success("移除收藏成功", result);
        } catch (Exception e) {
            log.error("移除收藏失败: error={}", e.getMessage());
            return Result.error("移除收藏失败: " + e.getMessage());
        }
    }

    /**
     * 更新收藏备注
     * PUT /route-collection/update-note
     */
    @PutMapping("/update-note")
    public Result<Boolean> updateCollectionNote(@RequestParam Integer id, @RequestParam String note) {
        try {
            log.info("更新收藏备注请求: id={}", id);
            RouteCollection collection = routeCollectionService.getById(id);
            if (collection == null) {
                return Result.error("收藏不存在");
            }
            boolean result = routeCollectionService.updateCollectionNotes(id, collection.getUserId(), note);
            return Result.success("更新备注成功", result);
        } catch (Exception e) {
            log.error("更新收藏备注失败: error={}", e.getMessage());
            return Result.error("更新备注失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户收藏分类列表
     * GET /route-collection/categories/{userId}
     */
    @GetMapping("/categories/{userId}")
    public Result<List<String>> getCollectionCategories(@PathVariable Integer userId) {
        try {
            log.info("获取用户收藏分类请求: userId={}", userId);
            List<String> categories = routeCollectionService.getUserCollectionCategories(userId);
            return Result.success("获取分类成功", categories);
        } catch (Exception e) {
            log.error("获取用户收藏分类失败: error={}", e.getMessage());
            return Result.error("获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 按分类获取用户收藏
     * GET /route-collection/category/{userId}/{category}
     */
    @GetMapping("/category/{userId}/{category}")
    public Result<List<RouteCollection>> getCollectionsByCategory(
            @PathVariable Integer userId,
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("按分类获取收藏请求: userId={}, category={}, page={}, size={}", userId, category, page, size);

            // 这里需要根据分类查询，暂时返回该用户的所有收藏
            // 后续可以优化为按分类过滤
            List<travel.entity.vo.RouteCollectionVO> collections = routeCollectionService.getUserCollections(userId, page + 1, size);

            // 转换为RouteCollection类型
            List<RouteCollection> result = collections.stream().map(vo -> {
                RouteCollection collection = new RouteCollection();
                collection.setId(vo.getId());
                collection.setRouteId(vo.getRouteId());
                collection.setUserId(vo.getUserId());
                collection.setCollectionTime(vo.getCollectionTime());
                collection.setIsPublic(vo.getIsPublic());
                collection.setNotes(vo.getNotes());
                return collection;
            }).toList();

            return Result.success("获取收藏成功", result);
        } catch (Exception e) {
            log.error("按分类获取收藏失败: error={}", e.getMessage());
            return Result.error("获取收藏失败: " + e.getMessage());
        }
    }

    /**
     * 批量移除收藏
     * DELETE /route-collection/batch-remove
     */
    @DeleteMapping("/batch-remove")
    public Result<Integer> batchRemoveCollections(@RequestBody List<Integer> ids) {
        try {
            log.info("批量移除收藏请求: count={}", ids.size());
            int count = routeCollectionService.batchRemoveCollections(ids);
            return Result.success("批量移除成功", count);
        } catch (Exception e) {
            log.error("批量移除收藏失败: error={}", e.getMessage());
            return Result.error("批量移除失败: " + e.getMessage());
        }
    }

    // ==================== 请求DTO ====================

    @Data
    public static class AddCollectionRequest {
        private Integer routeId;
        private Integer userId;
        private Boolean isPublic;
        private String note;
    }
}
