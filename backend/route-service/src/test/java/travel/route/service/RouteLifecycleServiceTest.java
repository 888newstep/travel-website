package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.exception.BusinessException;
import travel.route.dto.route.RouteScheduleItemRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteLifecycleServiceTest {

    @Mock
    private RouteService routeService;

    @Mock
    private RouteAttractionService routeAttractionService;

    @Mock
    private AttractionService attractionService;

    @Test
    void shouldRejectAttractionFromAnotherCity() {
        Route route = route(10, 1, 2, 42);
        Attraction attraction = new Attraction();
        attraction.setId(100);
        attraction.setCityId(2);
        when(routeService.getById(10)).thenReturn(route);
        when(attractionService.getById(100)).thenReturn(attraction);

        RouteLifecycleService service = new RouteLifecycleService(
                routeService, routeAttractionService, attractionService);

        RouteScheduleItemRequest item = item(100, 1, 1);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.replaceSchedule(10, 42, List.of(item)));

        assertEquals(4006, exception.getCode());
    }

    @Test
    void shouldRejectPublishingEmptySchedule() {
        Route route = route(10, 1, 1, 42);
        when(routeService.getById(10)).thenReturn(route);
        doReturn(List.of()).when(routeAttractionService)
                .getByRouteIdOrderByDayAndVisitForUpdate(10L);

        RouteLifecycleService service = new RouteLifecycleService(
                routeService, routeAttractionService, attractionService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.publish(10, 42));

        assertEquals(2008, exception.getCode());
    }

    @Test
    void shouldCopyAllScheduleItemsAsNewRelations() {
        Route source = route(10, 1, 2, 7);
        RouteAttraction first = relation(1, 10, 100, 1, 1);
        RouteAttraction second = relation(2, 10, 101, 2, 1);
        when(routeService.getById(10)).thenReturn(source);
        when(routeService.save(any(Route.class))).thenAnswer(invocation -> {
            Route copy = invocation.getArgument(0);
            copy.setId(20);
            return true;
        });
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(10L))
                .thenReturn(List.of(first, second));
        when(routeAttractionService.replaceCompleteSchedule(eq(20), any())).thenReturn(true);

        RouteLifecycleService service = new RouteLifecycleService(
                routeService, routeAttractionService, attractionService);

        Route copy = service.copyRoute(10, 42);

        assertEquals(20, copy.getId());
        verify(routeAttractionService).replaceCompleteSchedule(eq(20), any());
    }

    @Test
    void shouldReplaceScheduleWithMultipleNewRelations() {
        Route route = route(10, 1, 1, 42);
        Attraction firstAttraction = new Attraction();
        firstAttraction.setId(100);
        firstAttraction.setCityId(1);
        Attraction secondAttraction = new Attraction();
        secondAttraction.setId(101);
        secondAttraction.setCityId(1);
        when(routeService.getById(10)).thenReturn(route);
        when(attractionService.getById(100)).thenReturn(firstAttraction);
        when(attractionService.getById(101)).thenReturn(secondAttraction);
        when(routeAttractionService.replaceCompleteSchedule(eq(10), any())).thenReturn(true);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(10L)).thenReturn(List.of());

        RouteLifecycleService service = new RouteLifecycleService(
                routeService, routeAttractionService, attractionService);

        service.replaceSchedule(10, 42, List.of(item(100, 1, 1), item(101, 1, 2)));

        verify(routeAttractionService).replaceCompleteSchedule(eq(10), any());
    }

    private Route route(Integer id, Integer cityId, Integer days, Integer userId) {
        Route route = new Route();
        route.setId(id);
        route.setCityId(cityId);
        route.setDurationDays(days);
        route.setUserId(userId);
        route.setIsPublic(true);
        route.setStatus("PUBLISHED");
        return route;
    }

    private RouteAttraction relation(Integer id, Integer routeId, Integer attractionId,
                                     Integer dayNumber, Integer visitOrder) {
        RouteAttraction relation = new RouteAttraction();
        relation.setId(id);
        relation.setRouteId(routeId);
        relation.setAttractionId(attractionId);
        relation.setDayNumber(dayNumber);
        relation.setVisitOrder(visitOrder);
        return relation;
    }

    private RouteScheduleItemRequest item(Integer attractionId, Integer dayNumber, Integer visitOrder) {
        RouteScheduleItemRequest item = new RouteScheduleItemRequest();
        item.setAttractionId(attractionId);
        item.setDayNumber(dayNumber);
        item.setVisitOrder(visitOrder);
        return item;
    }
}
