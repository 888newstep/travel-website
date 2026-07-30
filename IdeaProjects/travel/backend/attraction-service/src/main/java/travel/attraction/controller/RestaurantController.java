package travel.attraction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import travel.common.entity.travel_recommendation.Restaurant;
import travel.attraction.service.RestaurantService;
import travel.common.utils.Result;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/restaurants")
@Tag(name = "餐厅管理", description = "餐厅相关接口")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/city/{cityId}")
    @Operation(summary = "获取城市餐厅列表")
    public Result<List<Restaurant>> getRestaurantsByCity(@PathVariable Integer cityId) {
        try {
            List<Restaurant> restaurants = restaurantService.getByCityId(cityId);
            return Result.success("获取餐厅列表成功", restaurants);
        } catch (Exception e) {
            log.error("获取餐厅列表失败: cityId={}, error={}", cityId, e.getMessage(), e);
            return Result.error("获取餐厅列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(summary = "搜索餐厅")
    public Result<List<Restaurant>> searchRestaurants(@RequestParam Integer cityId,
                                                      @RequestParam String keyword) {
        try {
            List<Restaurant> restaurants = restaurantService.search(cityId, keyword);
            return Result.success("搜索餐厅成功", restaurants);
        } catch (Exception e) {
            log.error("搜索餐厅失败: cityId={}, keyword={}, error={}", cityId, keyword, e.getMessage(), e);
            return Result.error("搜索餐厅失败: " + e.getMessage());
        }
    }

    @GetMapping("/top-rated/{cityId}")
    @Operation(summary = "获取高分餐厅")
    public Result<List<Restaurant>> getTopRatedRestaurants(@PathVariable Integer cityId,
                                                           @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Restaurant> restaurants = restaurantService.getTopRated(cityId, limit);
            return Result.success("获取高分餐厅成功", restaurants);
        } catch (Exception e) {
            log.error("获取高分餐厅失败: cityId={}, limit={}, error={}", cityId, limit, e.getMessage(), e);
            return Result.error("获取高分餐厅失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取餐厅详情")
    public Result<Map<String, Object>> getRestaurantById(@PathVariable Integer id) {
        try {
            Map<String, Object> detail = restaurantService.getRestaurantDetail(id);
            if (detail != null && !detail.isEmpty()) {
                return Result.success("获取餐厅详情成功", detail);
            } else {
                return Result.error("餐厅不存在");
            }
        } catch (Exception e) {
            log.error("获取餐厅详情失败: id={}, error={}", id, e.getMessage(), e);
            return Result.error("获取餐厅详情失败: " + e.getMessage());
        }
    }
}
