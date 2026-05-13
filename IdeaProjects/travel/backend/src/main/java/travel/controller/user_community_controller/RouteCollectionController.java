package travel.controller.user_community_controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import travel.entity.vo.RouteCollectionVO;
import travel.service.user_community.RouteCollectionService;
import travel.utils.Result;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/route-collections")
@RequiredArgsConstructor
public class RouteCollectionController {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionController.class);
    private final RouteCollectionService routeCollectionService;

    @PostMapping("/collect")
    public Result<Boolean> collectRoute(@RequestBody CollectRequest request) {
        try {
            log.info("收藏路线请求: routeId={}, userId={}", request.getRouteId(), request.getUserId());
            boolean result = routeCollectionService.collectRoute(request.getRouteId(), request.getUserId());
            return Result.success("收藏成功", result);
        } catch (Exception e) {
            log.error("收藏路线失败: error={}", e.getMessage());
            return Result.error("收藏失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/uncollect")
    public Result<Boolean> uncollectRoute(@RequestBody CollectRequest request) {
        try {
            log.info("取消路线收藏请求: routeId={}, userId={}", request.getRouteId(), request.getUserId());
            boolean result = routeCollectionService.uncollectRoute(request.getRouteId(), request.getUserId());
            return Result.success("取消收藏成功", result);
        } catch (Exception e) {
            log.error("取消路线收藏失败: error={}", e.getMessage());
            return Result.error("取消收藏失败: " + e.getMessage());
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
    public Result<List<travel.entity.user_community.RouteCollection>> getPublicCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("获取公开收藏列表请求: page={}, size={}", page, size);
            List<travel.entity.user_community.RouteCollection> collections = routeCollectionService.getPublicCollections(page, size);
            return Result.success("查询成功", collections);
        } catch (Exception e) {
            log.error("获取公开收藏列表失败: error={}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
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
