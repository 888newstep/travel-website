package travel.route.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import travel.common.dto.request.RouteAttractionBatchSortRequest;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.exception.BusinessException;
import travel.common.mapper.route_planning_mapper.RouteAttractionMapper;
import travel.route.service.impl.RouteAttractionServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteAttractionServiceImplTest {

    @Mock
    private RouteAttractionMapper routeAttractionMapper;

    @Mock
    private RouteService routeService;

    private RouteAttractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new RouteAttractionServiceImpl(routeAttractionMapper, routeService));
        ReflectionTestUtils.setField(service, "baseMapper", routeAttractionMapper);
    }

    @Test
    void shouldReserveAllOrdersBeforeReplacingSchedule() {
        List<RouteAttraction> schedule = List.of(
                relation(101, 10, 501, 1, 1),
                relation(102, 10, 502, 1, 2),
                relation(103, 10, 503, 2, 1));
        when(routeAttractionMapper.reserveVisitOrders(10)).thenReturn(3);
        doReturn(schedule).when(service).getByRouteIdOrderByDayAndVisitForUpdate(10L);
        doReturn(true).when(service).updateBatchById(anyList());

        assertTrue(service.replaceRouteSchedule(10, schedule));

        InOrder inOrder = inOrder(routeAttractionMapper, service);
        inOrder.verify(routeAttractionMapper).reserveVisitOrders(10);
        inOrder.verify(service).updateBatchById(schedule);
    }

    @Test
    void shouldRejectScheduleWhenReservedRowCountChanges() {
        List<RouteAttraction> schedule = List.of(
                relation(101, 10, 501, 1, 1),
                relation(102, 10, 502, 1, 2));
        when(routeAttractionMapper.reserveVisitOrders(10)).thenReturn(1);
        doReturn(schedule).when(service).getByRouteIdOrderByDayAndVisitForUpdate(10L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.replaceRouteSchedule(10, schedule));

        assertEquals(2006, exception.getCode());
        verify(service, never()).updateBatchById(anyList());
    }

    @Test
    void shouldRejectBatchSortRelationOutsideRequestedRoute() {
        Route route = new Route();
        route.setId(10);
        when(routeService.getById(10L)).thenReturn(route);
        doReturn(List.of(
                relation(101, 10, 501, 1, 1),
                relation(102, 10, 502, 1, 2)))
                .when(service).getByRouteIdOrderByDayAndVisitForUpdate(10L);

        RouteAttractionBatchSortRequest request = request(10L, item(999L, 1, 1));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.batchSortRouteAttractions(request));

        assertEquals(2004, exception.getCode());
        verify(routeAttractionMapper, never()).reserveVisitOrders(10);
    }

    @Test
    void shouldValidateDuplicatePositionAgainstCompleteSchedule() {
        Route route = new Route();
        route.setId(10);
        when(routeService.getById(10L)).thenReturn(route);
        doReturn(List.of(
                relation(101, 10, 501, 1, 1),
                relation(102, 10, 502, 1, 2)))
                .when(service).getByRouteIdOrderByDayAndVisitForUpdate(10L);

        RouteAttractionBatchSortRequest request = request(10L, item(101L, 1, 2));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.batchSortRouteAttractions(request));

        assertEquals(2003, exception.getCode());
        verify(routeAttractionMapper, never()).reserveVisitOrders(10);
    }

    @Test
    void shouldSkipDatabaseWriteWhenBatchSortHasNoChanges() {
        Route route = new Route();
        route.setId(10);
        when(routeService.getById(10L)).thenReturn(route);
        doReturn(List.of(
                relation(101, 10, 501, 1, 1),
                relation(102, 10, 502, 1, 2)))
                .when(service).getByRouteIdOrderByDayAndVisitForUpdate(10L);

        RouteAttractionBatchSortRequest request = request(
                10L,
                item(101L, 1, 1),
                item(102L, 1, 2));

        assertTrue(service.batchSortRouteAttractions(request));
        verify(routeAttractionMapper, never()).reserveVisitOrders(10);
    }

    @Test
    void shouldReplaceCompleteScheduleWithMultipleNewRelations() {
        List<RouteAttraction> schedule = List.of(
                relation(null, 10, 501, 1, 1),
                relation(null, 10, 502, 1, 2));
        when(routeAttractionMapper.delete(any())).thenReturn(0);
        doReturn(true).when(service).saveBatch(anyList());

        assertTrue(service.replaceCompleteSchedule(10, schedule));

        verify(service).saveBatch(schedule);
    }

    private RouteAttractionBatchSortRequest request(
            Long routeId, RouteAttractionBatchSortRequest.SortItem... items) {
        RouteAttractionBatchSortRequest request = new RouteAttractionBatchSortRequest();
        request.setRouteId(routeId);
        request.setSortItems(List.of(items));
        return request;
    }

    private RouteAttractionBatchSortRequest.SortItem item(
            Long relationId, Integer dayNumber, Integer visitOrder) {
        RouteAttractionBatchSortRequest.SortItem item = new RouteAttractionBatchSortRequest.SortItem();
        item.setRelationId(relationId);
        item.setDayNumber(dayNumber);
        item.setVisitOrder(visitOrder);
        return item;
    }

    private RouteAttraction relation(
            Integer id,
            Integer routeId,
            Integer attractionId,
            Integer dayNumber,
            Integer visitOrder) {
        RouteAttraction relation = new RouteAttraction();
        relation.setId(id);
        relation.setRouteId(routeId);
        relation.setAttractionId(attractionId);
        relation.setDayNumber(dayNumber);
        relation.setVisitOrder(visitOrder);
        return relation;
    }
}
