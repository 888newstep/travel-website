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
import travel.common.utils.PasswordEncoderUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final CacheUtil cacheUtil;

    @Override
    public String sendCaptcha(String phone) {
        String captcha = generateRandomCaptcha();

        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        cacheUtil.set(cacheKey, captcha, 5, TimeUnit.MINUTES);

        log.info("向手机号 {} 发送验证码: {}", phone, captcha);

        return captcha;
    }

    private String generateRandomCaptcha() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private boolean validateCaptcha(String phone, String captcha) {
        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        String cachedCaptcha = cacheUtil.get(cacheKey, String.class);
        return captcha != null && captcha.equals(cachedCaptcha);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user, String captcha, Boolean agreement) {
        if (agreement == null || !agreement) {
            throw new BusinessException(ErrorCodeEnum.AGREEMENT_ERROR);
        }

        if (existsByPhone(user.getPhone())) {
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(ErrorCodeEnum.USER_EXIST);
        }

        if (!validateCaptcha(user.getPhone(), captcha)) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }

        user.setPassword(PasswordEncoderUtil.encode(user.getPassword()));

        save(user);

        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, user.getPhone());
        cacheUtil.delete(cacheKey);

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

        String token = JwtHelper.createToken(user.getId().longValue(), 1);

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
        if (!validateCaptcha(phone, captcha)) {
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);

        User user = getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        user.setPassword(PasswordEncoderUtil.encode(newPassword));
        updateById(user);

        String cacheKey = CacheUtil.generateKey(CacheUtil.CAPTCHA_KEY_PREFIX, phone);
        cacheUtil.delete(cacheKey);

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
        Long userId = JwtHelper.getUserId(oldToken);
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        return JwtHelper.createToken(userId, 1);
    }
}
