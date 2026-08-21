package travel.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import travel.route.dto.ai.AIPlanRouteConstraints;
import travel.route.dto.ai.AIPlanRoutePreferences;
import travel.route.dto.ai.AIPlanRouteResponse;
import travel.route.dto.ai.AITravelGuideContent;
import travel.route.service.AIAdvancedService;
import travel.route.service.QwenService;
import travel.common.exception.GlobalExceptionHandler;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AIAdvancedControllerTest {

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @Mock
    private AIAdvancedService aiAdvancedService;

    @Mock
    private QwenService qwenService;

    @InjectMocks
    private AIAdvancedController aiAdvancedController;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(aiAdvancedController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldBindTypedPlanRequestAndPreserveExtensions() throws Exception {
        AIPlanRouteResponse route = AIPlanRouteResponse.builder()
                .success(true)
                .destination("上海")
                .days(2)
                .travelStyle("slow")
                .build();
        when(aiAdvancedService.planRoute(any(AIPlanRoutePreferences.class), any(AIPlanRouteConstraints.class)))
                .thenReturn(route);

        String requestBody = """
                {
                  "preferences": {
                    "destination": "上海",
                    "days": 2,
                    "travelStyle": "slow",
                    "transportPreference": "subway",
                    "budget": 3000,
                    "pace": "relaxed",
                    "extensions": {
                      "theme": "culture"
                    }
                  },
                  "constraints": {
                    "maxDailyHours": 8,
                    "mustVisitAttractions": ["外滩"],
                    "fixedTimeWindows": [
                      {"start": "09:00", "end": "12:00"}
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.destination").value("上海"))
                .andExpect(jsonPath("$.data.days").value(2));

        ArgumentCaptor<AIPlanRoutePreferences> preferencesCaptor =
                ArgumentCaptor.forClass(AIPlanRoutePreferences.class);
        ArgumentCaptor<AIPlanRouteConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(AIPlanRouteConstraints.class);
        verify(aiAdvancedService).planRoute(preferencesCaptor.capture(), constraintsCaptor.capture());

        AIPlanRoutePreferences preferences = preferencesCaptor.getValue();
        assertEquals("上海", preferences.getDestination());
        assertEquals(2, preferences.getDays());
        assertEquals("relaxed", preferences.getExtensions().get("pace").asText());
        assertEquals("culture", preferences.getExtensions().get("theme").asText());

        AIPlanRouteConstraints constraints = constraintsCaptor.getValue();
        assertEquals(8, constraints.getMaxDailyHours());
        assertEquals(1, constraints.getMustVisitAttractions().size());
        assertNotNull(constraints.getFixedTimeWindows());
        assertEquals("09:00", constraints.getFixedTimeWindows().get(0).getStart());
    }

    @Test
    void shouldMapConstraintConflictToBadRequestResult() throws Exception {
        when(aiAdvancedService.planRoute(any(AIPlanRoutePreferences.class), any(AIPlanRouteConstraints.class)))
                .thenThrow(new IllegalArgumentException("constraint conflict"));

        String requestBody = """
                {
                  "preferences": {"days": 1},
                  "constraints": {
                    "mustVisitAttractions": ["Museum"],
                    "avoidAttractions": ["Museum"]
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("参数错误"));
    }

    @Test
    void shouldHideUnexpectedPlanFailureBehindHttp500() throws Exception {
        when(aiAdvancedService.planRoute(
                any(AIPlanRoutePreferences.class), nullable(AIPlanRouteConstraints.class)))
                .thenThrow(new RuntimeException("database password leaked"));

        mockMvc.perform(post("/ai/advanced/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferences\":{\"days\":1}}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统异常，请稍后重试"));
    }

    @Test
    void shouldRejectInvalidPlanDaysBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "preferences": {
                    "days": 0
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiAdvancedService);
    }

    @Test
    void shouldRejectReverseTimeWindowBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "constraints": {
                    "fixedTimeWindows": [
                      {"start": "12:00", "end": "09:00"}
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiAdvancedService);
    }

    @Test
    void shouldRejectZeroLengthTimeWindowBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "constraints": {
                    "fixedTimeWindows": [
                      {"start": "09:00", "end": "09:00"}
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiAdvancedService);
    }

    @Test
    void shouldBindGuidePreferencesAsJsonNodesAndForwardToService() throws Exception {
        when(aiAdvancedService.generateTravelGuide(any(), anyInt(), anyMap()))
                .thenReturn(AITravelGuideContent.builder().success(true).days(2).build());

        String requestBody = """
                {
                  "cityId": 1,
                  "days": 2,
                  "preferences": {
                    "budget": 3000,
                    "pace": "relaxed",
                    "avoidCrowd": true
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/guide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.days").value(2));

        ArgumentCaptor<Map<String, JsonNode>> preferencesCaptor = ArgumentCaptor.captor();
        verify(aiAdvancedService).generateTravelGuide(eq(1), eq(2), preferencesCaptor.capture());

        Map<?, ?> preferences = preferencesCaptor.getValue();
        assertEquals(3, preferences.size());
        assertTrue(preferences.get("budget") instanceof JsonNode);
        assertEquals(3000, ((JsonNode) preferences.get("budget")).asInt());
        assertEquals("relaxed", ((JsonNode) preferences.get("pace")).asText());
        assertTrue(((JsonNode) preferences.get("avoidCrowd")).asBoolean());
    }

    @Test
    void shouldRejectGuideDaysOutsideSupportedRangeBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "cityId": 1,
                  "days": 31
                }
                """;

        mockMvc.perform(post("/ai/advanced/guide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiAdvancedService);
    }

    @Test
    void shouldRejectGuideWhenPreferencesExceedLimitBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "cityId": 1,
                  "days": 2,
                  "preferences": {
                    "p01": 1, "p02": 2, "p03": 3, "p04": 4, "p05": 5,
                    "p06": 6, "p07": 7, "p08": 8, "p09": 9, "p10": 10,
                    "p11": 11, "p12": 12, "p13": 13, "p14": 14, "p15": 15,
                    "p16": 16, "p17": 17, "p18": 18, "p19": 19, "p20": 20,
                    "p21": 21
                  }
                }
                """;

        mockMvc.perform(post("/ai/advanced/guide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiAdvancedService);
    }
}
