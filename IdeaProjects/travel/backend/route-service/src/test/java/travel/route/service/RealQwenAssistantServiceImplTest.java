package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.common.entity.route_planning.Route;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.exception.BusinessException;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIAttractionIntroResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.service.impl.RealQwenAssistantServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealQwenAssistantServiceImplTest {

    @Mock
    private RouteService routeService;

    @Mock
    private AttractionService attractionService;

    @Mock
    private QwenService qwenService;

    @InjectMocks
    private RealQwenAssistantServiceImpl realQwenAssistantService;

    @Test
    void shouldReturnProviderAnswerWithoutInventedConfidence() {
        when(qwenService.travelQA("门票怎么预订")).thenReturn("请通过景区官方渠道预订。");

        AIAskQuestionResponse response = realQwenAssistantService.askQuestion("门票怎么预订", 1);

        assertEquals("qwen", response.getSource());
        assertEquals("请通过景区官方渠道预订。", response.getAnswer());
        assertNull(response.getConfidence());
    }

    @Test
    void shouldExposeDependencyFailureWhenQuestionProviderFails() {
        when(qwenService.travelQA("门票怎么预订")).thenThrow(new RuntimeException("boom"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> realQwenAssistantService.askQuestion("门票怎么预订", 1));

        assertEquals(5006, exception.getCode());
    }

    @Test
    void shouldReturnRouteSuggestionWithoutInventedScore() {
        Route route = new Route();
        route.setId(5);
        route.setTitle("route-5");
        when(routeService.getById(5)).thenReturn(route);
        when(qwenService.chatCompletion(anyString(), anyString())).thenReturn("优先乘坐地铁。");

        AIOptimizeRouteResponse response = realQwenAssistantService.optimizeRouteByAI(5);

        assertEquals("qwen", response.getSource());
        assertEquals("优先乘坐地铁。", response.getSuggestions().get(0).getDescription());
        assertNull(response.getOptimizedScore());
    }

    @Test
    void shouldRejectMissingRoute() {
        when(routeService.getById(5)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> realQwenAssistantService.optimizeRouteByAI(5));

        assertEquals(2001, exception.getCode());
    }

    @Test
    void shouldLeaveUnavailableAttractionFieldsEmpty() {
        Attraction attraction = new Attraction();
        attraction.setId(6);
        attraction.setName("museum");
        when(attractionService.getById(6)).thenReturn(attraction);
        when(qwenService.generateAttractionIntro(anyString(), anyString())).thenReturn("provider intro");

        AIAttractionIntroResponse response = realQwenAssistantService.getAttractionIntro(6);

        assertEquals("provider intro", response.getDetailedIntro());
        assertEquals("qwen", response.getSource());
        assertNull(response.getFunFacts());
        assertNull(response.getBestVisitTime());
        assertNull(response.getEstimatedDuration());
    }

    @Test
    void shouldRejectMissingAttraction() {
        when(attractionService.getById(6)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> realQwenAssistantService.getAttractionIntro(6));

        assertEquals(3002, exception.getCode());
    }
}
