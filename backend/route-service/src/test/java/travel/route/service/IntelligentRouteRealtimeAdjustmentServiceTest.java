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
import java.util.Map;

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
    @Mock
    private RouteRealTimeAdjustmentService routeRealTimeAdjustmentService;

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
        when(routeRealTimeAdjustmentService.getRealTimeTrafficInfo(1L)).thenReturn(Map.of(
                "dataAvailable", true,
                "segment:101-102", "heavy"));
        when(routeRealTimeAdjustmentService.getRealTimeAttractionStatus(List.of(101L, 102L)))
                .thenReturn(Map.of(
                        101L, Map.of("weather", "rainy", "crowdLevel", 2),
                        102L, Map.of("weather", "rainy", "crowdLevel", 4)));

        RealTimeAdjustmentResult result = intelligentRouteRealtimeAdjustmentService.getRealTimeAdjustment(
                1,
                request(new RealTimeLocation(31.2, 121.5), "sunny",
                        List.of("client-road"), List.of(999))
        );

        assertEquals(1, result.getRouteId());
        assertEquals("route-1", result.getRouteName());
        assertEquals(3, result.getAdjustments().size());
        assertEquals(2, result.getAlternativeAttractions().size());
        assertTrue(result.getAdjustments().get(0).contains("indoor"));
        assertTrue(result.getAdjustments().get(1).contains("segment:101-102"));
        assertTrue(result.getAdjustments().get(2).contains("B"));
        assertEquals("rainy", result.getRealTimeFactors().getWeather());
        assertEquals(List.of("segment:101-102"),
                result.getRealTimeFactors().getTraffic().getCongestedRoutes());
        assertEquals(List.of(102), result.getRealTimeFactors().getCrowd().getCrowdedAttractions());
    }

    @Test
    void shouldHandleMissingRealtimeFactorsSafely() {
        Route route = new Route();
        route.setId(2);
        route.setTitle("route-2");
        route.setCityId(20);
        when(routeService.getById(2)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(2L)).thenReturn(List.of());
        when(routeRealTimeAdjustmentService.getRealTimeTrafficInfo(2L)).thenReturn(Map.of(
                "dataAvailable", false,
                "source", "amap"));

        RealTimeAdjustmentResult result = intelligentRouteRealtimeAdjustmentService.getRealTimeAdjustment(2, new RealTimeAdjustmentRequest());

        assertEquals(2, result.getRouteId());
        assertTrue(result.getAdjustments().isEmpty());
        assertTrue(result.getAlternativeAttractions().isEmpty());
        assertEquals(null, result.getRealTimeFactors().getWeather());
        assertTrue(result.getRealTimeFactors().getTraffic().getCongestedRoutes().isEmpty());
        assertTrue(result.getRealTimeFactors().getCrowd().getCrowdedAttractions().isEmpty());
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
