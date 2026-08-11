package travel.route.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.route_planning.RouteAttraction;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.route.dto.route.SmartRouteItem;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligentRouteRecommendationServiceTest {

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
    private IntelligentRouteRecommendationService intelligentRouteRecommendationService;

    @BeforeEach
    void setUp() {
        intelligentRouteRecommendationService.init();
    }

    @Test
    void shouldReturnPopularRoutesOrderedByPopularity() {
        Route higher = buildRoute(1, 1, 3, true, 100, 20);
        Route lower = buildRoute(2, 1, 3, true, 60, 5);
        when(cacheUtil.get(anyString(), eq(List.class))).thenReturn(null);
        when(routeService.list()).thenReturn(List.of(lower, higher));
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(1L)).thenReturn(List.of(buildRelation(101), buildRelation(102)));
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(2L)).thenReturn(List.of(buildRelation(103)));
        when(attractionService.getById(101)).thenReturn(buildAttraction(101, "park"));
        when(attractionService.getById(102)).thenReturn(buildAttraction(102, "museum"));
        when(attractionService.getById(103)).thenReturn(buildAttraction(103, "food"));

        List<SmartRouteItem> result = intelligentRouteRecommendationService.getPopularRoutes(1, 3, 10);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRouteId());
        assertEquals(2, result.get(0).getAttractionCount());
        verify(cacheUtil).set(anyString(), eq(result), eq(2L), eq(java.util.concurrent.TimeUnit.HOURS));
    }

    @Test
    void shouldReturnSimilarRoutesOrderedBySimilarity() {
        Route target = buildRoute(1, 1, 3, true, 50, 10);
        Route similar = buildRoute(2, 1, 3, true, 80, 8);
        Route lessSimilar = buildRoute(3, 1, 2, true, 40, 3);
        when(cacheUtil.get(anyString(), eq(List.class))).thenReturn(null);
        when(routeService.getById(1)).thenReturn(target);
        when(routeService.list()).thenReturn(List.of(target, similar, lessSimilar));
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(1L)).thenReturn(List.of(buildRelation(11), buildRelation(12)));
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(2L)).thenReturn(List.of(buildRelation(11), buildRelation(12)));
        when(routeAttractionService.getByRouteIdOrderByDayAndVisit(3L)).thenReturn(List.of(buildRelation(11), buildRelation(13), buildRelation(14)));

        List<SmartRouteItem> result = intelligentRouteRecommendationService.getSimilarRoutes(1, 5);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getRouteId());
        assertEquals(1.0, result.get(0).getSimilarity());
        assertEquals(3, result.get(1).getRouteId());
    }

    @Test
    void shouldBuildSeasonalRoutesForThreePreferences() {
        when(cacheUtil.get(anyString(), eq(List.class))).thenReturn(null);
        when(attractionService.getByCityId(1)).thenReturn(List.of(
                buildAttraction(201, "公园 山水"),
                buildAttraction(202, "花园 自然")
        ));
        when(routeOptimizationService.planOptimalRoute(eq(List.of(201, 202)), eq(2), eq(BigDecimal.valueOf(1000)), anyString())).thenReturn(null);

        List<SmartRouteItem> result = intelligentRouteRecommendationService.getSeasonalRoutes(1, "spring", 2);

        assertEquals(3, result.size());
        assertEquals("spring", result.get(0).getSeason());
        assertEquals(2, result.get(0).getAttractionCount());
        assertNull(result.get(0).getRoute());
    }

    @Test
    void shouldBuildThemeRoutesForThreePreferences() {
        when(cacheUtil.get(anyString(), eq(List.class))).thenReturn(null);
        when(attractionService.getByCityId(1)).thenReturn(List.of(
                buildAttraction(301, "博物馆 历史"),
                buildAttraction(302, "古迹 文化")
        ));
        when(routeOptimizationService.planOptimalRoute(eq(List.of(301, 302)), eq(2), eq(BigDecimal.valueOf(1000)), anyString())).thenReturn(null);

        List<SmartRouteItem> result = intelligentRouteRecommendationService.getThemeRoutes("文化历史", 1, 2);

        assertEquals(3, result.size());
        assertEquals("文化历史", result.get(0).getTheme());
        assertEquals(2, result.get(0).getAttractionCount());
    }

    private Route buildRoute(Integer id, Integer cityId, int days, boolean isPublic, int views, int likes) {
        Route route = new Route();
        route.setId(id);
        route.setCityId(cityId);
        route.setDurationDays(days);
        route.setIsPublic(isPublic);
        route.setViewCount(views);
        route.setLikeCount(likes);
        route.setTitle("route-" + id);
        route.setDescription("description-" + id);
        return route;
    }

    private RouteAttraction buildRelation(Integer attractionId) {
        RouteAttraction relation = new RouteAttraction();
        relation.setAttractionId(attractionId);
        return relation;
    }

    private Attraction buildAttraction(Integer id, String description) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setDescription(description);
        return attraction;
    }
}