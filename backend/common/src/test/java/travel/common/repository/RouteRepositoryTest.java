package travel.common.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.mapper.route_planning_mapper.RouteMapper;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRepositoryTest {

    @Mock
    private RouteMapper routeMapper;

    @InjectMocks
    private RouteRepository routeRepository;

    @Test
    void shouldReturnSavedRouteWhenOneRowIsInserted() {
        Route route = new Route();
        when(routeMapper.insert(route)).thenReturn(1);

        assertSame(route, routeRepository.save(route));
    }

    @Test
    void shouldReturnNullWhenNoRowIsInserted() {
        Route route = new Route();
        when(routeMapper.insert(route)).thenReturn(0);

        assertNull(routeRepository.save(route));
    }
}
