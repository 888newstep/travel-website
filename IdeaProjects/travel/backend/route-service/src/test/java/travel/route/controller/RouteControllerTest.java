package travel.route.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import travel.route.dto.route.RealTimeAdjustmentRequest;
import travel.route.dto.route.RealTimeAdjustmentResult;
import travel.route.dto.route.PersonalizedRouteConstraints;
import travel.route.dto.route.PersonalizedRoutePreferences;
import travel.route.dto.route.PersonalizedRouteResult;
import travel.route.dto.route.RouteQualityEvaluation;
import travel.route.dto.route.RouteQualityEvaluationRequest;
import travel.route.service.IntelligentRouteService;
import travel.route.service.RouteService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RouteControllerTest {

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @Mock
    private RouteService routeService;

    @Mock
    private IntelligentRouteService intelligentRouteService;

    @InjectMocks
    private RouteController routeController;

    @BeforeEach
    public void setup() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(routeController)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    public void testGetRouteThemes() throws Exception {
        mockMvc.perform(get("/routes/smart/themes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testGetRouteSeasons() throws Exception {
        mockMvc.perform(get("/routes/smart/seasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldBindTypedRealtimeAdjustmentRequestAndPreserveExtensions() throws Exception {
        when(intelligentRouteService.getRealTimeAdjustment(eq(1), any(RealTimeAdjustmentRequest.class)))
                .thenReturn(RealTimeAdjustmentResult.builder()
                        .routeId(1)
                        .routeName("route-1")
                        .build());

        String requestBody = """
                {
                  "currentLocation": {
                    "latitude": 31.2,
                    "longitude": 121.5
                  },
                  "realTimeFactors": {
                    "weather": "rainy",
                    "traffic": {
                      "congestedRoutes": ["Road-A", "Road-B"]
                    },
                    "crowd": {
                      "crowdedAttractions": [201]
                    },
                    "supplierSignal": {
                      "level": "high"
                    }
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/real-time-adjustment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.routeId").value(1));

        ArgumentCaptor<RealTimeAdjustmentRequest> requestCaptor =
                ArgumentCaptor.forClass(RealTimeAdjustmentRequest.class);
        verify(intelligentRouteService).getRealTimeAdjustment(eq(1), requestCaptor.capture());

        RealTimeAdjustmentRequest request = requestCaptor.getValue();
        assertNotNull(request.getCurrentLocation());
        assertEquals(31.2, request.getCurrentLocation().getLat());
        assertEquals(121.5, request.getCurrentLocation().getLng());
        assertEquals("rainy", request.getRealTimeFactors().getWeather());
        assertEquals(List.of("Road-A", "Road-B"),
                request.getRealTimeFactors().getTraffic().getCongestedRoutes());
        assertEquals(List.of(201),
                request.getRealTimeFactors().getCrowd().getCrowdedAttractions());
        assertEquals("high", request.getRealTimeFactors().getExtensions()
                .get("supplierSignal").get("level").asText());
    }

    @Test
    void shouldRejectInvalidRealtimeCoordinatesBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "currentLocation": {
                    "lat": 91.0,
                    "lng": 121.5
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/real-time-adjustment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(intelligentRouteService);
    }

    @Test
    void shouldRejectPartialRealtimeCoordinatesBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "currentLocation": {
                    "lat": 31.2
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/real-time-adjustment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(intelligentRouteService);
    }

    @Test
    void shouldBindPersonalizedRoutePreferencesAndPreserveExtensions() throws Exception {
        when(intelligentRouteService.generatePersonalizedRoute(
                any(PersonalizedRoutePreferences.class), any(PersonalizedRouteConstraints.class)))
                .thenReturn(PersonalizedRouteResult.builder()
                        .cityId(8)
                        .days(3)
                        .transportPreference("public")
                        .build());

        String requestBody = """
                {
                  "userPreferences": {
                    "cityId": 8,
                    "days": 3,
                    "budget": 888.50,
                    "preference": "fast",
                    "interests": ["\u7f8e\u98df\u4e4b\u65c5"],
                    "transportPreference": "public",
                    "audience": {"type": "family"}
                  },
                  "constraints": {
                    "avoidPeak": true
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/generate-personalized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cityId").value(8));

        ArgumentCaptor<PersonalizedRoutePreferences> preferencesCaptor =
                ArgumentCaptor.forClass(PersonalizedRoutePreferences.class);
        ArgumentCaptor<PersonalizedRouteConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(PersonalizedRouteConstraints.class);
        verify(intelligentRouteService).generatePersonalizedRoute(
                preferencesCaptor.capture(), constraintsCaptor.capture());

        PersonalizedRoutePreferences preferences = preferencesCaptor.getValue();
        assertEquals(8, preferences.getCityId());
        assertEquals(3, preferences.getDays());
        assertEquals(new java.math.BigDecimal("888.50"), preferences.getBudget());
        assertEquals("fast", preferences.getPreference());
        assertEquals(List.of("\u7f8e\u98df\u4e4b\u65c5"), preferences.getInterests());
        assertEquals("family", preferences.getExtensions().get("audience").get("type").asText());
        assertEquals(true, constraintsCaptor.getValue().getExtensions().get("avoidPeak").asBoolean());
    }

    @Test
    void shouldRejectInvalidPersonalizedRouteCityBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "userPreferences": {
                    "cityId": 0,
                    "days": 3
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/generate-personalized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(intelligentRouteService);
    }

    @Test
    void shouldRejectMissingPersonalizedRoutePreferencesBeforeCallingService() throws Exception {
        mockMvc.perform(post("/routes/smart/generate-personalized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"constraints":{"avoidPeak":true}}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(intelligentRouteService);
    }

    @Test
    void shouldRejectTooManyPersonalizedRouteExtensionsBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "userPreferences": {
                    "cityId": 8,
                    "p01": 1, "p02": 2, "p03": 3, "p04": 4, "p05": 5,
                    "p06": 6, "p07": 7, "p08": 8, "p09": 9, "p10": 10,
                    "p11": 11, "p12": 12, "p13": 13, "p14": 14, "p15": 15,
                    "p16": 16, "p17": 17, "p18": 18, "p19": 19, "p20": 20,
                    "p21": 21
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/generate-personalized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(intelligentRouteService);
    }

    @Test
    void shouldBindRouteQualityEvaluationExtensions() throws Exception {
        when(intelligentRouteService.evaluateRouteQuality(eq(1), any(RouteQualityEvaluationRequest.class)))
                .thenReturn(RouteQualityEvaluation.builder()
                        .routeId(1)
                        .qualityScore(0.8)
                        .overallScore(0.8)
                        .build());

        String requestBody = """
                {
                  "profile": "balanced",
                  "includeDiagnostics": true,
                  "weights": {
                    "diversity": 0.2
                  }
                }
                """;

        mockMvc.perform(post("/routes/smart/evaluate/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.routeId").value(1))
                .andExpect(jsonPath("$.data.overallScore").value(0.8));

        ArgumentCaptor<RouteQualityEvaluationRequest> requestCaptor =
                ArgumentCaptor.forClass(RouteQualityEvaluationRequest.class);
        verify(intelligentRouteService).evaluateRouteQuality(eq(1), requestCaptor.capture());
        RouteQualityEvaluationRequest request = requestCaptor.getValue();
        assertEquals("balanced", request.getExtensions().get("profile").asText());
        assertTrue(request.getExtensions().get("includeDiagnostics").asBoolean());
        assertEquals(0.2, request.getExtensions().get("weights").get("diversity").asDouble());
    }

    @Test
    void shouldRejectTooManyRouteQualityExtensionsBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "p01": 1, "p02": 2, "p03": 3, "p04": 4, "p05": 5,
                  "p06": 6, "p07": 7, "p08": 8, "p09": 9, "p10": 10,
                  "p11": 11, "p12": 12, "p13": 13, "p14": 14, "p15": 15,
                  "p16": 16, "p17": 17, "p18": 18, "p19": 19, "p20": 20,
                  "p21": 21
                }
                """;

        mockMvc.perform(post("/routes/smart/evaluate/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(intelligentRouteService);
    }
}
