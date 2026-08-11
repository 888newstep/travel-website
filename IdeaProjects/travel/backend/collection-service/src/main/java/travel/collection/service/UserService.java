package travel.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import travel.common.entity.user_community.User;
import travel.common.utils.JwtHelper;
import travel.common.utils.Result;
import travel.collection.feign.UserFeignClient;

/**
 * 用户服务（Feign远程调用封装）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserFeignClient userFeignClient;

    public User getById(Long id) {
        try {
            String authorization = resolveAuthorizationHeader();
            Result<User> result = userFeignClient.getById(authorization, id != null ? id.intValue() : null);
            return result != null && result.isSuccess() ? result.getData() : null;
        } catch (Exception e) {
            log.error("Feign调用UserService.getById失败: id={}, error={}", id, e.getMessage());
            return null;
        }
    }

    public User getCurrentUser() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            if (request == null) {
                return null;
            }
            String authorization = request.getHeader("Authorization");
            if (authorization == null || authorization.isBlank()) {
                return null;
            }
            String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
            Long userId = JwtHelper.getUserId(token);
            if (userId == null) {
                return null;
            }
            User user = getById(userId);
            if (user != null) {
                return user;
            }
            User fallbackUser = new User();
            fallbackUser.setId(userId.intValue());
            return fallbackUser;
        } catch (Exception e) {
            log.error("Feign调用UserService.getCurrentUser失败: error={}", e.getMessage());
            return null;
        }
    }

    private String resolveAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader("Authorization");
    }
}
