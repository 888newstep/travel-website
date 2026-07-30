package travel.collection.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import travel.common.entity.user_community.User;
import travel.common.utils.Result;

@FeignClient(name = "user-service", path = "/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<User> getById(@PathVariable Integer id);

    @GetMapping("/current")
    Result<User> getCurrentUser();
}