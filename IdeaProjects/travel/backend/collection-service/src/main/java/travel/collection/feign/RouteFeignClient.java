package travel.collection.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import travel.common.entity.route_planning.Route;
import travel.common.utils.Result;

import java.util.List;

@FeignClient(name = "route-service", path = "/api/routes")
public interface RouteFeignClient {

    @GetMapping("/{id}")
    Result<Route> getById(@PathVariable Integer id);

    /**
     * 批量获取路线，避免收藏列表逐条跨服务调用。
     */
    @PostMapping("/batch")
    Result<List<Route>> getBatchRoutes(@RequestBody List<Integer> routeIds);
}
