package travel.collection.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import travel.collection.service.NotificationService;
import travel.common.entity.user_community.Notification;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.utils.Result;

import java.util.List;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "用户通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取当前用户的通知列表")
    public Result<List<Notification>> getNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success("获取成功", notificationService.getCurrentUserNotifications(page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public Result<Integer> getUnreadCount() {
        return Result.success("获取成功", notificationService.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    public Result<Boolean> markAsRead(@PathVariable Integer id) {
        if (!notificationService.markAsRead(id, null)) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_UPDATE_FAILED);
        }
        return Result.success("标记成功", true);
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已读")
    public Result<Boolean> markAllAsRead() {
        if (!notificationService.markAllAsRead()) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_UPDATE_FAILED);
        }
        return Result.success("标记成功", true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public Result<Boolean> deleteNotification(@PathVariable Integer id) {
        if (!notificationService.deleteNotification(id, null)) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_DELETE_FAILED);
        }
        return Result.success("删除成功", true);
    }
}
