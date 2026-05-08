package travel.controller.user_community_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.Feedback;
import travel.entity.user_community.Notification;
import travel.entity.user_community.User;
import travel.service.impl.user_community.NotificationServiceImpl;
import travel.service.user_community.FeedbackService;
import travel.service.user_community.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import travel.utils.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final NotificationServiceImpl notificationService;
    private final FeedbackService feedbackService;

    @PostMapping("/register")
    public Result<User> register(@RequestBody @Valid RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword());
        User registered = userService.register(user, request.getCaptcha(), request.getAgreement());
        return Result.success("注册成功", registered);
    }

    /*@PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return response;
    }*/
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody @Valid LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return Result.success("登录成功", response);
    }

    @PostMapping("/captcha")
    public Result<Boolean> sendCaptcha(@RequestParam @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误") String phone) {
        String captcha = userService.sendCaptcha(phone);
        return Result.success("验证码发送成功", captcha != null && !captcha.isEmpty());
    }

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Integer id) {
        User user = userService.getById(id);
        return Result.success("获取用户信息成功", user);
    }

    @PutMapping
    public Result<Boolean> updateUser(@RequestBody @Valid User user) {
        boolean result = userService.updateById(user);
        return Result.success("更新用户信息成功", result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(@PathVariable Integer id) {
        boolean result = userService.removeById(id);
        return Result.success("删除用户成功", result);
    }

    // 新增接口
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        return Result.success("获取当前用户成功", user);
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody @Valid User user) {
        User updated = userService.updateProfile(user);
        return Result.success("更新资料成功", updated);
    }

    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        boolean result = userService.changePassword(request.getOldPassword(), request.getNewPassword());
        return Result.success("修改密码成功", result);
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        boolean result = userService.resetPassword(request.getPhone(), request.getCaptcha(), request.getNewPassword());
        return Result.success("重置密码成功", result);
    }

    @PostMapping("/logout")
    public Result<Boolean> logout() {
        userService.logout();
        return Result.success("退出登录成功", true);
    }

    @PostMapping("/refresh-token")
    public Result<RefreshTokenResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        String newToken = userService.refreshToken(request.getOldToken());
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setToken(newToken);
        return Result.success("刷新Token成功", response);
    }

    // 通知相关接口
    @GetMapping("/notifications")
    public Result<List<Notification>> getNotifications(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer size) {
        User currentUser = userService.getCurrentUser();
        List<Notification> notifications = notificationService.getByUserId(currentUser.getId(), page, size);
        return Result.success("获取通知列表成功", notifications);
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Boolean> markNotificationAsRead(@PathVariable Integer id) {
        User currentUser = userService.getCurrentUser();
        notificationService.markAsRead(id, currentUser.getId());
        return Result.success("标记为已读成功", true);
    }

    @DeleteMapping("/notifications/{id}")
    public Result<Boolean> deleteNotification(@PathVariable Integer id) {
        User currentUser = userService.getCurrentUser();
        notificationService.deleteNotification(id, currentUser.getId());
        return Result.success("删除通知成功", true);
    }

    @GetMapping("/notifications/unread-count")
    public Result<Integer> getUnreadCount() {
        int count = notificationService.getUnreadCount();
        return Result.success("获取未读数量成功", count);
    }

    @PostMapping("/notifications/mark-all-read")
    public Result<Boolean> markAllAsRead() {
        notificationService.markAllAsRead();
        return Result.success("全部标记为已读成功", true);
    }

    @PostMapping("/feedback")
    public Result<Feedback> submitFeedback(@RequestBody @Valid FeedbackRequest request) {
        User currentUser = userService.getCurrentUser();

        Feedback feedback = new Feedback();
        feedback.setType(request.getType());
        feedback.setContent(request.getContent());
        feedback.setContactInfo(request.getContactInfo());

        Feedback result = feedbackService.submitFeedback(feedback);
        return Result.success("提交反馈成功", result);
    }

    @GetMapping("/feedback/list")
    public Result<List<Feedback>> getFeedbackList(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer size) {
        User currentUser = userService.getCurrentUser();
        List<Feedback> feedbackList = feedbackService.getCurrentUserFeedbacks(page, size);
        return Result.success("获取反馈列表成功", feedbackList);
    }

    // 请求和响应对象
    @lombok.Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
        private String username;
        
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
        private String phone;
        
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
        private String password;
        
        @NotBlank(message = "验证码不能为空")
        @Size(min = 6, max = 6, message = "验证码长度必须为6位")
        private String captcha;
        
        private Boolean agreement;
    }

    @lombok.Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @lombok.Data
    public static class CaptchaResponse {
        private String captcha;
    }

    @lombok.Data
    public static class LoginResponse {
        private String token;
    }

    @lombok.Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;
        
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度必须在6-20之间")
        private String newPassword;
    }

    @lombok.Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
        private String phone;
        
        @NotBlank(message = "验证码不能为空")
        @Size(min = 6, max = 6, message = "验证码长度必须为6位")
        private String captcha;
        
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度必须在6-20之间")
        private String newPassword;
    }

    @lombok.Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "旧token不能为空")
        private String oldToken;
    }

    @lombok.Data
    public static class RefreshTokenResponse {
        private String token;
    }

    @lombok.Data
    public static class NotificationResponse {
        private Integer id;
        private String type;
        private String title;
        private String content;
        private Boolean isRead;
        private String createTime;
    }

    @lombok.Data
    public static class FeedbackRequest {
        @NotBlank(message = "反馈类型不能为空")
        private String type;
        
        @NotBlank(message = "反馈内容不能为空")
        @Size(min = 10, max = 1000, message = "反馈内容长度必须在10-1000之间")
        private String content;
        
        private String contactInfo;
    }

    @lombok.Data
    public static class FeedbackResponse {
        private Integer id;
        private String type;
        private String content;
        private String contactInfo;
        private String status;
        private String createTime;
        private String replyContent;
        private String replyTime;
    }
}
