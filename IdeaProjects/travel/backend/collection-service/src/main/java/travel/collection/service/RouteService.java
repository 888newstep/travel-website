package travel.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.utils.Result;
import travel.collection.feign.RouteFeignClient;

/**
 * 路线服务（Feign远程调用封装）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteFeignClient routeFeignClient;

    public Route getById(Long id) {
        try {
            Result<Route> result = routeFeignClient.getById(id != null ? id.intValue() : null);
            return result != null && result.isSuccess() ? result.getData() : null;
        } catch (Exception e) {
            log.error("Feign调用RouteService.getById失败: id={}, error={}", id, e.getMessage());
            return null;
        }
    }
}