package travel.collection.service;

import org.junit.jupiter.api.Test;
import travel.collection.feign.RouteFeignClient;
import travel.common.entity.route_planning.Route;
import travel.common.performance.PerformanceStageRecorder;
import travel.common.utils.Result;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteServiceTest {

    @Test
    void shouldUseSideEffectFreeReadableEndpoint() {
        RouteFeignClient routeFeignClient = mock(RouteFeignClient.class);
        RouteService routeService = new RouteService(routeFeignClient, PerformanceStageRecorder.disabled());
        Route route = new Route();
        route.setId(8);
        when(routeFeignClient.getReadableById(8)).thenReturn(Result.success(route));

        Route result = routeService.getById(8L);

        assertSame(route, result);
        verify(routeFeignClient).getReadableById(8);
    }
}
