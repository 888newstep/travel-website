package travel.controller.route_planning_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import travel.entity.route_planning.RouteRating;
import travel.service.route_planning.RouteRatingService;
import travel.service.user_community.UserService;
import travel.utils.Result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/route-rating")
@RequiredArgsConstructor
@Validated
@Tag(name = "路线评分管理", description = "用户对路线的评分接口")
public class RouteRatingController {

    private final RouteRatingService routeRatingService;
    private final UserService userService;

    @PostMapping("/submit")
    @Operation(summary = "提交路线评分", description = "用户对路线进行1-5星评分")
    public Result<RouteRating> submitRating(@RequestBody @Valid RatingRequest request) {
        try {
            var currentUser = userService.getCurrentUser();

            if (routeRatingService.hasRated(request.getRouteId(), currentUser.getId())) {
                return Result.error("您已经对该路线评过分了");
            }

            RouteRating rating = new RouteRating();
            rating.setRouteId(request.getRouteId());
            rating.setUserId(currentUser.getId());
            rating.setRating(request.getRating());
            rating.setReview(request.getComment());

            routeRatingService.save(rating);

            log.info("用户提交路线评分: userId={}, routeId={}, rating={}",
                    currentUser.getId(), request.getRouteId(), request.getRating());

            return Result.success("评分提交成功", rating);
        } catch (Exception e) {
            log.error("提交路线评分失败: error={}", e.getMessage());
            return Result.error("评分提交失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新路线评分", description = "用户修改自己的评分")
    public Result<RouteRating> updateRating(@RequestBody @Valid RatingRequest request) {
        try {
            var currentUser = userService.getCurrentUser();

            RouteRating existingRating = routeRatingService.getUserRating(request.getRouteId(), currentUser.getId());
            if (existingRating == null) {
                return Result.error("您还没有对该路线评过分");
            }

            existingRating.setRating(request.getRating());
            existingRating.setReview(request.getComment());

            routeRatingService.updateById(existingRating);

            log.info("用户更新路线评分: userId={}, routeId={}, rating={}",
                    currentUser.getId(), request.getRouteId(), request.getRating());

            return Result.success("评分更新成功", existingRating);
        } catch (Exception e) {
            log.error("更新路线评分失败: error={}", e.getMessage());
            return Result.error("评分更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{routeId}")
    @Operation(summary = "删除路线评分", description = "用户删除自己的评分")
    public Result<Boolean> deleteRating(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            var currentUser = userService.getCurrentUser();

            RouteRating existingRating = routeRatingService.getUserRating(routeId, currentUser.getId());
            if (existingRating == null) {
                return Result.error("您还没有对该路线评过分");
            }

            routeRatingService.removeById(existingRating.getId());

            log.info("用户删除路线评分: userId={}, routeId={}",
                    currentUser.getId(), routeId);

            return Result.success("评分删除成功", true);
        } catch (Exception e) {
            log.error("删除路线评分失败: error={}", e.getMessage());
            return Result.error("评分删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}")
    @Operation(summary = "获取路线的所有评分")
    public Result<List<RouteRating>> getRouteRatings(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            List<RouteRating> ratings = routeRatingService.getByRouteId(routeId);
            return Result.success("获取评分列表成功", ratings);
        } catch (Exception e) {
            log.error("获取路线评分失败: error={}", e.getMessage());
            return Result.error("获取评分失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}/average")
    @Operation(summary = "获取路线平均评分")
    public Result<Double> getAverageRating(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            Double averageRating = routeRatingService.getAverageRating(routeId);
            return Result.success("获取平均评分成功", averageRating);
        } catch (Exception e) {
            log.error("获取平均评分失败: error={}", e.getMessage());
            return Result.error("获取平均评分失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}/count")
    @Operation(summary = "获取路线评分数量")
    public Result<Integer> getRatingCount(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            Integer count = routeRatingService.getRatingCount(routeId);
            return Result.success("获取评分数量成功", count);
        } catch (Exception e) {
            log.error("获取评分数量失败: error={}", e.getMessage());
            return Result.error("获取评分数量失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}/user")
    @Operation(summary = "获取当前用户对该路线的评分")
    public Result<RouteRating> getUserRating(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            var currentUser = userService.getCurrentUser();
            RouteRating rating = routeRatingService.getUserRating(routeId, currentUser.getId());
            return Result.success("获取用户评分成功", rating);
        } catch (Exception e) {
            log.error("获取用户评分失败: error={}", e.getMessage());
            return Result.error("获取用户评分失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/history")
    @Operation(summary = "获取用户的评分历史")
    public Result<List<RouteRating>> getUserRatingHistory() {
        try {
            var currentUser = userService.getCurrentUser();
            List<RouteRating> ratings = routeRatingService.getByUserId(currentUser.getId());
            return Result.success("获取评分历史成功", ratings);
        } catch (Exception e) {
            log.error("获取用户评分历史失败: error={}", e.getMessage());
            return Result.error("获取评分历史失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}/stats")
    @Operation(summary = "获取路线评分统计信息")
    public Result<Map<String, Object>> getRatingStats(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            List<RouteRating> ratings = routeRatingService.getByRouteId(routeId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRatings", ratings.size());
            stats.put("averageRating", routeRatingService.getAverageRating(routeId));

            Map<Integer, Long> distribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                int finalI = i;
                long count = ratings.stream().filter(r -> r.getRating() == finalI).count();
                distribution.put(i, count);
            }
            stats.put("ratingDistribution", distribution);

            return Result.success("获取评分统计成功", stats);
        } catch (Exception e) {
            log.error("获取评分统计失败: error={}", e.getMessage());
            return Result.error("获取评分统计失败: " + e.getMessage());
        }
    }

    @lombok.Data
    public static class RatingRequest {
        @NotNull(message = "路线ID不能为空")
        private Integer routeId;

        @Min(value = 1, message = "评分最低为1")
        @Max(value = 5, message = "评分最高为5")
        @NotNull(message = "评分不能为空")
        private Integer rating;

        private String comment;
    }
}
