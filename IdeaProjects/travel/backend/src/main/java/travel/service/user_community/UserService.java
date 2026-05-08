package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 发送验证码
     */
    String sendCaptcha(String phone);

    /**
     * 注册
     */
    User register(User user, String captcha, Boolean agreement);

    /**
     * 登录
     */
    String login(String username, String password);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 获取当前登录用户
     */
    User getCurrentUser();

    /**
     * 更新个人信息
     */
    User updateProfile(User user);

    /**
     * 修改密码
     */
    boolean changePassword(String oldPassword, String newPassword);

    /**
     * 忘记密码（通过验证码重置）
     */
    boolean resetPassword(String phone, String captcha, String newPassword);

    /**
     * 登出
     */
    void logout();

    /**
     * 刷新token
     */
    String refreshToken(String oldToken);
}
