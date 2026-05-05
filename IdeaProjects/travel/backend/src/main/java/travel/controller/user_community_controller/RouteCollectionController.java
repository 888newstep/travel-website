package travel.controller.user_community_controller;

import lombok.RequiredArgsConstructor;
import travel.entity.user_community.RouteCollection;
import travel.service.user_community.RouteCollectionService;
import travel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/route-collection")
@RequiredArgsConstructor
public class RouteCollectionController {

    private static final Logger log = LoggerFactory.getLogger(RouteCollectionController.class);

    private final RouteCollectionService routeCollectionService;

    @PostMapping("/add")
    public Result<RouteCollection> addCollection(@RequestBody RouteCollection collection) {
        try {
            log.info("添加路线收藏请求: userId={}, routeId={}", collection.getUserId(), collection.getRouteId());
            RouteCollection result = routeCollectionService.addCollection(collection);
            return Result.success("收藏成功", result);
        } catch (Exception e) {
            log.error("添加路线收藏失败: error={}", e.getMessage());
            return Result.error("收藏失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public Result<Boolean> removeCollection(@RequestParam Integer userId, @RequestParam Integer routeId) {
        try {
            log.info("取消路线收藏请求: userId={}, routeId={}", userId, routeId);
            boolean result = routeCollectionService.removeCollection(userId, routeId);
            return Result.success("取消收藏成功", result);
        } catch (Exception e) {
            log.error("取消路线收藏失败: error={}", e.getMessage());
            return Result.error("取消收藏失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{userId}")
    public Result<List<RouteCollection>> getUserCollections(@PathVariable Integer userId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("查询用户收藏列表请求: userId={}, page={}, size={}", userId, page, size);
            List<RouteCollection> collections = routeCollectionService.getUserCollections(userId, page, size);
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
            boolean collected = routeCollectionService.checkCollected(userId, routeId);
            return Result.success("查询成功", collected);
        } catch (Exception e) {
            log.error("检查路线收藏状态失败: error={}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/update-note")
    public Result<Boolean> updateCollectionNote(@RequestParam Long id, @RequestParam String note) {
        try {
            log.info("更新收藏备注请求: id={}", id);
            boolean result = routeCollectionService.updateCollectionNote(id, note);
            return Result.success("更新备注成功", result);
        } catch (Exception e) {
            log.error("更新收藏备注失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新备注失败: " + e.getMessage());
        }
    }

    @GetMapping("/categories/{userId}")
    public Result<List<String>> getCollectionCategories(@PathVariable Integer userId) {
        try {
            log.info("获取收藏分类列表请求: userId={}", userId);
            List<String> categories = routeCollectionService.getCollectionCategories(userId);
            return Result.success("获取分类成功", categories);
        } catch (Exception e) {
            log.error("获取收藏分类列表失败: userId={}, error={}", userId, e.getMessage());
            return Result.error("获取分类失败: " + e.getMessage());
        }
    }

    @GetMapping("/category/{userId}/{category}")
    public Result<List<RouteCollection>> getCollectionsByCategory(@PathVariable Integer userId,
                                                                   @PathVariable String category,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("按分类查询收藏请求: userId={}, category={}", userId, category);
            List<RouteCollection> collections = routeCollectionService.getCollectionsByCategory(userId, category, page, size);
            return Result.success("查询成功", collections);
        } catch (Exception e) {
            log.error("按分类查询收藏失败: userId={}, category={}, error={}", userId, category, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch-remove")
    public Result<Integer> batchRemoveCollections(@RequestBody List<Long> ids) {
        try {
            log.info("批量取消收藏请求: count={}", ids.size());
            int count = routeCollectionService.batchRemoveCollections(ids);
            return Result.success("批量取消成功", count);
        } catch (Exception e) {
            log.error("批量取消收藏失败: error={}", e.getMessage());
            return Result.error("批量取消失败: " + e.getMessage());
        }
    }
}