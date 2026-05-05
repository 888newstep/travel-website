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
 * 文件分享控制器
 * 处理用户文件分享、下载和权限管理
 */
@Slf4j
@RestController
@RequestMapping("/file-share")
@RequiredArgsConstructor
public class FileShareController {

    private final RouteShareService routeShareService;

    /**
     * 创建文件分享链接
     * POST /api/file-share/create
     */
    @PostMapping("/create")
    public Result<RouteShare> createRouteShare(@RequestBody RouteShare fileShare) {
        try {
            log.info("创建文件分享链接请求: userId={}, fileName={}", fileShare.getUserId(), fileShare.getFileName());
            RouteShare result = routeShareService.createRouteShare(fileShare);
            return Result.success("创建分享链接成功", result);
        } catch (Exception e) {
            log.error("创建文件分享链接失败: error={}", e.getMessage());
            return Result.error("创建分享链接失败: " + e.getMessage());
        }
    }

    /**
     * 通过分享码获取文件信息
     * GET /api/file-share/info/{shareCode}
     */
    @GetMapping("/info/{shareCode}")
    public Result<RouteShare> getShareInfo(@PathVariable String shareCode) {
        try {
            log.info("获取分享文件信息请求: shareCode={}", shareCode);
            RouteShare fileShare = routeShareService.getShareInfo(shareCode);
            return Result.success("获取分享信息成功", fileShare);
        } catch (Exception e) {
            log.error("获取分享文件信息失败: shareCode={}, error={}", shareCode, e.getMessage());
            return Result.error("获取分享信息失败: " + e.getMessage());
        }
    }

    /**
     * 访问分享文件
     * GET /api/file-share/access/{shareCode}
     */
    @GetMapping("/access/{shareCode}")
    public Result<String> accessShareFile(@PathVariable String shareCode,
                                          @RequestParam(required = false) String password) {
        try {
            log.info("访问分享文件请求: shareCode={}", shareCode);
            String accessUrl = routeShareService.accessShareFile(shareCode, password);
            return Result.success("访问成功", accessUrl);
        } catch (Exception e) {
            log.error("访问分享文件失败: shareCode={}, error={}", shareCode, e.getMessage());
            return Result.error("访问失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的分享列表
     * GET /api/file-share/user/{userId}
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
     * DELETE /api/file-share/cancel/{id}
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
     * PUT /api/file-share/update/{id}
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
     * 获取分享统计信息
     * GET /api/file-share/statistics/{id}
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
     * 批量取消分享
     * POST /api/file-share/batch-cancel
     */
    @PostMapping("/batch-cancel")
    public Result<Integer> batchCancelShares(@RequestBody List<Long> ids) {
        try {
            log.info("批量取消分享请求: count={}", ids.size());
            int count = routeShareService.batchCancelShares(ids);
            return Result.success("批量取消成功", count);
        } catch (Exception e) {
            log.error("批量取消分享失败: error={}", e.getMessage());
            return Result.error("批量取消失败: " + e.getMessage());
        }
    }
}
