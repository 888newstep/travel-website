package travel.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AIBudgetDetails;
import travel.route.dto.ai.AIActivity;
import travel.route.dto.ai.AIPlanRouteConstraints;
import travel.route.dto.ai.AITimeWindow;
import travel.route.dto.ai.AIPlanRoutePreferences;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AIPersonalizedRecommendationItem;
import travel.route.dto.ai.AITravelGuideContent;
import travel.route.service.impl.AIAdvancedServiceImpl;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIAdvancedServiceImplTest {

    @Mock
    private CacheUtil cacheUtil;

    @InjectMocks
    private AIAdvancedServiceImpl aiAdvancedService;

    @Test
    void shouldUseDefaultsWhenPlanPreferencesAreNull() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(null, null);

        assertEquals("北京", response.getDestination());
        assertEquals(3, response.getDays());
        assertEquals(3, response.getDailyPlans().size());
    }

    @Test
    void shouldParseAndClampPlanDays() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder()
                        .destination("上海")
                        .days(99)
                        .travelStyle("slow")
                        .build(),
                null);

        assertEquals("上海", response.getDestination());
        assertEquals(30, response.getDays());
        assertEquals(30, response.getDailyPlans().size());
        assertEquals("slow", response.getTravelStyle());
    }

    @Test
    void shouldIncludeEveryMustVisitAttractionInGeneratedPlans() {
        List<String> requiredAttractions = List.of("Museum A", "Museum B", "Museum C");

        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(2).build(),
                AIPlanRouteConstraints.builder()
                        .mustVisitAttractions(requiredAttractions)
                        .build());

        List<String> activityNames = allActivities(response).stream()
                .map(AIActivity::getName)
                .toList();

        assertTrue(activityNames.containsAll(requiredAttractions));
        assertTrue(activityNames.indexOf("Museum A") < activityNames.indexOf("Museum B"));
        assertTrue(activityNames.indexOf("Museum B") < activityNames.indexOf("Museum C"));
    }

    @Test
    void shouldPrioritizeMustVisitAttractionsOverOptionalActivitiesWhenDailyCapacityIsTight() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .maxDailyHours(6)
                        .mustVisitAttractions(List.of("Museum A", "Museum B"))
                        .build());

        List<AIActivity> activities = allActivities(response);

        assertEquals(List.of("Museum A", "Museum B"), activities.stream()
                .map(AIActivity::getName)
                .toList());
        assertTrue(activities.stream().allMatch(activity -> "attraction".equals(activity.getType())));
        assertEquals(6 * 60, activities.stream()
                .mapToInt(activity -> durationMinutes(activity.getTime()))
                .sum());
    }

    @Test
    void shouldExcludeAvoidedAttractionsFromGeneratedPlans() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .avoidAttractions(List.of("\u666f\u70b9"))
                        .build());

        List<AIActivity> activities = allActivities(response);

        assertTrue(activities.stream().noneMatch(activity ->
                "attraction".equals(activity.getType()) && activity.getName().contains("\u666f\u70b9")));
        assertTrue(activities.stream().anyMatch(activity -> "restaurant".equals(activity.getType())));
    }

    @Test
    void shouldKeepMustVisitAttractionWhileFilteringOnlyDefaultAttractions() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .mustVisitAttractions(List.of("Preferred Museum"))
                        .avoidAttractions(List.of("景点"))
                        .build());

        List<AIActivity> activities = allActivities(response);

        assertEquals(List.of("Preferred Museum", "餐厅1"), activities.stream()
                .map(AIActivity::getName)
                .toList());
        assertTrue(activities.stream().noneMatch(activity ->
                "attraction".equals(activity.getType()) && activity.getName().contains("景点")));
    }

    @Test
    void shouldKeepEveryActivityCompletelyInsideFixedTimeWindows() {
        List<AITimeWindow> windows = List.of(
                new AITimeWindow("09:00", "12:00"),
                new AITimeWindow("14:00", "17:00"));

        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .fixedTimeWindows(windows)
                        .build());

        List<AIActivity> activities = allActivities(response);

        assertFalse(activities.isEmpty());
        activities.forEach(activity -> assertTrue(
                isInsideAnyWindow(activity.getTime(), windows),
                "activity is outside fixed windows: " + activity.getTime()));
    }

    @Test
    void shouldAllowShorterActivityToUseWindowRejectedByLongerActivity() {
        List<AITimeWindow> windows = List.of(new AITimeWindow("12:00", "13:30"));

        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .fixedTimeWindows(windows)
                        .build());

        List<AIActivity> activities = allActivities(response);

        assertEquals(1, activities.size());
        assertEquals("restaurant", activities.get(0).getType());
        assertEquals("12:00-13:30", activities.get(0).getTime());
    }

    @Test
    void shouldUseLaterWindowForMustVisitWhenEarlierWindowIsTooShort() {
        List<AITimeWindow> windows = List.of(
                new AITimeWindow("09:00", "10:00"),
                new AITimeWindow("12:00", "13:30"),
                new AITimeWindow("14:00", "17:00"));

        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .mustVisitAttractions(List.of("Museum"))
                        .fixedTimeWindows(windows)
                        .build());

        List<AIActivity> activities = allActivities(response);

        assertEquals(1, activities.size());
        assertEquals("Museum", activities.get(0).getName());
        assertEquals("14:00-17:00", activities.get(0).getTime());
        activities.forEach(activity -> assertTrue(
                isInsideAnyWindow(activity.getTime(), windows),
                "activity is outside fixed windows: " + activity.getTime()));
    }

    @Test
    void shouldRespectMaximumDailyActivityDuration() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(1).build(),
                AIPlanRouteConstraints.builder()
                        .maxDailyHours(4)
                        .build());

        List<AIActivity> activities = allActivities(response);
        int totalMinutes = activities.stream()
                .mapToInt(activity -> durationMinutes(activity.getTime()))
                .sum();

        assertEquals(1, activities.size());
        assertEquals("attraction", activities.get(0).getType());
        assertEquals(180, totalMinutes);
        assertTrue(totalMinutes <= 4 * 60);
    }

    @Test
    void shouldApplyMaximumDailyActivityDurationIndependentlyForEachDay() {
        AIPlanRouteResponse response = aiAdvancedService.planRoute(
                AIPlanRoutePreferences.builder().days(2).build(),
                AIPlanRouteConstraints.builder()
                        .maxDailyHours(4)
                        .build());

        assertEquals(2, response.getDailyPlans().size());
        response.getDailyPlans().forEach(dailyPlan -> {
            int dailyMinutes = dailyPlan.getActivities().stream()
                    .mapToInt(activity -> durationMinutes(activity.getTime()))
                    .sum();
            assertEquals(180, dailyMinutes);
            assertTrue(dailyMinutes <= 4 * 60);
        });
    }

    @Test
    void shouldRejectMustVisitAndAvoidConflict() {
        AIPlanRouteConstraints constraints = AIPlanRouteConstraints.builder()
                .mustVisitAttractions(List.of("Museum"))
                .avoidAttractions(List.of("museum"))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                aiAdvancedService.planRoute(
                        AIPlanRoutePreferences.builder().days(1).build(), constraints));
        assertTrue(exception.getMessage().contains("Museum"));
    }

    @Test
    void shouldRejectMustVisitWhenDailyCapacityIsInsufficient() {
        AIPlanRouteConstraints constraints = AIPlanRouteConstraints.builder()
                .mustVisitAttractions(List.of("Museum A", "Museum B", "Museum C"))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                aiAdvancedService.planRoute(
                        AIPlanRoutePreferences.builder().days(1).build(), constraints));
        assertTrue(exception.getMessage().contains("Museum C"));
    }

    @Test
    void shouldRejectMustVisitWhenNoFixedWindowCanFitIt() {
        AIPlanRouteConstraints constraints = AIPlanRouteConstraints.builder()
                .mustVisitAttractions(List.of("Museum"))
                .fixedTimeWindows(List.of(new AITimeWindow("09:00", "10:00")))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                aiAdvancedService.planRoute(
                        AIPlanRoutePreferences.builder().days(1).build(), constraints));
        assertTrue(exception.getMessage().contains("Museum"));
    }

    @Test
    void shouldReturnTypedTravelGuideAndNormalizeInvalidDays() {
        AITravelGuideContent guide = aiAdvancedService.generateTravelGuide(1, 0, Map.<String, JsonNode>of());

        assertTrue(guide.getSuccess());
        assertEquals(1, guide.getDays());
        assertNotNull(guide.getGuideContent());
        assertEquals(1, guide.getGuideContent().getDailyItineraries().size());
        assertEquals("首都国际机场、大兴国际机场",
                guide.getGuideContent().getTransportation().getAirport());
        assertEquals(2, guide.getGuideContent().getFoodRecommendations().size());
    }

    @Test
    void shouldReturnTypedBudgetDetailsAndNormalizeInvalidDays() {
        AIBudgetDetails details = aiAdvancedService.estimateBudget(1, -1, null);

        assertTrue(details.getSuccess());
        assertEquals(1, details.getDays());
        assertNotNull(details.getBudgetDetails());
        assertEquals(1050.0, details.getBudgetDetails().getTotal());
        assertEquals("CNY", details.getCurrency());
        assertEquals(5, details.getSavingTips().size());
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

        assertEquals(50, recommendations.size());

        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheUtil).get(cacheKeyCaptor.capture(), eq(Object.class));
        assertTrue(cacheKeyCaptor.getValue().endsWith(":50"));
        verify(cacheUtil).set(eq(cacheKeyCaptor.getValue()), any(), eq(12L), any());
    }

    @Test
    void shouldIsolateRecommendationCachesByLimit() {
        aiAdvancedService.getPersonalizedRecommendations(1, "attractions", 3);
        aiAdvancedService.getPersonalizedRecommendations(1, "attractions", 5);

        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheUtil, org.mockito.Mockito.times(2))
                .get(cacheKeyCaptor.capture(), eq(Object.class));

        assertEquals(2, cacheKeyCaptor.getAllValues().size());
        assertTrue(cacheKeyCaptor.getAllValues().get(0).endsWith(":3"));
        assertTrue(cacheKeyCaptor.getAllValues().get(1).endsWith(":5"));
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

    private static List<AIActivity> allActivities(AIPlanRouteResponse response) {
        return response.getDailyPlans().stream()
                .flatMap(dailyPlan -> dailyPlan.getActivities().stream())
                .toList();
    }

    private static int durationMinutes(String timeRange) {
        LocalTime[] times = parseTimeRange(timeRange);
        return Math.toIntExact(Duration.between(times[0], times[1]).toMinutes());
    }

    private static boolean isInsideAnyWindow(String timeRange, List<AITimeWindow> windows) {
        LocalTime[] activityTimes = parseTimeRange(timeRange);
        return windows.stream().anyMatch(window -> {
            LocalTime windowStart = LocalTime.parse(window.getStart());
            LocalTime windowEnd = LocalTime.parse(window.getEnd());
            return !activityTimes[0].isBefore(windowStart)
                    && !activityTimes[1].isAfter(windowEnd);
        });
    }

    private static LocalTime[] parseTimeRange(String timeRange) {
        String[] parts = timeRange.split("-", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid activity time range: " + timeRange);
        }
        return new LocalTime[]{LocalTime.parse(parts[0]), LocalTime.parse(parts[1])};
    }
}
