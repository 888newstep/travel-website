package travel.route.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.route.algorithm.RoutePlanAlgorithm;
import travel.route.dto.route.MultiDayRouteResult;
import travel.route.dto.route.PersonalizedRouteConstraints;
import travel.route.dto.route.PersonalizedRoutePreferences;
import travel.route.dto.route.PersonalizedRouteResult;
import travel.route.dto.route.RouteRecommendationReason;
import travel.route.dto.route.UserPreferenceRecommendation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligentRoutePersonalizationServiceTest {

    @Mock
    private AttractionService attractionService;

    @Mock
    private RouteOptimizationService routeOptimizationService;

    @Mock
    private CacheUtil cacheUtil;

    @InjectMocks
    private IntelligentRoutePersonalizationService intelligentRoutePersonalizationService;

    @BeforeEach
    void setUp() {
        intelligentRoutePersonalizationService.init();
    }

    @Test
    void shouldBuildPreferenceRecommendationsFromFilteredAttractions() {
        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();
        when(cacheUtil.get(anyString(), eq(List.class))).thenReturn(null);
        when(attractionService.getByCityId(1)).thenReturn(List.of(
                buildAttraction(101, "\u535a\u7269\u9986 \u5386\u53f2"),
                buildAttraction(102, "\u591c\u666f \u516c\u56ed")
        ));
        when(routeOptimizationService.planOptimalRoute(eq(List.of(101)), eq(2), eq(new BigDecimal("1500")), eq("balanced"))).thenReturn(optimalRoute);
        when(routeOptimizationService.planOptimalRoute(eq(List.of(101)), eq(2), eq(new BigDecimal("1500")), eq("lowCost"))).thenReturn(optimalRoute);
        when(routeOptimizationService.planOptimalRoute(eq(List.of(101)), eq(2), eq(new BigDecimal("1500")), eq("fast"))).thenReturn(optimalRoute);
        when(routeOptimizationService.planOptimalRoute(eq(List.of(101)), eq(2), eq(new BigDecimal("1500")), eq("lowCarbon"))).thenReturn(optimalRoute);

        List<UserPreferenceRecommendation> result = intelligentRoutePersonalizationService.recommendRoutesByUserPreference(
                7,
                1,
                2,
                Map.of(
                        "preferredTypes", List.of("\u6587\u5316\u5386\u53f2"),
                        "budget", "1500"
                )
        );

        assertEquals(4, result.size());
        assertTrue(result.stream().allMatch(item -> item.getAttractionCount() == 1));
        assertTrue(result.stream().allMatch(item -> item.getRoute() == optimalRoute));
        verify(cacheUtil).set(anyString(), eq(result), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldReturnCachedPersonalizedRouteDirectly() {
        PersonalizedRouteResult cached = PersonalizedRouteResult.builder()
                .cityId(8)
                .days(3)
                .build();
        when(cacheUtil.get(anyString(), eq(PersonalizedRouteResult.class))).thenReturn(cached);

        PersonalizedRouteResult result = intelligentRoutePersonalizationService.generatePersonalizedRoute(
                PersonalizedRoutePreferences.builder()
                        .cityId(8)
                        .days(3)
                        .build(),
                PersonalizedRouteConstraints.builder().build()
        );

        assertSame(cached, result);
    }

    @Test
    void shouldBuildPersonalizedRouteFromInterestsAndStringBudget() {
        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();
        when(cacheUtil.get(anyString(), eq(PersonalizedRouteResult.class))).thenReturn(null);
        when(attractionService.getByCityId(3)).thenReturn(List.of(
                buildAttraction(301, "\u7f8e\u98df \u9910\u5385"),
                buildAttraction(302, "\u5c71\u6c34 \u81ea\u7136")
        ));
        when(routeOptimizationService.planOptimalRoute(eq(List.of(301)), eq(4), eq(new BigDecimal("888.50")), eq("fast"))).thenReturn(optimalRoute);

        PersonalizedRouteResult result = intelligentRoutePersonalizationService.generatePersonalizedRoute(
                PersonalizedRoutePreferences.builder()
                        .cityId(3)
                        .days(4)
                        .budget(new BigDecimal("888.50"))
                        .preference("fast")
                        .transportPreference("taxi")
                        .interests(List.of("\u7f8e\u98df\u4e4b\u65c5"))
                        .build(),
                PersonalizedRouteConstraints.builder()
                        .build()
        );

        assertSame(optimalRoute, result.getRoute());
        assertEquals(3, result.getCityId());
        assertEquals(4, result.getDays());
        assertEquals(1, result.getAttractionCount());
        assertEquals("taxi", result.getTransportPreference());
        assertEquals(new BigDecimal("888.50"), result.getUserPreferences().getBudget());
        assertEquals(List.of("\u7f8e\u98df\u4e4b\u65c5"), result.getUserPreferences().getInterests());
        verify(cacheUtil).set(anyString(), eq(result), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldGenerateMultiDayRouteUsingCityAndDateRange() {
        RoutePlanAlgorithm.OptimalRoute optimalRoute = new RoutePlanAlgorithm.OptimalRoute();
        when(cacheUtil.get(anyString(), eq(PersonalizedRouteResult.class))).thenReturn(null);
        when(attractionService.getByCityId(9)).thenReturn(List.of(buildAttraction(901, "\u516c\u56ed \u5c71\u6c34")));
        when(routeOptimizationService.planOptimalRoute(eq(List.of(901)), eq(3), eq(BigDecimal.valueOf(1000)), eq("balanced"))).thenReturn(optimalRoute);

        MultiDayRouteResult result = intelligentRoutePersonalizationService.generateMultiDayRoute(
                9,
                "2026-08-10",
                "2026-08-12",
                Map.of("interests", List.of("\u81ea\u7136\u98ce\u5149"))
        );

        assertEquals(9, result.getCityId());
        assertEquals("2026-08-10", result.getStartDate());
        assertEquals("2026-08-12", result.getEndDate());
        assertEquals(1, result.getAttractionCount());
        assertEquals(9, result.getUserPreferences().get("cityId"));
        assertEquals(3, result.getUserPreferences().get("days"));
    }

    @Test
    void shouldReturnRecommendationReason() {
        RouteRecommendationReason result = intelligentRoutePersonalizationService.getRouteRecommendationReason(11, 99);

        assertEquals(11, result.getRouteId());
        assertEquals(99, result.getUserId());
        assertEquals(List.of("\u8def\u7ebf\u8bc4\u5206\u9ad8", "\u7b26\u5408\u7528\u6237\u504f\u597d", "\u70ed\u95e8\u63a8\u8350"), result.getReasons());
    }

    private Attraction buildAttraction(Integer id, String description) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setDescription(description);
        return attraction;
    }
}
