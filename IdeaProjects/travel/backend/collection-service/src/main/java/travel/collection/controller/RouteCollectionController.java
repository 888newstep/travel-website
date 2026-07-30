package travel.collection.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import travel.common.entity.user_community.RouteCollection;
import travel.common.vo.RouteCollectionVO;
import travel.collection.service.RouteCollectionService;
import travel.common.utils.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/route-collections")
@RequiredArgsConstructor
public class RouteCollectionController {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionController.class);
    private final RouteCollectionService routeCollectionService;

    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggleCollection(@RequestBody CollectRequest request) {
        try {
            log.info("切换路线收藏状态: routeId={}, userId={}", request.getRouteId(), request.getUserId());
            boolean alreadyCollected = routeCollectionService.isCollected(request.getRouteId(), request.getUserId());
            Map<String, Object> result = new HashMap<>();
            if (alreadyCollected) {
                routeCollectionService.uncollectRoute(request.getRouteId(), request.getUserId());
                result.put("collected", false);
            } else {
                routeCollectionService.collectRoute(request.getRouteId(), request.getUserId());
                result.put("collected", true);
            }
            return Result.success(alreadyCollected ? "取消收藏成功" : "收藏成功", result);
        } catch (Exception e) {
            log.error("切换路线收藏状态失败: error={}", e.getMessage());
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{userId}")
    public Result<List<RouteCollectionVO>> getUserCollections(@PathVariable Integer userId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("查询用户收藏列表请求: userId={}, page={}, size={}", userId, page, size);
            List<RouteCollectionVO> collections = routeCollectionService.getUserCollections(userId, page, size);
            return Result.success("查询成功", collections);
        } catch (Exception e) {
            log.error("查询用户收藏列表失败: userId={}, error={}", userId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/check")
    public Result<Boolean> checkCollected(@RequestParam Integer userId, @RequestParam Integer routeId) {
        try {
            log.info("检查路线收藏状态请求: userId={}, routeId={}", userId, routeId);
            boolean collected = routeCollectionService.isCollected(userId, routeId);
            return Result.success("查询成功", collected);
        } catch (Exception e) {
            log.error("检查路线收藏状态失败: error={}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/{collectionId}/notes")
    public Result<Boolean> updateCollectionNotes(@PathVariable Integer collectionId,
                                                 @RequestBody UpdateNotesRequest request) {
        try {
            log.info("更新收藏备注请求: collectionId={}, userId={}", collectionId, request.getUserId());
            boolean result = routeCollectionService.updateCollectionNotes(collectionId, request.getUserId(), request.getNotes());
            return Result.success("更新备注成功", result);
        } catch (Exception e) {
            log.error("更新收藏备注失败: collectionId={}, error={}", collectionId, e.getMessage());
            return Result.error("更新备注失败: " + e.getMessage());
        }
    }

    @PutMapping("/{collectionId}/public-status")
    public Result<Boolean> updatePublicStatus(@PathVariable Integer collectionId,
                                              @RequestBody UpdatePublicStatusRequest request) {
        try {
            log.info("更新收藏公开状态请求: collectionId={}, userId={}", collectionId, request.getUserId());
            boolean result = routeCollectionService.updateCollectionPublicStatus(collectionId, request.getUserId(), request.getIsPublic());
            return Result.success("更新状态成功", result);
        } catch (Exception e) {
            log.error("更新收藏公开状态失败: collectionId={}, error={}", collectionId, e.getMessage());
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/public")
    public Result<List<travel.common.entity.user_community.RouteCollection>> getPublicCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取公开收藏列表请求: page={}, size={}", page, size);
            List<travel.common.entity.user_community.RouteCollection> collections = routeCollectionService.getPublicCollections(page, size);
            return Result.success("查询成功", collections);
        } catch (Exception e) {
            log.error("获取公开收藏列表失败: error={}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

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

    @GetMapping("/category/{userId}/{category}")
    public Result<List<RouteCollection>> getCollectionsByCategory(
            @PathVariable Integer userId,
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("按分类获取收藏请求: userId={}, category={}, page={}, size={}", userId, category, page, size);
            List<RouteCollectionVO> collections = routeCollectionService.getUserCollections(userId, page + 1, size);
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
    public static class CollectRequest {
        private Integer routeId;
        private Integer userId;
    }

    @Data
    public static class UpdateNotesRequest {
        private Integer userId;
        private String notes;
    }

    @Data
    public static class UpdatePublicStatusRequest {
        private Integer userId;
        private Boolean isPublic;
    }
}
