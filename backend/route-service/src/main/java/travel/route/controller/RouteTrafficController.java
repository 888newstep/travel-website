package travel.route.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travel.common.entity.route_planning.Route;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.security.AuthenticatedUserSupport;
import travel.common.utils.Result;
import travel.route.dto.route.RouteTrafficResponse;
import travel.route.service.RouteRealTimeAdjustmentService;
import travel.route.service.RouteService;

import java.util.Objects;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteTrafficController {

    private final RouteService routeService;
    private final RouteRealTimeAdjustmentService routeRealTimeAdjustmentService;

    @GetMapping("/{id}/traffic")
    public Result<RouteTrafficResponse> getRouteTraffic(@PathVariable Integer id) {
        requireReadableRoute(id);
        RouteTrafficResponse response = RouteTrafficResponse.from(
                id, routeRealTimeAdjustmentService.getRealTimeTrafficInfo(id.longValue()));
        return Result.success("Fetched route traffic successfully", response);
    }

    private void requireReadableRoute(Integer routeId) {
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        Integer currentUserId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        boolean publishedPublic = "PUBLISHED".equals(route.getStatus()) && Boolean.TRUE.equals(route.getIsPublic());
        if (!publishedPublic && !Objects.equals(route.getUserId(), currentUserId)) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }
    }
}
