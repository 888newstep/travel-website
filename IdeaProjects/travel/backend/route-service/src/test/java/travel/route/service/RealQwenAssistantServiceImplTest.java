package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.service.impl.RealQwenAssistantServiceImpl;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealQwenAssistantServiceImplTest {

    @Mock
    private RouteService routeService;

    @Mock
    private AttractionService attractionService;

    @Mock
    private CacheUtil cacheUtil;

    @Mock
    private QwenService qwenService;

    @InjectMocks
    private RealQwenAssistantServiceImpl realQwenAssistantService;

    @Test
    void shouldFallbackForAskQuestionWhenQwenFails() {
        when(cacheUtil.get(anyString(), eq(Map.class))).thenReturn(null);
        when(qwenService.travelQA("\u95e8\u7968\u600e\u4e48\u9884\u8ba2")).thenThrow(new RuntimeException("boom"));

        AIAskQuestionResponse response = realQwenAssistantService.askQuestion("\u95e8\u7968\u600e\u4e48\u9884\u8ba2", 1);

        assertEquals("fallback", response.getSource());
        assertTrue(response.getAnswer().contains("\u5b98\u65b9\u6e20\u9053"));
    }

    @Test
    void shouldFallbackForRouteOptimizationWhenQwenFails() {
        Route route = new Route();
        route.setId(5);
        route.setTitle("route-5");
        when(routeService.getById(5)).thenReturn(route);
        when(qwenService.chatCompletion(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        AIOptimizeRouteResponse response = realQwenAssistantService.optimizeRouteByAI(5);

        assertTrue(response.getSuccess());
        assertEquals("fallback", response.getSource());
        assertEquals(1, response.getSuggestions().size());
        assertTrue(response.getMessage().contains("timeout"));
    }

    @Test
    void shouldFallbackForAttractionIntroWhenQwenFails() {
        Attraction attraction = new Attraction();
        attraction.setId(6);
        attraction.setName("museum");
        attraction.setDescription("\u57fa\u7840\u4ecb\u7ecd");
        when(attractionService.getById(6)).thenReturn(attraction);
        when(cacheUtil.get(anyString(), eq(Map.class))).thenReturn(null);
        when(qwenService.generateAttractionIntro(anyString(), anyString())).thenThrow(new RuntimeException("provider down"));

        AIAttractionIntroResponse response = realQwenAssistantService.getAttractionIntro(6);

        assertTrue(response.getSuccess());
        assertEquals("fallback", response.getSource());
        assertEquals("\u57fa\u7840\u4ecb\u7ecd", response.getDetailedIntro());
        assertTrue(response.getMessage().contains("provider down"));
    }
}