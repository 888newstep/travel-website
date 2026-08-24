package travel.route.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.security.AuthenticatedUserSupport;
import travel.common.utils.Result;
import travel.route.dto.route.ReplaceRouteScheduleRequest;
import travel.route.service.RouteLifecycleService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteScheduleController {

    private final RouteLifecycleService routeLifecycleService;

    @GetMapping("/{routeId}/schedule")
    public Result<List<RouteAttraction>> getSchedule(@PathVariable Integer routeId) {
        Integer userId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        return Result.success("获取路线日程成功",
                routeLifecycleService.getReadableSchedule(routeId, userId));
    }

    @PutMapping("/{routeId}/schedule")
    public Result<List<RouteAttraction>> replaceSchedule(
            @PathVariable Integer routeId,
            @Valid @RequestBody ReplaceRouteScheduleRequest request) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        log.info("替换路线日程: routeId={}, userId={}, itemCount={}",
                routeId, userId, request.getItems().size());
        return Result.success("替换路线日程成功",
                routeLifecycleService.replaceSchedule(routeId, userId, request.getItems()));
    }

    @PostMapping("/{routeId}/publish")
    public Result<?> publish(@PathVariable Integer routeId) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        return Result.success("发布路线成功", routeLifecycleService.publish(routeId, userId));
    }

    @PostMapping("/{routeId}/archive")
    public Result<?> archive(@PathVariable Integer routeId) {
        Integer userId = AuthenticatedUserSupport.requireIntegerUserId();
        return Result.success("归档路线成功", routeLifecycleService.archive(routeId, userId));
    }
}
