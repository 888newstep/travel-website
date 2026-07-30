package travel.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.user_community.User;
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
            Result<User> result = userFeignClient.getById(id != null ? id.intValue() : null);
            return result != null && result.isSuccess() ? result.getData() : null;
        } catch (Exception e) {
            log.error("Feign调用UserService.getById失败: id={}, error={}", id, e.getMessage());
            return null;
        }
    }

    public User getCurrentUser() {
        try {
            Result<User> result = userFeignClient.getCurrentUser();
            return result != null && result.isSuccess() ? result.getData() : null;
        } catch (Exception e) {
            log.error("Feign调用UserService.getCurrentUser失败: error={}", e.getMessage());
            return null;
        }
    }
}