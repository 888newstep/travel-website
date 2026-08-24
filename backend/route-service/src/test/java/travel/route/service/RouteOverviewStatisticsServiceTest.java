package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.repository.RouteRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteOverviewStatisticsServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteOverviewStatisticsService routeOverviewStatisticsService;

    @Test
    void shouldAggregateRouteStatisticsWithNullSafeFields() {
        when(routeRepository.findAll()).thenReturn(List.of(
                buildRoute(1, 3, 100, 10, true),
                buildRoute(2, null, null, 5, false),
                buildRoute(1, 5, 50, null, null)
        ));

        Map<String, Object> statistics = routeOverviewStatisticsService.getRouteStatistics();

        assertEquals(3, statistics.get("totalRoutes"));
        assertEquals(1L, statistics.get("publicRoutes"));
        assertEquals(1L, statistics.get("privateRoutes"));
        assertEquals(4.0, statistics.get("averageDays"));
        assertEquals(150, statistics.get("totalViews"));
        assertEquals(15, statistics.get("totalLikes"));
    }

    @Test
    void shouldReturnSortedCityStatistics() {
        when(routeRepository.findAll()).thenReturn(List.of(
                buildRoute(3, 2, 10, 1, true),
                buildRoute(1, 4, 20, 2, true),
                buildRoute(3, 1, 30, 3, false)
        ));

        List<Map<String, Object>> cityStats = routeOverviewStatisticsService.getRouteStatisticsByCity();

        assertEquals(2, cityStats.size());
        assertEquals(1, cityStats.get(0).get("cityId"));
        assertEquals(1, cityStats.get(0).get("routeCount"));
        assertEquals(3, cityStats.get(1).get("cityId"));
        assertEquals(2, cityStats.get(1).get("routeCount"));
    }

    @Test
    void shouldCalculateCompletionRate() {
        when(routeRepository.findAll()).thenReturn(List.of(
                buildRoute(1, 2, 0, 0, true),
                buildRoute(1, 3, 8, 1, false),
                buildRoute(2, 4, 12, 2, true)
        ));

        Map<String, Object> completionRate = routeOverviewStatisticsService.getRouteCompletionRate();

        assertEquals(3, completionRate.get("totalRoutes"));
        assertEquals(2, completionRate.get("completedRoutes"));
        assertEquals(2D / 3D, (Double) completionRate.get("rate"));
    }

    @Test
    void shouldReturnSortedDurationDistribution() {
        when(routeRepository.findAll()).thenReturn(List.of(
                buildRoute(1, 5, 10, 1, true),
                buildRoute(2, 3, 20, 2, false),
                buildRoute(3, 5, 30, 3, true),
                buildRoute(4, null, 40, 4, true)
        ));

        List<Map<String, Object>> distribution = routeOverviewStatisticsService.getRouteDurationDistribution();

        assertEquals(2, distribution.size());
        assertEquals(3, distribution.get(0).get("days"));
        assertEquals(1L, distribution.get(0).get("count"));
        assertEquals(5, distribution.get(1).get("days"));
        assertEquals(2L, distribution.get(1).get("count"));
    }

    @Test
    void shouldReturnFallbackResultWhenRepositoryThrows() {
        when(routeRepository.findAll()).thenThrow(new RuntimeException("db error"));

        Map<String, Object> statistics = routeOverviewStatisticsService.getRouteStatistics();
        Map<String, Object> completionRate = routeOverviewStatisticsService.getRouteCompletionRate();
        List<Map<String, Object>> cityStats = routeOverviewStatisticsService.getRouteStatisticsByCity();
        List<Map<String, Object>> distribution = routeOverviewStatisticsService.getRouteDurationDistribution();

        assertEquals(false, statistics.get("success"));
        assertTrue(((String) statistics.get("message")).contains("db error"));
        assertEquals(false, completionRate.get("success"));
        assertTrue(((String) completionRate.get("message")).contains("db error"));
        assertTrue(cityStats.isEmpty());
        assertTrue(distribution.isEmpty());
    }

    private Route buildRoute(Integer cityId, Integer durationDays, Integer viewCount, Integer likeCount, Boolean isPublic) {
        Route route = new Route();
        route.setCityId(cityId);
        route.setDurationDays(durationDays);
        route.setViewCount(viewCount);
        route.setLikeCount(likeCount);
        route.setIsPublic(isPublic);
        return route;
    }
}
