package travel.collection.controller;

import travel.common.exception.ExceptionPropagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.collection.service.UserStatisticsService;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/user/stats")
@Slf4j
@RequiredArgsConstructor
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    /**
     * 获取当前用户统计信息
     * GET /api/v1/user/stats
     */
    @GetMapping
    public Result<Map<String, Object>> getUserStats() {
        try {
            Map<String, Object> stats = userStatisticsService.getCurrentUserStats();
            return Result.success("获取统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取用户统计信息失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

}
