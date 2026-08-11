package travel.route.service;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.City;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AISmartItineraryOptimization;
import travel.route.dto.ai.AISmartItineraryPlan;
import travel.route.dto.ai.AISmartItineraryResponse;
import travel.route.dto.ai.AISmartItineraryOptimizeResponse;
import travel.route.service.impl.AISmartItineraryServiceImpl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
class AISmartItineraryServiceImplTest {

    @Mock
    private RouteService routeService;

    @Mock
    private CacheUtil cacheUtil;

    @InjectMocks
    private AISmartItineraryServiceImpl service;

    @Test
    void shouldBuildTypedItineraryAndPreserveJsonNodePreferences() {
        Map<String, com.fasterxml.jackson.databind.JsonNode> preferences = Map.of(
                "pace", TextNode.valueOf("relaxed"));

        AISmartItineraryResponse response = service.generateItinerary(
                preferences, 1000.0, 2, 7, 9);

        AISmartItineraryPlan plan = response.getItinerary();
        assertTrue(plan.getSuccess());
        assertEquals(2, plan.getDailyPlans().size());
        assertEquals(4, plan.getDailyPlans().get(0).getActivities().size());
        assertEquals(9.0, plan.getDailyPlans().get(0).getTotalDuration());
        assertEquals(800.0, plan.getEstimatedCost());
        assertEquals("relaxed", plan.getPreferences().get("pace").asText());
        verify(cacheUtil).set(anyString(), any(AISmartItineraryPlan.class), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void shouldReadTypedItineraryFromVersionedCache() {
        AISmartItineraryPlan cachedPlan = AISmartItineraryPlan.builder()
                .success(true)
                .days(1)
                .build();
        when(cacheUtil.get(anyString(), eq(AISmartItineraryPlan.class))).thenReturn(cachedPlan);

        AISmartItineraryResponse response = service.generateItinerary(Map.of(), 1000.0, 1, 7, 9);

        assertSame(cachedPlan, response.getItinerary());
        verify(cacheUtil, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldBuildTypedOptimizationResult() {
        Route route = new Route();
        route.setTitle("City route");
        City city = new City();
        city.setName("Nanjing");
        route.setCity(city);
        when(routeService.getById(11)).thenReturn(route);

        AISmartItineraryOptimizeResponse response = service.optimizeItinerary(11, Map.of());

        AISmartItineraryOptimization optimization = response.getOptimized();
        assertTrue(optimization.getSuccess());
        assertEquals("City route", optimization.getRouteTitle());
        assertEquals("Nanjing", optimization.getCityName());
        assertEquals(3, optimization.getOptimizations().size());
    }

    @Test
    void shouldRejectOptimizationForMissingRoute() {
        when(routeService.getById(11)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.optimizeItinerary(11, Map.of()));
    }
}
