package travel.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import travel.common.entity.route_planning.Route;
import travel.common.utils.Result;
import travel.collection.feign.RouteFeignClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * 批量查询路线详情，供收藏列表等批量读场景使用。
     * 远程调用失败时返回空列表，由上层保留基础收藏数据并完成降级。
     */
    public List<Route> getByIds(List<Integer> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return List.of();
        }

        List<Integer> normalizedIds = routeIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        try {
            Result<List<Route>> result = routeFeignClient.getBatchRoutes(normalizedIds);
            return result != null && result.isSuccess() && result.getData() != null
                    ? result.getData()
                    : List.of();
        } catch (Exception e) {
            log.error("Feign批量查询路线失败: routeIds={}, error={}", normalizedIds, e.getMessage());
            return List.of();
        }
    }
}
