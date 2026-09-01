package travel.collection.controller;


import lombok.RequiredArgsConstructor;
import travel.collection.service.UserStatisticsService;
import travel.collection.dto.UserStatisticsResponse;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/user/stats")
@RequiredArgsConstructor
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    /**
     * 获取当前用户统计信息
     * GET /api/v1/user/stats
     */
    @GetMapping
    public Result<UserStatisticsResponse> getUserStats() {
        UserStatisticsResponse stats = userStatisticsService.getCurrentUserStats();
        return Result.success("获取统计信息成功", stats);
    }

}
