package travel.route.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import travel.common.dto.route.RouteVisitDailyAggregate;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteVisit;
import travel.common.mapper.route_planning_mapper.RouteMapper;
import travel.common.mapper.route_planning_mapper.RouteVisitMapper;
import travel.route.dto.route.RouteVisitAnalyticsResponse;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteVisitAnalyticsServiceTest {

    @Mock
    private RouteMapper routeMapper;

    @Mock
    private RouteVisitMapper routeVisitMapper;

    @Mock
    private RouteCacheService routeCacheService;

    @Mock
    private HttpServletRequest request;

    private RouteVisitAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new RouteVisitAnalyticsService(routeMapper, routeVisitMapper, routeCacheService);
        ReflectionTestUtils.setField(service, "hashSalt", "route-analytics-test-secret");
    }

    @Test
    void shouldPersistHashedVisitAndIncrementCounter() {
        Route route = new Route();
        route.setId(7);
        when(routeMapper.incrementViewCount(7)).thenReturn(1);
        when(routeVisitMapper.insert(any(RouteVisit.class))).thenReturn(1);

        service.recordVisit(route, 42, request);

        ArgumentCaptor<RouteVisit> captor = ArgumentCaptor.forClass(RouteVisit.class);
        verify(routeVisitMapper).insert(captor.capture());
        RouteVisit visit = captor.getValue();
        assertEquals(7, visit.getRouteId());
        assertEquals(42, visit.getUserId());
        assertEquals("AUTHENTICATED", visit.getVisitorType());
        assertEquals(64, visit.getVisitorHash().length());
        assertFalse(visit.getVisitorHash().contains("42"));
        verify(routeMapper).incrementViewCount(7);
        verify(routeCacheService).invalidateRouteCache(7);
    }

    @Test
    void shouldKeepPersistedVisitWhenCacheInvalidationFails() {
        Route route = new Route();
        route.setId(7);
        when(routeMapper.incrementViewCount(7)).thenReturn(1);
        when(routeVisitMapper.insert(any(RouteVisit.class))).thenReturn(1);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(routeCacheService).invalidateRouteCache(7);

        service.recordVisit(route, 42, request);

        verify(routeMapper).incrementViewCount(7);
        verify(routeVisitMapper).insert(any(RouteVisit.class));
    }

    @Test
    void shouldBuildCompleteDailyTrendAndRetentionRate() {
        Route route = new Route();
        route.setId(7);
        route.setViewCount(99);
        when(routeMapper.selectById(7)).thenReturn(route);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(2);
        RouteVisitDailyAggregate aggregate = new RouteVisitDailyAggregate();
        aggregate.setVisitDate(startDate.plusDays(1));
        aggregate.setVisits(5L);
        aggregate.setUniqueVisitors(3L);
        when(routeVisitMapper.countVisits(7, startDate, endDate)).thenReturn(5L);
        when(routeVisitMapper.countUniqueVisitors(7, startDate, endDate)).thenReturn(4L);
        when(routeVisitMapper.countReturningVisitors(7, startDate, endDate)).thenReturn(1L);
        when(routeVisitMapper.selectDailyTrend(7, startDate, endDate)).thenReturn(List.of(aggregate));

        RouteVisitAnalyticsResponse response = service.getAnalytics(7, 3);

        assertEquals(99, response.totalViews());
        assertEquals(5, response.periodVisits());
        assertEquals(4, response.uniqueVisitors());
        assertEquals(1, response.returningVisitors());
        assertEquals(0.25, response.retentionRate());
        assertEquals(3, response.trend().size());
        assertEquals(0, response.trend().get(0).visits());
        assertEquals(5, response.trend().get(1).visits());
        assertEquals(0, response.trend().get(2).visits());
    }
}
