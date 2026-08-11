package travel.collection.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.collection.service.UserStatisticsService;
import travel.collection.service.UserService;
import travel.collection.util.CurrentUserSupport;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/user/stats")
@Slf4j
@RequiredArgsConstructor
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;
    private final UserService userService;

    /**
     * 获取当前用户统计信息
     * GET /api/v1/user/stats
     */
    @GetMapping
    public Result<Map<String, Object>> getUserStats() {
        try {
            var currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
            Map<String, Object> stats = userStatisticsService.getUserStats(currentUser.getId());
            return Result.success("获取统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取用户统计信息失败: error={}", e.getMessage());
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定用户统计信息
     * GET /api/v1/user/stats/{userId}
     */
    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getUserStatsById(@PathVariable Integer userId) {
        try {
            log.info("获取用户统计信息请求: userId={}", userId);
            Map<String, Object> stats = userStatisticsService.getUserStats(userId);
            return Result.success("获取统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取用户统计信息失败: userId={}, error={}", userId, e.getMessage());
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }
}
