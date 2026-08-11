package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.route.dto.optimization.RouteQualityEvaluationResult;
import travel.route.dto.route.RouteComparisonResult;
import travel.route.dto.route.RouteOptimizationSuggestionResult;
import travel.route.dto.route.RouteQualityEvaluation;
import travel.route.dto.route.RouteQualityEvaluationRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligentRouteEvaluationServiceTest {

    @Mock
    private RouteService routeService;
    @Mock
    private AttractionService attractionService;
    @Mock
    private RouteAttractionService routeAttractionService;
    @Mock
    private RouteOptimizationService routeOptimizationService;
    @Mock
    private CacheUtil cacheUtil;

    @InjectMocks
    private IntelligentRouteEvaluationService intelligentRouteEvaluationService;

    @Test
    void shouldCompareRoutesAndPickBestRoute() {
        Route route1 = buildRoute(1, "route-1", 2, 100, 10);
        Route route2 = buildRoute(2, "route-2", 2, 60, 30);
        when(routeService.getById(1)).thenReturn(route1);
        when(routeService.getById(2)).thenReturn(route2);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(1L)).thenReturn(List.of(buildRelation(11), buildRelation(12)));
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(2L)).thenReturn(List.of(buildRelation(21)));
        when(attractionService.getById(11)).thenReturn(buildAttraction(11, "文化", 4.0, 20, 120.0, 30.0));
        when(attractionService.getById(12)).thenReturn(buildAttraction(12, "自然", 5.0, 25, 121.0, 31.0));
        when(attractionService.getById(21)).thenReturn(buildAttraction(21, "美食", 4.5, 10, 122.0, 32.0));

        RouteComparisonResult result = intelligentRouteEvaluationService.compareRoutes(List.of(1, 2));

        assertEquals(2, result.getTotalRoutes());
        assertEquals(2, result.getBestRoute().getRouteId());
        assertEquals(2, result.getRoutes().get(0).getTotalAttractions());
    }

    @Test
    void shouldEvaluateRouteQualityWithCompositeScores() {
        Route route = buildRoute(1, "route-1", 2, 50, 5);
        RouteQualityEvaluationResult base = RouteQualityEvaluationResult.builder().qualityScore(0.8).build();
        when(routeOptimizationService.evaluateRouteQuality(1)).thenReturn(base);
        when(routeService.getById(1)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(1L)).thenReturn(List.of(buildRelation(11), buildRelation(12)));
        when(attractionService.getById(11)).thenReturn(buildAttraction(11, "文化 历史", 4.0, 30, 120.0, 30.0));
        when(attractionService.getById(12)).thenReturn(buildAttraction(12, "公园 自然", 5.0, 40, 121.0, 31.0));

        RouteQualityEvaluation result = intelligentRouteEvaluationService.evaluateRouteQuality(
                1, new RouteQualityEvaluationRequest());

        assertEquals(1, result.getRouteId());
        assertEquals(0.8, result.getQualityScore());
        assertTrue(result.getOverallScore() > 0.0);
        assertTrue(result.getDiversityScore() > 0.0);
    }

    @Test
    void shouldBuildCostOptimizationSuggestions() {
        Route route = buildRoute(1, "route-1", 2, 50, 5);
        when(cacheUtil.get(anyString(), eq(RouteOptimizationSuggestionResult.class))).thenReturn(null);
        when(routeService.getById(1)).thenReturn(route);
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(1L)).thenReturn(List.of(buildRelation(11), buildRelation(12), buildRelation(13)));
        when(attractionService.getById(11)).thenReturn(buildAttraction(11, "文化", 4.0, 80, 120.0, 30.0));
        when(attractionService.getById(12)).thenReturn(buildAttraction(12, "自然", 4.5, 90, 121.0, 31.0));
        when(attractionService.getById(13)).thenReturn(buildAttraction(13, "美食", 4.2, 20, 122.0, 32.0));

        RouteOptimizationSuggestionResult result = intelligentRouteEvaluationService.getRouteOptimizationSuggestions(1, "cost");

        assertEquals(1, result.getRouteId());
        assertEquals("cost", result.getOptimizationType());
        assertTrue(result.getSuggestions().size() >= 3);
        assertTrue(result.getSuggestions().stream().anyMatch(item -> item.contains("low-cost") || item.contains("public transport") || item.contains("discount")));
    }

    private Route buildRoute(Integer id, String title, Integer days, Integer views, Integer likes) {
        Route route = new Route();
        route.setId(id);
        route.setTitle(title);
        route.setDurationDays(days);
        route.setViewCount(views);
        route.setLikeCount(likes);
        return route;
    }

    private RouteAttraction buildRelation(Integer attractionId) {
        RouteAttraction relation = new RouteAttraction();
        relation.setAttractionId(attractionId);
        return relation;
    }

    private Attraction buildAttraction(Integer id, String description, Double rating, Integer ticketPrice, Double longitude, Double latitude) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setDescription(description);
        attraction.setRating(BigDecimal.valueOf(rating));
        attraction.setTicketPrice(BigDecimal.valueOf(ticketPrice));
        attraction.setLongitude(BigDecimal.valueOf(longitude));
        attraction.setLatitude(BigDecimal.valueOf(latitude));
        return attraction;
    }
}
