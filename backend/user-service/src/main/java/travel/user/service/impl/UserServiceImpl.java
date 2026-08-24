package travel.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.User;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.user_community_mapper.UserMapper;
import travel.user.service.UserService;
import travel.common.utils.CacheUtil;
import travel.common.utils.JwtHelper;
import travel.common.service.MessageProducerService;
import travel.common.utils.PasswordEncoderUtil;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.dao.DuplicateKeyException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long CAPTCHA_COOLDOWN_MILLIS = 60_000;

    private final CacheUtil cacheUtil;

    /** Outbox 未启用时允许为空，保持轻量部署和单元测试兼容。 */
    @Autowired(required = false)
    private MessageProducerService messageProducerService;

    @Override
    public String sendCaptcha(String phone) {
        String cooldownKey = CacheUtil.generateKey("captcha_cooldown", phone);
        String requestId = UUID.randomUUID().toString();
        // 冷却窗口使用 Redis 原子抢占，防止多实例下重复发送。
        if (!cacheUtil.tryLock(cooldownKey, requestId, CAPTCHA_COOLDOWN_MILLIS)) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }

        String captcha = generateRandomCaptcha();
        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        cacheUtil.set(cacheKey, captcha, 5, TimeUnit.MINUTES);
        String storedCaptcha = cacheUtil.get(cacheKey, String.class);
        if (!captcha.equals(storedCaptcha)) {
            cacheUtil.releaseLock(cooldownKey, requestId);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_REDIS_ERROR);
        }

        log.info("验证码已生成并写入缓存: phone={}", maskPhone(phone));

        return captcha;
    }

    private String generateRandomCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private void consumeCaptcha(String phone, String captcha) {
        if (captcha == null || captcha.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }
        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        try {
            if (!cacheUtil.consumeIfEquals(cacheKey, captcha)) {
                throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("验证码原子消费失败: phone={}", maskPhone(phone), exception);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_REDIS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user, String captcha, Boolean agreement) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
                || user.getPhone() == null || user.getPhone().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        if (agreement == null || !agreement) {
            throw new BusinessException(ErrorCodeEnum.AGREEMENT_ERROR);
        }

        if (existsByPhone(user.getPhone())) {
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        consumeCaptcha(user.getPhone(), captcha);

        user.setPassword(PasswordEncoderUtil.encode(user.getPassword()));
        user.setUserType(1);

        try {
            save(user);
        } catch (DuplicateKeyException exception) {
            log.info("注册发生唯一键竞争: username={}, phone={}", user.getUsername(), maskPhone(user.getPhone()));
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        if (messageProducerService != null) {
            messageProducerService.enqueueNotification(
                    user.getId(),
                    "system",
                    "欢迎加入行旅",
                    "账号注册成功，开始规划你的下一段旅程吧");
            log.info("注册欢迎通知已提交: userId={}", user.getId());
        }

        return user;
    }

    @Override
    public String login(String username, String password) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .or()
                .eq(User::getPhone, username);

        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        Integer userType = user.getUserType() == null ? 1 : user.getUserType();
        String token = JwtHelper.createToken(user.getId().longValue(), userType);

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

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

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
        user.setPassword(currentUser.getPassword());
        user.setUserType(currentUser.getUserType());
        user.setCreatedAt(currentUser.getCreatedAt());
        user.setUpdatedAt(java.time.LocalDateTime.now());

        updateById(user);
        return getById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(String oldPassword, String newPassword) {
        User currentUser = getCurrentUser();

        if (!PasswordEncoderUtil.matches(oldPassword, currentUser.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        currentUser.setPassword(PasswordEncoderUtil.encode(newPassword));
        return updateById(currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String phone, String captcha, String newPassword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);

        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        consumeCaptcha(phone, captcha);
        user.setPassword(PasswordEncoderUtil.encode(newPassword));
        updateById(user);

        return true;
    }

    @Override
    public void logout() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (!JwtHelper.isExpiration(token)) {
                    // 将 token 加入 Redis 黑名单，TTL = token 剩余有效期 + 5分钟缓冲
                    Long expMillis = JwtHelper.getExpiration(token);
                    if (expMillis == null) return;
                    long remainingTtl = expMillis - System.currentTimeMillis();
                    if (remainingTtl > 0) {
                        String blacklistKey = "blacklist:token:" + token;
                        cacheUtil.set(blacklistKey, "1", remainingTtl + 300000, TimeUnit.MILLISECONDS);
                        log.info("用户登出，token已加入黑名单");
                    }
                }
            }
        }
        log.info("用户登出完成");
    }

    @Override
    public String refreshToken(String oldToken) {
        if (oldToken == null || oldToken.isBlank()
                || "1".equals(cacheUtil.get("blacklist:token:" + oldToken, String.class))) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        Long userId = JwtHelper.getUserId(oldToken);
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        User user = getById(userId.intValue());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }
        Integer userType = user.getUserType() == null ? 1 : user.getUserType();
        return JwtHelper.createToken(userId, userType);
    }
}
