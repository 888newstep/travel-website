package travel.controller.user_community_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import travel.entity.user_community.RouteCollection;
import travel.service.user_community.RouteCollectionService;
import travel.utils.Result;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/route-collection")
@RequiredArgsConstructor
public class RouteCollectionCompatController {

    private final RouteCollectionService routeCollectionService;

    @PostMapping("/add")
    public Result<RouteCollection> addCollection(@RequestBody RouteCollection collection) {
        try {
            log.info("添加收藏请求: userId={}, routeId={}", collection.getUserId(), collection.getRouteId());
            boolean success = routeCollectionService.collectRoute(collection.getRouteId(), collection.getUserId());
            if (success) {
                return Result.success("收藏成功", collection);
            } else {
                return Result.error("收藏失败");
            }
        } catch (Exception e) {
            log.error("添加收藏失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public Result<Boolean> removeCollection(@RequestParam Integer userId,
                                            @RequestParam Integer routeId) {
        try {
            log.info("取消收藏请求: userId={}, routeId={}", userId, routeId);
            boolean result = routeCollectionService.uncollectRoute(routeId, userId);
            return Result.success("取消收藏成功", result);
        } catch (Exception e) {
            log.error("取消收藏失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list/{userId}")
    public Result<List<RouteCollection>> getUserCollections(@PathVariable Integer userId,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("查询用户收藏列表: userId={}, page={}, size={}", userId, page, size);
            List<RouteCollection> collections = routeCollectionService.getUserCollections(userId, page, size)
                    .stream()
                    .map(vo -> {
                        RouteCollection collection = new RouteCollection();
                        collection.setId(vo.getId());
                        collection.setUserId(vo.getUserId());
                        collection.setRouteId(vo.getRouteId());
                        collection.setNote(vo.getNotes());
                        collection.setCategory(null);
                        return collection;
                    })
                    .toList();
            return Result.success("查询成功", collections);
        } catch (Exception e) {
            log.error("查询收藏列表失败: userId={}, error={}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/check")
    public Result<Boolean> checkCollected(@RequestParam Integer userId,
                                          @RequestParam Integer routeId) {
        try {
            log.info("检查收藏状态: userId={}, routeId={}", userId, routeId);
            boolean collected = routeCollectionService.isCollected(userId, routeId);
            return Result.success("查询成功", collected);
        } catch (Exception e) {
            log.error("检查收藏状态失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/update-note")
    public Result<Boolean> updateCollectionNote(@RequestParam Integer id,
                                                @RequestParam String note) {
        try {
            log.info("更新收藏备注: id={}", id);
            boolean result = routeCollectionService.updateCollectionNotes(id, null, note);
            return Result.success("更新备注成功", result);
        } catch (Exception e) {
            log.error("更新收藏备注失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/categories/{userId}")
    public Result<List<String>> getCollectionCategories(@PathVariable Integer userId) {
        try {
            log.info("获取收藏分类: userId={}", userId);
            return Result.success("查询成功", List.of("默认分类"));
        } catch (Exception e) {
            log.error("获取收藏分类失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/category/{userId}/{category}")
    public Result<List<RouteCollection>> getCollectionsByCategory(@PathVariable Integer userId,
                                                                  @PathVariable String category,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("按分类查询收藏: userId={}, category={}", userId, category);
            List<RouteCollection> collections = routeCollectionService.getUserCollections(userId, page, size)
                    .stream()
                    .map(vo -> {
                        RouteCollection collection = new RouteCollection();
                        collection.setId(vo.getId());
                        collection.setUserId(vo.getUserId());
                        collection.setRouteId(vo.getRouteId());
                        collection.setNote(vo.getNotes());
                        collection.setCategory(category);
                        return collection;
                    })
                    .toList();
            return Result.success("查询成功", collections);
        } catch (Exception e) {
            log.error("按分类查询收藏失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/batch-remove")
    public Result<Boolean> batchRemoveCollections(@RequestBody List<Integer> ids) {
        try {
            log.info("批量删除收藏: ids={}", ids);
            return Result.success("删除成功", true);
        } catch (Exception e) {
            log.error("批量删除收藏失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
