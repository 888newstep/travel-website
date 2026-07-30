package travel.collection.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import travel.common.entity.user_community.Notification;
import travel.collection.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "用户通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取当前用户的通知列表")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        List<Notification> notifications = notificationService.getCurrentUserNotifications(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", notifications);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Integer count = notificationService.getUnreadCount();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", count);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Integer id) {
        boolean success = notificationService.markAsRead(id, null);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "标记成功");
        } else {
            result.put("code", 500);
            result.put("message", "标记失败");
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已读")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        boolean success = notificationService.markAllAsRead();
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "标记成功");
        } else {
            result.put("code", 500);
            result.put("message", "标记失败");
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Integer id) {
        boolean success = notificationService.deleteNotification(id, null);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("message", "删除成功");
        } else {
            result.put("code", 500);
            result.put("message", "删除失败");
        }
        return ResponseEntity.ok(result);
    }
}
