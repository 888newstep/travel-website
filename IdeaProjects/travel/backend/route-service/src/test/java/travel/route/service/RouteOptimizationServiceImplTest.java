package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.repository.RoutePlanRepository;
import travel.common.utils.AMapRouteService;
import travel.route.algorithm.GeneticAlgorithmTSP;
import travel.route.algorithm.RoutePlanAlgorithm;
import travel.route.dto.optimization.RouteAlternative;
import travel.route.dto.optimization.RouteAlternativeData;
import travel.route.dto.optimization.RouteCrowdPredictionItem;
import travel.route.dto.optimization.RouteRecommendationDayPlan;
import travel.route.dto.optimization.RouteRecommendationItem;
import travel.route.service.impl.RouteOptimizationServiceImpl;

import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationServiceImplTest {

    @Mock
    private RoutePlanAlgorithm routePlanAlgorithm;

    @Mock
    private RouteService routeService;

    @Mock
    private AttractionService attractionService;

    @Mock
    private RouteAttractionService routeAttractionService;

    @Mock
    private RoutePlanRepository routePlanRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private AMapRouteService aMapRouteService;

    @Mock
    private GeneticAlgorithmTSP geneticAlgorithmTSP;

    @InjectMocks
    private RouteOptimizationServiceImpl routeOptimizationService;

    @Test
    void shouldReturnTypedAlternativeDataWithStableMetrics() {
        Route route = new Route();
        route.setId(17);
        route.setDurationDays(3);

        RouteAttraction relation = new RouteAttraction();
        relation.setAttractionId(101);

        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();
        optimalRoute.setTotalFitness(0.91);
        optimalRoute.setTotalDistance(12.5);
        optimalRoute.setTotalCost(88.0);
        optimalRoute.setTotalTime(240.0);

        when(routeService.getById(17)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(17L)).thenReturn(List.of(relation));
        when(routePlanAlgorithm.planOptimalRoute(
                eq(List.of(101)), eq(3), eq(new BigDecimal("1000")), eq("balanced")))
                .thenReturn(optimalRoute);

        List<RouteAlternative> alternatives = routeOptimizationService.generateRouteAlternatives(17, 1);

        assertEquals(1, alternatives.size());
        RouteAlternative alternative = alternatives.get(0);
        assertEquals(17, alternative.getOriginalRouteId());

        RouteAlternativeData data = alternative.getRouteData();
        assertNotNull(data);
        assertEquals("balanced", data.getPreference());
        assertEquals(0.91, data.getFitness());
        assertEquals(12.5, data.getTotalDistance());
        assertEquals(88.0, data.getTotalCost());
        assertEquals(240.0, data.getTotalTime());
        verify(routePlanAlgorithm).planOptimalRoute(
                eq(List.of(101)), eq(3), eq(new BigDecimal("1000")), eq("balanced"));
    }

    @Test
    void shouldReturnTypedRecommendationDayPlans() {
        RoutePlanAlgorithm.RouteDayPlan sourceDayPlan = new RoutePlanAlgorithm.RouteDayPlan();
        sourceDayPlan.setDayNumber(2);
        sourceDayPlan.setAttractionIds(List.of(201, 202));
        sourceDayPlan.setDistance(6.5);
        sourceDayPlan.setCost(35.0);
        sourceDayPlan.setTime(180.0);

        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();
        optimalRoute.setTotalFitness(0.8);
        optimalRoute.getDayPlans().add(sourceDayPlan);

        when(attractionService.getByCityId(8)).thenReturn(List.of());
        when(routePlanAlgorithm.planOptimalRoute(
                eq(List.of()), eq(2), eq(new BigDecimal("500")), anyString()))
                .thenReturn(optimalRoute);

        List<RouteRecommendationItem> recommendations = routeOptimizationService.getRouteRecommendations(
                8, 2, List.of(), new BigDecimal("500"));

        assertEquals(4, recommendations.size());
        for (RouteRecommendationItem recommendation : recommendations) {
            assertEquals(1, recommendation.getDayPlans().size());
            RouteRecommendationDayPlan plan = recommendation.getDayPlans().get(0);
            assertEquals(2, plan.getDayNumber());
            assertEquals(List.of(201, 202), plan.getAttractionIds());
            assertEquals(6.5, plan.getDistance());
            assertEquals(35.0, plan.getCost());
            assertEquals(180.0, plan.getTime());
        }
    }

    @Test
    void shouldReturnTypedWeekendCrowdPredictions() {
        Route route = new Route();
        route.setId(21);
        route.setTitle("weekend-route");

        RouteAttraction relation = new RouteAttraction();
        relation.setAttractionId(301);

        travel.common.entity.travel_recommendation.Attraction attraction =
                new travel.common.entity.travel_recommendation.Attraction();
        attraction.setId(301);
        attraction.setName("景点 A");
        attraction.setViewCount(600000);

        when(routeService.getById(21)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(21L)).thenReturn(List.of(relation));
        when(attractionService.getById(301)).thenReturn(attraction);

        travel.route.dto.optimization.RouteCrowdPrediction result =
                routeOptimizationService.predictRouteCrowd(21, "2026-08-15");

        assertEquals(21, result.getRouteId());
        assertEquals("weekend-route", result.getRouteName());
        assertEquals("2026-08-15", result.getPredictDate());
        assertEquals(true, result.getIsWeekend());
        assertEquals(false, result.getIsHoliday());
        assertEquals(1, result.getCrowdPredictions().size());

        RouteCrowdPredictionItem item = result.getCrowdPredictions().get(0);
        assertEquals(301, item.getAttractionId());
        assertEquals("景点 A", item.getAttractionName());
        assertEquals(1200, item.getPredictedCrowd());
        assertEquals("\u62e5\u6324", item.getCrowdLevel());
        org.junit.jupiter.api.Assertions.assertTrue(item.getSuggestedTime().contains("9:00"));
    }

    @Test
    void shouldTreatMissingViewCountAsZeroInCrowdPrediction() {
        Route route = new Route();
        route.setId(22);
        RouteAttraction relation = new RouteAttraction();
        relation.setAttractionId(302);

        travel.common.entity.travel_recommendation.Attraction attraction =
                new travel.common.entity.travel_recommendation.Attraction();
        attraction.setId(302);
        attraction.setName("景点 B");
        attraction.setViewCount(null);

        when(routeService.getById(22)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(22L)).thenReturn(List.of(relation));
        when(attractionService.getById(302)).thenReturn(attraction);

        travel.route.dto.optimization.RouteCrowdPrediction result =
                routeOptimizationService.predictRouteCrowd(22, "2026-08-11");

        assertEquals(0, result.getCrowdPredictions().get(0).getPredictedCrowd());
        assertEquals("\u8f83\u5c11", result.getCrowdPredictions().get(0).getCrowdLevel());
    }
}
