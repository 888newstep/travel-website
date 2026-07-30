package travel.collection.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import travel.common.entity.route_planning.Route;
import travel.common.utils.Result;

@FeignClient(name = "route-service", path = "/routes")
public interface RouteFeignClient {

    @GetMapping("/{id}")
    Result<Route> getById(@PathVariable Integer id);
}