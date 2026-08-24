package travel.route.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.route.dto.route.RouteScheduleItemRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 路线聚合服务，统一处理路线主表和路线日程的业务事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteLifecycleService {

    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String ARCHIVED = "ARCHIVED";

    private final RouteService routeService;
    private final RouteAttractionService routeAttractionService;
    private final AttractionService attractionService;

    @Transactional(rollbackFor = Exception.class)
    public Route copyRoute(Integer routeId, Integer userId) {
        Route source = requireReadableRoute(routeId, userId);
        List<RouteAttraction> sourceSchedule = routeAttractionService
                .getByRouteIdOrderByDayAndVisit(routeId.longValue());

        Route copy = new Route();
        copy.setTitle(source.getTitle() + " (副本)");
        copy.setDescription(source.getDescription());
        copy.setCityId(source.getCityId());
        copy.setDurationDays(source.getDurationDays());
        copy.setDifficulty(source.getDifficulty());
        copy.setCoverImage(source.getCoverImage());
        copy.setUserId(userId);
        copy.setIsPublic(false);
        copy.setStatus(DRAFT);
        copy.setViewCount(0);
        copy.setLikeCount(0);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());

        if (!routeService.save(copy)) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_CREATE_FAILED);
        }

        List<RouteAttraction> copiedSchedule = new ArrayList<>();
        for (RouteAttraction sourceItem : sourceSchedule) {
            RouteAttraction copyItem = new RouteAttraction();
            copyItem.setRouteId(copy.getId());
            copyItem.setAttractionId(sourceItem.getAttractionId());
            copyItem.setDayNumber(sourceItem.getDayNumber());
            copyItem.setVisitOrder(sourceItem.getVisitOrder());
            copyItem.setNotes(sourceItem.getNotes());
            copiedSchedule.add(copyItem);
        }
        routeAttractionService.replaceCompleteSchedule(copy.getId(), copiedSchedule);
        log.info("路线复制完成: sourceRouteId={}, copyRouteId={}, userId={}, scheduleSize={}",
                routeId, copy.getId(), userId, copiedSchedule.size());
        return copy;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<RouteAttraction> replaceSchedule(
            Integer routeId,
            Integer userId,
            List<RouteScheduleItemRequest> items) {
        Route route = requireOwner(routeId, userId);
        List<RouteAttraction> relations = toRelations(route, items);
        routeAttractionService.replaceCompleteSchedule(routeId, relations);
        return routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Route publish(Integer routeId, Integer userId) {
        Route route = requireOwner(routeId, userId);
        List<RouteAttraction> schedule = routeAttractionService
                .getByRouteIdOrderByDayAndVisitForUpdate(routeId.longValue());
        if (schedule.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_PUBLISH_FAILED.getCode(), "路线至少需要一个景点才能发布");
        }
        validateScheduleAttractions(route, schedule);
        route.setStatus(PUBLISHED);
        route.setIsPublic(true);
        route.setUpdatedAt(LocalDateTime.now());
        if (!routeService.updateById(route)) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_PUBLISH_FAILED);
        }
        return route;
    }

    @Transactional(rollbackFor = Exception.class)
    public Route archive(Integer routeId, Integer userId) {
        Route route = requireOwner(routeId, userId);
        route.setStatus(ARCHIVED);
        route.setIsPublic(false);
        route.setUpdatedAt(LocalDateTime.now());
        if (!routeService.updateById(route)) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_UPDATE_FAILED);
        }
        return route;
    }

    public List<RouteAttraction> getReadableSchedule(Integer routeId, Integer userId) {
        requireReadableRoute(routeId, userId);
        return routeAttractionService.getByRouteIdOrderByDayAndVisit(routeId.longValue());
    }

    private Route requireReadableRoute(Integer routeId, Integer userId) {
        if (routeId == null || routeId <= 0 || (userId != null && userId <= 0)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        if (!(PUBLISHED.equals(route.getStatus()) && Boolean.TRUE.equals(route.getIsPublic()))
                && !Objects.equals(route.getUserId(), userId)) {
            throw new BusinessException(ErrorCodeEnum.NO_PERMISSION);
        }
        return route;
    }

    private Route requireOwner(Integer routeId, Integer userId) {
        if (routeId == null || routeId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        routeService.checkRouteOwner(routeId.longValue(), userId.longValue());
        Route route = routeService.getById(routeId);
        if (route == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }
        return route;
    }

    private List<RouteAttraction> toRelations(Route route, List<RouteScheduleItemRequest> items) {
        if (items == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<RouteAttraction> relations = new ArrayList<>();
        for (RouteScheduleItemRequest item : items) {
            if (item == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            RouteAttraction relation = new RouteAttraction();
            relation.setRouteId(route.getId());
            relation.setAttractionId(item.getAttractionId());
            relation.setDayNumber(item.getDayNumber());
            relation.setVisitOrder(item.getVisitOrder());
            relation.setNotes(item.getNotes());
            relations.add(relation);
        }
        validateScheduleShape(route, relations);
        validateScheduleAttractions(route, relations);
        return relations;
    }

    private void validateScheduleAttractions(Route route, List<RouteAttraction> relations) {
        for (RouteAttraction relation : relations) {
            Attraction attraction = attractionService.getById(relation.getAttractionId());
            if (attraction == null || !Objects.equals(attraction.getCityId(), route.getCityId())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_VALUE_ERROR.getCode(),
                        "路线景点不存在或不属于当前城市");
            }
        }
    }

    private void validateScheduleShape(Route route, List<RouteAttraction> relations) {
        if (relations.size() > 100 || route.getDurationDays() == null || route.getDurationDays() <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_RANGE_ERROR);
        }
        Set<Integer> attractionIds = new HashSet<>();
        Set<String> positions = new HashSet<>();
        for (RouteAttraction relation : relations) {
            if (relation.getAttractionId() == null || relation.getAttractionId() <= 0
                    || relation.getDayNumber() == null || relation.getDayNumber() <= 0
                    || relation.getVisitOrder() == null || relation.getVisitOrder() <= 0
                    || relation.getDayNumber() > route.getDurationDays()) {
                throw new BusinessException(ErrorCodeEnum.PARAM_VALUE_ERROR.getCode(),
                        "路线日程超出天数范围");
            }
            if (!attractionIds.add(relation.getAttractionId())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_DUPLICATE_ERROR);
            }
            if (!positions.add(relation.getDayNumber() + ":" + relation.getVisitOrder())) {
                throw new BusinessException(ErrorCodeEnum.ROUTE_ATTR_ORDER_DUPLICATE);
            }
        }
    }
}
