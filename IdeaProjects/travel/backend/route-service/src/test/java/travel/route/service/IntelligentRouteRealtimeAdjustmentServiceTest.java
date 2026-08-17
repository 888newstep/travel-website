package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.route.dto.route.RealTimeAdjustmentResult;
import travel.route.dto.route.RealTimeAdjustmentRequest;
import travel.route.dto.route.RealTimeCrowdFactors;
import travel.route.dto.route.RealTimeFactors;
import travel.route.dto.route.RealTimeLocation;
import travel.route.dto.route.RealTimeTrafficFactors;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligentRouteRealtimeAdjustmentServiceTest {

    @Mock
    private RouteService routeService;
    @Mock
    private AttractionService attractionService;
    @Mock
    private RouteAttractionService routeAttractionService;

    @InjectMocks
    private IntelligentRouteRealtimeAdjustmentService intelligentRouteRealtimeAdjustmentService;

    @Test
    void shouldBuildRealtimeAdjustmentsFromWeatherTrafficAndCrowd() {
        Route route = new Route();
        route.setId(1);
        route.setTitle("route-1");
        route.setCityId(10);
        when(routeService.getById(1)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(1L)).thenReturn(List.of(buildRelation(101), buildRelation(102)));
        when(attractionService.getById(101)).thenReturn(buildAttraction(101, "A", "室内文化", 10));
        when(attractionService.getById(102)).thenReturn(buildAttraction(102, "B", "自然风景", 10));
        when(attractionService.getByCityId(10)).thenReturn(List.of(
                buildAttraction(201, "Museum", "博物馆 历史", 10),
                buildAttraction(202, "Indoor", "室内 文化", 10)
        ));
        when(attractionService.getById(201)).thenReturn(buildAttraction(201, "Museum", "博物馆 历史", 10));

        RealTimeAdjustmentResult result = intelligentRouteRealtimeAdjustmentService.getRealTimeAdjustment(
                1,
                request(new RealTimeLocation(31.2, 121.5), "rainy",
                        List.of("Road-A", "Road-B"), List.of(201))
        );

        assertEquals(1, result.getRouteId());
        assertEquals("route-1", result.getRouteName());
        assertEquals(3, result.getAdjustments().size());
        assertEquals(2, result.getAlternativeAttractions().size());
        assertTrue(result.getAdjustments().get(0).contains("indoor"));
        assertTrue(result.getAdjustments().get(1).contains("Road-A"));
    }

    @Test
    void shouldHandleMissingRealtimeFactorsSafely() {
        Route route = new Route();
        route.setId(2);
        route.setTitle("route-2");
        route.setCityId(20);
        when(routeService.getById(2)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(2L)).thenReturn(List.of());

        RealTimeAdjustmentResult result = intelligentRouteRealtimeAdjustmentService.getRealTimeAdjustment(2, new RealTimeAdjustmentRequest());

        assertEquals(2, result.getRouteId());
        assertTrue(result.getAdjustments().isEmpty());
        assertTrue(result.getAlternativeAttractions().isEmpty());
        assertEquals(null, result.getRealTimeFactors().getWeather());
        assertEquals(null, result.getRealTimeFactors().getTraffic());
        assertEquals(null, result.getRealTimeFactors().getCrowd());
        assertTrue(result.getRealTimeFactors().getExtensions() == null
                || result.getRealTimeFactors().getExtensions().isEmpty());
    }

    private RouteAttraction buildRelation(Integer attractionId) {
        RouteAttraction relation = new RouteAttraction();
        relation.setAttractionId(attractionId);
        return relation;
    }

    private Attraction buildAttraction(Integer id, String name, String description, Integer cityId) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setName(name);
        attraction.setDescription(description);
        attraction.setCityId(cityId);
        return attraction;
    }

    private RealTimeAdjustmentRequest request(RealTimeLocation location, String weather,
                                              List<String> congestedRoutes,
                                              List<Integer> crowdedAttractions) {
        RealTimeTrafficFactors traffic = new RealTimeTrafficFactors();
        traffic.setCongestedRoutes(congestedRoutes);
        RealTimeCrowdFactors crowd = new RealTimeCrowdFactors();
        crowd.setCrowdedAttractions(crowdedAttractions);
        RealTimeFactors factors = new RealTimeFactors();
        factors.setWeather(weather);
        factors.setTraffic(traffic);
        factors.setCrowd(crowd);
        RealTimeAdjustmentRequest request = new RealTimeAdjustmentRequest();
        request.setCurrentLocation(location);
        request.setRealTimeFactors(factors);
        return request;
    }
}
