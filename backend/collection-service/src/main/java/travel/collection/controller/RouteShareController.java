package travel.collection.controller;

import travel.common.exception.ExceptionPropagation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.RouteShare;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.security.AuthenticatedUserSupport;
import travel.collection.service.RouteShareService;
import travel.collection.dto.SharedRouteAccessResponse;
import travel.collection.dto.ShareStatisticsResponse;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.baomidou.mybatisplus.extension.toolkit.Db.count;

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
     * 创建/生成路线分享（统一入口）
     * POST /api/route-share/generate
     * 支持两种方式：
     * 1. 简化模式：{ itemId, itemType }
     * 2. 完整模式：{ userId, routeId, itemId, itemType }
     */
    @PostMapping("/generate")
    public Result<RouteShare> generateShareCode(@RequestBody Map<String, Object> request) {
        try {
            Integer itemId = (Integer) request.get("itemId");
            String requestedItemType = (String) request.get("itemType");
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            Integer routeId = (Integer) request.get("routeId");
            String itemType = "note".equals(requestedItemType) || "travel_note".equals(requestedItemType)
                    ? "note" : "route";
            Integer effectiveItemId = routeId != null ? routeId : itemId;

            log.info("生成分享码请求: userId={}, itemId={}, itemType={}", userId, effectiveItemId, itemType);

            RouteShare share = new RouteShare();
            share.setItemId(effectiveItemId);
            share.setItemType(itemType);
            share.setUserId(userId);
            share.setRouteId("route".equals(itemType) ? effectiveItemId : null);

            RouteShare result = routeShareService.generateShareCode(share);
            return Result.success("生成分享码成功", result);
        } catch (Exception e) {
            log.error("生成分享码失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @GetMapping("/validate")
    public Result<Boolean> validateShareCode(@RequestParam(required = false) String code,
                                             @RequestParam(required = false) String shareCode) {
        try {
            log.info("验证分享码请求: code={}", code);
            String actualCode = (code != null && !code.isBlank()) ? code : shareCode;
            boolean valid = routeShareService.validateShareCode(actualCode);
            return Result.success("验证成功", valid);
        } catch (Exception e) {
            log.error("验证分享码失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 访问分享路线
     * GET /api/route-share/access/{shareCode}
     */
    @GetMapping("/access/{shareCode}")
    public Result<SharedRouteAccessResponse> accessShareRoute(@PathVariable String shareCode) {
        try {
            log.info("访问分享路线请求: shareCode={}", shareCode);
            SharedRouteAccessResponse routeInfo = routeShareService.accessShareRoute(shareCode);
            return Result.success("访问成功", routeInfo);
        } catch (Exception e) {
            log.error("访问分享路线失败: shareCode={}, error={}", shareCode, e.getMessage());
            throw ExceptionPropagation.propagate(e);
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
            Integer currentUserId = AuthenticatedUserSupport.requireIntegerUserId();
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
            }
            log.info("查询用户分享列表请求: userId={}, page={}, size={}", currentUserId, page, size);
            List<RouteShare> shares = routeShareService.getUserShares(currentUserId, page, size);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            log.error("查询用户分享列表失败: userId={}, error={}", userId, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 取消分享
     * DELETE /api/route-share/cancel/{id}
     */
    @DeleteMapping("/cancel/{id}")
    public Result<Boolean> cancelShare(@PathVariable Long id) {
        try {
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            log.info("取消分享请求: id={}", id);
            boolean result = routeShareService.cancelShare(id.intValue(), userId);
            return Result.success("取消分享成功", result);
        } catch (Exception e) {
            log.error("取消分享失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 更新分享设置
     * PUT /api/route-share/update/{id}
     */
    @PutMapping("/update/{id}")
    public Result<Boolean> updateShareSettings(@PathVariable Long id, @RequestBody Map<String, Object> settings) {
        try {
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            log.info("更新分享设置请求: id={}", id);
            boolean result = routeShareService.updateShareSettings(id, userId, settings);
            return Result.success("更新设置成功", result);
        } catch (Exception e) {
            log.error("更新分享设置失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
        }
    }

    /**
     * 获取分享统计信息
     * GET /api/route-share/statistics/{id}
     */
    @GetMapping("/statistics/{id}")
    public Result<ShareStatisticsResponse> getShareStatistics(@PathVariable Long id) {
        try {
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            log.info("获取分享统计信息请求: id={}", id);
            ShareStatisticsResponse statistics = routeShareService.getShareStatistics(id, userId);
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取分享统计信息失败: id={}, error={}", id, e.getMessage());
            throw ExceptionPropagation.propagate(e);
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
            throw ExceptionPropagation.propagate(e);
        }
    }

    // ==================== 文件分享相关接口 ====================

    @PostMapping("/file/generate")
    public Result<RouteShare> generateFileShareCode(@RequestBody Map<String, Object> request) {
        try {
            Integer fileId = (Integer) request.get("fileId");
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();

            log.info("生成文件分享码请求: fileId={}", fileId);

            RouteShare share = new RouteShare();
            share.setItemId(fileId);
            share.setItemType("file");
            share.setUserId(userId);

            RouteShare result = routeShareService.generateShareCode(share);
            return Result.success("生成分享码成功", result);
        } catch (Exception e) {
            log.error("生成文件分享码失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @GetMapping("/file/access/{shareCode}")
    public Result<String> accessShareFile(@PathVariable String shareCode,
                                          @RequestParam(required = false) String password) {
        try {
            log.info("访问分享文件请求: shareCode={}", shareCode);
            String accessUrl = routeShareService.accessShareFile(shareCode, password);
            return Result.success("访问成功", accessUrl);
        } catch (Exception e) {
            log.error("访问分享文件失败: shareCode={}, error={}", shareCode, e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }

    @PostMapping("/batch-cancel")
    public Result<Integer> batchCancelShares(@RequestBody List<Long> ids) {
        try {
            Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
            log.info("批量取消分享请求: count={}", ids == null ? 0 : ids.size());
            int count = routeShareService.batchCancelShares(ids, userId);
            return Result.success("批量取消成功", count);
        } catch (Exception e) {
            log.error("批量取消分享失败: error={}", e.getMessage());
            throw ExceptionPropagation.propagate(e);
        }
    }
}
