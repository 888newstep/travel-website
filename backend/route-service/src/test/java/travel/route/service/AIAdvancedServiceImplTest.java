package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.exception.BusinessException;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AIActivity;
import travel.route.dto.ai.AIPersonalizedRecommendationItem;
import travel.route.dto.ai.AIPlanRouteConstraints;
import travel.route.dto.ai.AIPlanRoutePreferences;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AITimeWindow;
import travel.route.service.impl.AIAdvancedServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIAdvancedServiceImplTest {

    @Mock
    private CacheUtil cacheUtil;

    @InjectMocks
    private AIAdvancedServiceImpl aiAdvancedService;

    @Test
    void shouldRejectPlanWithoutDestination() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                aiAdvancedService.planRoute(
                        AIPlanRoutePreferences.builder().days(1).build(),
                        AIPlanRouteConstraints.builder()
                                .mustVisitAttractions(List.of("Museum"))
                                .build()));

        assertEquals(4001, exception.getCode());
    }

    @Test
    void shouldRejectPlanWithoutExplicitAttractions() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                aiAdvancedService.planRoute(
                        AIPlanRoutePreferences.builder()
                                .destination("上海")
                                .days(1)
                                .build(),
                        null));

        assertEquals(18006, exception.getCode());
    }

    @Test
    void shouldScheduleOnlyUserProvidedAttractionsWithoutFabricatedMetrics() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder()
                        .destination("上海")
                        .days(1)
                        .travelStyle("slow")
                        .build(),
                AIPlanRouteConstraints.builder()
                        .mustVisitAttractions(List.of("Museum A", "Museum B"))
                        .build());

        List<AIActivity> activities = response.getDailyPlans().get(0).getActivities();
        assertEquals(List.of("Museum A", "Museum B"),
                activities.stream().map(AIActivity::getName).toList());
        assertTrue(activities.stream().allMatch(activity -> "attraction".equals(activity.getType())));
        assertEquals("constraint-scheduler", response.getPlanType());
        assertNull(response.getEstimatedCost());
        assertNull(response.getOptimizationScore());
    }

    @Test
    void shouldRespectFixedWindowsForExplicitAttractions() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder()
                        .destination("南京")
                        .days(1)
                        .build(),
                AIPlanRouteConstraints.builder()
                        .mustVisitAttractions(List.of("Museum"))
                        .fixedTimeWindows(List.of(new AITimeWindow("14:00", "17:00")))
                        .build());

        assertEquals("14:00-17:00",
                response.getDailyPlans().get(0).getActivities().get(0).getTime());
    }

    @Test
    void shouldRejectConflictingExplicitAttractions() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                aiAdvancedService.planRoute(
                        AIPlanRoutePreferences.builder()
                                .destination("南京")
                                .days(1)
                                .build(),
                        AIPlanRouteConstraints.builder()
                                .mustVisitAttractions(List.of("Museum"))
                                .avoidAttractions(List.of("museum"))
                                .build()));

        assertTrue(exception.getMessage().contains("Museum"));
    }

    @Test
    void shouldReportUnavailableUnsourcedAdvancedContent() {
        assertEquals(19002, assertThrows(BusinessException.class,
                () -> aiAdvancedService.generateTravelGuide(1, 2, null)).getCode());
        assertEquals(19002, assertThrows(BusinessException.class,
                () -> aiAdvancedService.estimateBudget(1, 2, null)).getCode());
        assertEquals(19002, assertThrows(BusinessException.class,
                () -> aiAdvancedService.getSafetyAdvice(1)).getCode());
    }

    @Test
    void shouldReturnEmptyRecommendationsForNonPositiveLimit() {
        List<AIPersonalizedRecommendationItem> recommendations =
                aiAdvancedService.getPersonalizedRecommendations(1, "attractions", -1);

        assertTrue(recommendations.isEmpty());
    }

    @Test
    void shouldClampLargeLimitAndIncludeLimitInCacheKey() {
        List<AIPersonalizedRecommendationItem> recommendations =
                aiAdvancedService.getPersonalizedRecommendations(1, "attractions", 100);

        assertTrue(recommendations.isEmpty());
        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheUtil).get(cacheKeyCaptor.capture(), eq(Object.class));
        assertTrue(cacheKeyCaptor.getValue().endsWith(":50"));
        verify(cacheUtil, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void shouldReadTypedRecommendationsFromCache() {
        List<AIPersonalizedRecommendationItem> cached = List.of(
                AIPersonalizedRecommendationItem.builder().id(1).name("one").build(),
                AIPersonalizedRecommendationItem.builder().id(2).name("two").build(),
                AIPersonalizedRecommendationItem.builder().id(3).name("three").build());
        when(cacheUtil.get(anyString(), eq(Object.class))).thenReturn(cached);

        List<AIPersonalizedRecommendationItem> recommendations =
                aiAdvancedService.getPersonalizedRecommendations(1, "attractions", 2);

        assertEquals(2, recommendations.size());
        assertEquals(1, recommendations.get(0).getId());
        assertEquals(2, recommendations.get(1).getId());
    }
}
