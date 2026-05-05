package travel.controller.user_community_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.RouteShare;
import travel.service.user_community.RouteShareService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 路线分享控制器
 * 处理用户路线分享、分享链接管理和访问控制
 */
@Slf4j
@RestController
@RequestMapping("/route-share")
@RequiredArgsConstructor
public class RouteShareController {

    private final RouteShareService routeShareService;

    /**
     * 创建路线分享
     * POST /api/route-share/create
     */
    @PostMapping("/create")
    public Result<RouteShare> createRouteShare(@RequestBody RouteShare routeShare) {
        try {
            log.info("创建路线分享请求: userId={}, routeId={}", routeShare.getUserId(), routeShare.getRouteId());
            RouteShare result = routeShareService.createRouteShare(routeShare);
            return Result.success("创建分享成功", result);
        } catch (Exception e) {
            log.error("创建路线分享失败: error={}", e.getMessage());
            return Result.error("创建分享失败: " + e.getMessage());
        }
    }

    /**
     * 通过分享码获取路线信息
     * GET /api/route-share/info/{shareCode}
     */
    @GetMapping("/info/{shareCode}")
    public Result<RouteShare> getShareInfo(@PathVariable String shareCode) {
        try {
            log.info("获取分享路线信息请求: shareCode={}", shareCode);
            RouteShare routeShare = routeShareService.getShareInfo(shareCode);
            return Result.success("获取分享信息成功", routeShare);
        } catch (Exception e) {
            log.error("获取分享路线信息失败: shareCode={}, error={}", shareCode, e.getMessage());
            return Result.error("获取分享信息失败: " + e.getMessage());
        }
    }

    /**
     * 访问分享路线
     * GET /api/route-share/access/{shareCode}
     */
    @GetMapping("/access/{shareCode}")
    public Result<Map<String, Object>> accessShareRoute(@PathVariable String shareCode) {
        try {
            log.info("访问分享路线请求: shareCode={}", shareCode);
            Map<String, Object> routeInfo = routeShareService.accessShareRoute(shareCode);
            return Result.success("访问成功", routeInfo);
        } catch (Exception e) {
            log.error("访问分享路线失败: shareCode={}, error={}", shareCode, e.getMessage());
            return Result.error("访问失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的分享列表
     * GET /api/route-share/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<RouteShare>> getUserShares(@PathVariable Integer userId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("查询用户分享列表请求: userId={}, page={}, size={}", userId, page, size);
            List<RouteShare> shares = routeShareService.getUserShares(userId, page, size);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            log.error("查询用户分享列表失败: userId={}, error={}", userId, e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 取消分享
     * DELETE /api/route-share/cancel/{id}
     */
    @DeleteMapping("/cancel/{id}")
    public Result<Boolean> cancelShare(@PathVariable Long id) {
        try {
            log.info("取消分享请求: id={}", id);
            boolean result = routeShareService.cancelShare(id);
            return Result.success("取消分享成功", result);
        } catch (Exception e) {
            log.error("取消分享失败: id={}, error={}", id, e.getMessage());
            return Result.error("取消分享失败: " + e.getMessage());
        }
    }

    /**
     * 更新分享设置
     * PUT /api/route-share/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<Boolean> updateShareSettings(@PathVariable Long id, @RequestBody Map<String, Object> settings) {
        try {
            log.info("更新分享设置请求: id={}", id);
            boolean result = routeShareService.updateShareSettings(id, settings);
            return Result.success("更新设置成功", result);
        } catch (Exception e) {
            log.error("更新分享设置失败: id={}, error={}", id, e.getMessage());
            return Result.error("更新设置失败: " + e.getMessage());
        }
    }

    /**
     * 增加分享访问次数
     * POST /api/route-share/visit/{shareCode}
     */
    @PostMapping("/visit/{shareCode}")
    public Result<Boolean> increaseVisitCount(@PathVariable String shareCode) {
        try {
            log.info("增加分享访问次数请求: shareCode={}", shareCode);
            boolean result = routeShareService.increaseVisitCount(shareCode);
            return Result.success("记录成功", result);
        } catch (Exception e) {
            log.error("增加分享访问次数失败: shareCode={}, error={}", shareCode, e.getMessage());
            return Result.error("记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取分享统计信息
     * GET /api/route-share/statistics/{id}
     */
    @GetMapping("/statistics/{id}")
    public Result<Map<String, Object>> getShareStatistics(@PathVariable Long id) {
        try {
            log.info("获取分享统计信息请求: id={}", id);
            Map<String, Object> statistics = routeShareService.getShareStatistics(id);
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取分享统计信息失败: id={}, error={}", id, e.getMessage());
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门分享
     * GET /api/route-share/popular
     */
    @GetMapping("/popular")
    public Result<List<RouteShare>> getPopularShares(@RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("获取热门分享请求: limit={}", limit);
            List<RouteShare> shares = routeShareService.getPopularShares(limit);
            return Result.success("获取成功", shares);
        } catch (Exception e) {
            log.error("获取热门分享失败: error={}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
