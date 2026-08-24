package travel.collection.feign;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FeignClientPathTest {

    @Test
    void shouldIncludeServiceContextPathInFeignClients() {
        FeignClient routeClient = RouteFeignClient.class.getAnnotation(FeignClient.class);
        FeignClient userClient = UserFeignClient.class.getAnnotation(FeignClient.class);

        assertAll(
                () -> assertNotNull(routeClient),
                () -> assertNotNull(userClient),
                () -> assertEquals("/api/routes", routeClient.path()),
                () -> assertEquals("/api/users", userClient.path()));
    }
}
