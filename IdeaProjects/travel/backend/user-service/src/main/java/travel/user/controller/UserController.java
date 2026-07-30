package travel.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.User;
import travel.user.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import travel.common.utils.Result;
import travel.common.service.MessageProducerService;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final MessageProducerService messageProducerService;

    @PostMapping("/register")
    public Result<User> register(@RequestBody @Valid RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword());
        User registered = userService.register(user, request.getCaptcha(), request.getAgreement());
        return Result.success("注册成功", registered);
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody @Valid LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername())
                .or()
                .eq(User::getPhone, request.getUsername());
        User user = userService.getOne(queryWrapper);

        if (user != null) {
            messageProducerService.sendNotification(
                    user.getId(),
                    "system",
                    "登录提醒",
                    "您的账号于 " + java.time.LocalDateTime.now() + " 登录"
            );
        }

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
}
