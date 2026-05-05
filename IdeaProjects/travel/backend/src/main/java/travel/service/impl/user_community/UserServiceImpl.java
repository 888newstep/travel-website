package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.user_community_mapper.UserMapper;
import travel.service.user_community.UserService;
import travel.utils.CacheUtil;
import travel.utils.JwtHelper;
import travel.utils.PasswordEncoderUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final CacheUtil cacheUtil;

    @Override
    public String sendCaptcha(String phone) {
        // 生成6位随机验证码
        String captcha = generateRandomCaptcha();
        
        // 缓存验证码，5分钟过期
        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        cacheUtil.set(cacheKey, captcha, 5, TimeUnit.MINUTES);
        
        // 实际项目中应该调用短信服务发送验证码
        log.info("向手机号 {} 发送验证码: {}", phone, captcha);
        
        return captcha;
    }

    /**
     * 生成随机验证码
     */
    private String generateRandomCaptcha() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 验证验证码
     */
    private boolean validateCaptcha(String phone, String captcha) {
        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        String cachedCaptcha = cacheUtil.get(cacheKey, String.class);
        return captcha != null && captcha.equals(cachedCaptcha);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user, String captcha, Boolean agreement) {
        // 1. 验证用户协议
        if (agreement == null || !agreement) {
            throw new BusinessException(ErrorCodeEnum.AGREEMENT_ERROR);
        }

        // 2. 验证手机号
        if (existsByPhone(user.getPhone())) {
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        // 3. 验证用户名
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        // 4. 验证验证码
        if (!validateCaptcha(user.getPhone(), captcha)) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }

        // 5. 加密密码
        user.setPassword(PasswordEncoderUtil.encode(user.getPassword()));

        // 6. 保存用户
        save(user);

        // 7. 清除验证码缓存
        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, user.getPhone());
        cacheUtil.delete(cacheKey);

        return user;
    }

    @Override
    public String login(String username, String password) {
        // 1. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .or()
                .eq(User::getPhone, username);

        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 2. 验证密码
        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 3. 生成JWT令牌
        String token = JwtHelper.createToken(user.getId().longValue(), 1); // 默认用户类型为1

        return token;
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return count(queryWrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        return count(queryWrapper) > 0;
    }

    @Override
    public User getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        // 从token中获取用户ID
        Long userId = JwtHelper.getUserId(token);
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        User user = getById(userId.intValue());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(User user) {
        User currentUser = getCurrentUser();
        user.setId(currentUser.getId());
        user.setPassword(currentUser.getPassword()); // 不更新密码

        updateById(user);
        return getById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(String oldPassword, String newPassword) {
        User currentUser = getCurrentUser();

        // 验证旧密码
        if (!PasswordEncoderUtil.matches(oldPassword, currentUser.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 加密新密码
        currentUser.setPassword(PasswordEncoderUtil.encode(newPassword));
        return updateById(currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String phone, String captcha, String newPassword) {
        // 1. 验证手机号是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 2. 验证验证码
        if (!validateCaptcha(phone, captcha)) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }

        // 3. 加密新密码
        user.setPassword(PasswordEncoderUtil.encode(newPassword));
        boolean result = updateById(user);

        // 4. 清除验证码缓存
        if (result) {
            String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
            cacheUtil.delete(cacheKey);
        }

        return result;
    }

    @Override
    public void logout() {
        // 1. 获取当前请求的token
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("Authorization");
        
        // 2. 如果token存在，将其加入黑名单
        if (token != null && !token.isEmpty()) {
            // 从token中获取过期时间
            Long expiration = JwtHelper.getExpiration(token);
            if (expiration != null) {
                // 计算剩余过期时间
                long remainingTime = expiration - System.currentTimeMillis();
                if (remainingTime > 0) {
                    // 将token加入黑名单，设置过期时间为剩余时间
                    String blacklistKey = "blacklist:token:" + token;
                    cacheUtil.set(blacklistKey, "1", remainingTime, TimeUnit.MILLISECONDS);
                    log.info("Token已加入黑名单: {}", token);
                }
            }
        }
        
        // 3. 使session失效
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public String refreshToken(String oldToken) {
        Long userId = JwtHelper.getUserId(oldToken);
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        return JwtHelper.createToken(userId, 1);
    }
}
