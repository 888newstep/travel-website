package travel.route.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.entity.route_planning.Route;
import travel.common.exception.BusinessException;
import travel.route.dto.route.RouteTrafficResponse;
import travel.route.service.RouteRealTimeAdjustmentService;
import travel.route.service.RouteService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RouteTrafficControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTypedTrafficForPublicRoute() {
        RouteService routeService = mock(RouteService.class);
        RouteRealTimeAdjustmentService trafficService = mock(RouteRealTimeAdjustmentService.class);
        Route route = new Route();
        route.setIsPublic(true);
        route.setStatus("PUBLISHED");
        when(routeService.getById(8)).thenReturn(route);
        when(trafficService.getRealTimeTrafficInfo(8L)).thenReturn(Map.of(
                "dataAvailable", true,
                "source", "amap",
                "totalDistanceMeters", 12000,
                "totalDurationSeconds", 1800,
                "routeDetails", List.of(Map.of(
                        "segmentId", "segment:1-2",
                        "fromAttractionId", 1,
                        "toAttractionId", 2,
                        "status", "moderate",
                        "distanceMeters", 12000,
                        "durationSeconds", 1800))));

        RouteTrafficResponse response = new RouteTrafficController(routeService, trafficService)
                .getRouteTraffic(8).getData();

        assertEquals(12000, response.totalDistanceMeters());
        assertEquals("moderate", response.segments().get(0).status());
    }

    @Test
    void shouldRejectUnreadablePrivateRouteBeforeAmapAggregation() {
        RouteService routeService = mock(RouteService.class);
        RouteRealTimeAdjustmentService trafficService = mock(RouteRealTimeAdjustmentService.class);
        Route route = new Route();
        route.setIsPublic(false);
        route.setUserId(99);
        when(routeService.getById(8)).thenReturn(route);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, List.of()));

        assertThrows(BusinessException.class,
                () -> new RouteTrafficController(routeService, trafficService).getRouteTraffic(8));
        verifyNoInteractions(trafficService);
    }

    @Test
    void shouldRejectPublicDraftOwnedByAnotherUser() {
        RouteService routeService = mock(RouteService.class);
        RouteRealTimeAdjustmentService trafficService = mock(RouteRealTimeAdjustmentService.class);
        Route route = new Route();
        route.setIsPublic(true);
        route.setStatus("DRAFT");
        route.setUserId(99);
        when(routeService.getById(8)).thenReturn(route);

        assertThrows(BusinessException.class,
                () -> new RouteTrafficController(routeService, trafficService).getRouteTraffic(8));
        verifyNoInteractions(trafficService);
    }
}
