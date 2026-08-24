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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import travel.route.service.RouteLifecycleService;
import travel.route.service.RouteVisitAnalyticsService;
import travel.route.dto.route.RouteVisitAnalyticsResponse;
import travel.route.dto.route.RouteVisitTrendItem;
import travel.common.entity.route_planning.Route;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.exception.GlobalExceptionHandler;

import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Mock
    private RouteLifecycleService routeLifecycleService;

    @Mock
    private RouteVisitAnalyticsService routeVisitAnalyticsService;

    @InjectMocks
    private RouteController routeController;

    @BeforeEach
    public void setup() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(routeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void shouldUseAuthenticatedUserWhenCreatingRoute() throws Exception {
        authenticate(42L);
        doReturn(true).when(routeService).save(any(Route.class));

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":99,
                                  "userId":7,
                                  "title":"secure route",
                                  "cityId":1,
                                  "durationDays":3,
                                  "viewCount":999,
                                  "likeCount":999,
                                  "isPublic":true,
                                  "status":"PUBLISHED",
                                  "version":99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.viewCount").value(0))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.isPublic").value(false))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
        verify(routeService).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(42, captor.getValue().getUserId());
        assertEquals(0, captor.getValue().getViewCount());
        assertEquals(0, captor.getValue().getLikeCount());
        assertEquals(false, captor.getValue().getIsPublic());
        assertEquals("DRAFT", captor.getValue().getStatus());
    }

    @Test
    void shouldRejectInvalidCreateRouteRequests() throws Exception {
        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cityId\":1,\"durationDays\":3}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"route\",\"durationDays\":3}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"route\",\"cityId\":1,\"durationDays\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"route\",\"cityId\":1,\"durationDays\":31}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeService);
    }

    @Test
    void shouldReturnBusinessErrorWhenCreateFails() throws Exception {
        authenticate(42L);
        when(routeService.save(any(Route.class))).thenReturn(false);

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"route\",\"cityId\":1,\"durationDays\":3}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ROUTE_CREATE_FAILED.getCode()));
    }

    @Test
    void shouldCheckOwnerAndPreserveOwnerWhenUpdatingRoute() throws Exception {
        authenticate(42L);
        Route existing = new Route();
        existing.setId(8);
        existing.setUserId(42);
        existing.setViewCount(10);
        existing.setLikeCount(5);
        existing.setIsPublic(false);
        existing.setStatus("DRAFT");
        doReturn(existing).when(routeService).getById(8);
        doReturn(true).when(routeService).updateById(any(Route.class));

        mockMvc.perform(put("/routes/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":7,
                                  "title":"updated route",
                                  "viewCount":999,
                                  "likeCount":999,
                                  "isPublic":true,
                                  "status":"PUBLISHED",
                                  "version":99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.isPublic").value(false))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(routeService).checkRouteOwner(8L, 42L);
        ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
        verify(routeService).updateById(captor.capture());
        assertEquals(42, captor.getValue().getUserId());
        assertEquals(10, captor.getValue().getViewCount());
        assertEquals(5, captor.getValue().getLikeCount());
        assertEquals(false, captor.getValue().getIsPublic());
        assertEquals("DRAFT", captor.getValue().getStatus());
    }

    @Test
    void shouldRejectBlankTitleWhenUpdatingRoute() throws Exception {
        mockMvc.perform(put("/routes/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeService);
    }

    @Test
    void shouldRejectEmptyUpdateRouteRequest() throws Exception {
        mockMvc.perform(put("/routes/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeService);
    }

    @Test
    void shouldReturnBusinessErrorWhenUpdateFails() throws Exception {
        authenticate(42L);
        Route existing = new Route();
        existing.setId(8);
        existing.setUserId(42);
        when(routeService.getById(8)).thenReturn(existing);
        when(routeService.updateById(existing)).thenReturn(false);

        mockMvc.perform(put("/routes/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated route\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ROUTE_UPDATE_FAILED.getCode()));
    }

    @Test
    void shouldReturnNotFoundForMissingRoute() throws Exception {
        when(routeService.getById(404)).thenReturn(null);

        mockMvc.perform(get("/routes/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ROUTE_NOT_EXIST.getCode()));
    }

    @Test
    void shouldReturnBusinessErrorWhenDeleteFails() throws Exception {
        authenticate(42L);
        when(routeService.removeById(8)).thenReturn(false);

        mockMvc.perform(delete("/routes/8"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ROUTE_DELETE_FAILED.getCode()));

        verify(routeService).checkRouteOwner(8L, 42L);
    }

    @Test
    void shouldReturnNotFoundWhenCopySourceDoesNotExist() throws Exception {
        authenticate(42L);
        when(routeLifecycleService.copyRoute(404, 42))
                .thenThrow(new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST));

        mockMvc.perform(post("/routes/404/copy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodeEnum.ROUTE_NOT_EXIST.getCode()));
    }

    @Test
    void shouldRecordReadableRouteVisitAndReturnIncrementedCount() throws Exception {
        Route route = new Route();
        route.setId(1);
        route.setUserId(7);
        route.setStatus("PUBLISHED");
        route.setIsPublic(true);
        route.setViewCount(10);
        when(routeService.getById(1)).thenReturn(route);

        mockMvc.perform(get("/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(11));

        verify(routeVisitAnalyticsService).recordVisit(eq(route), eq(null), any());
    }

    @Test
    void shouldReadRouteForBusinessValidationWithoutRecordingVisit() throws Exception {
        authenticate(42L);
        Route route = new Route();
        route.setId(1);
        route.setUserId(7);
        route.setStatus("PUBLISHED");
        route.setIsPublic(true);
        route.setViewCount(10);
        when(routeService.getById(1)).thenReturn(route);

        mockMvc.perform(get("/routes/1/readable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(10));

        verifyNoInteractions(routeVisitAnalyticsService);
    }

    @Test
    void shouldRestrictRouteAnalyticsToOwnerAndReturnTypedResponse() throws Exception {
        authenticate(42L);
        LocalDate today = LocalDate.now();
        RouteVisitAnalyticsResponse response = new RouteVisitAnalyticsResponse(
                8,
                today.minusDays(6),
                today,
                100,
                20,
                10,
                3,
                0.3,
                List.of(new RouteVisitTrendItem(today, 2, 2)));
        when(routeVisitAnalyticsService.getAnalytics(8, 7)).thenReturn(response);

        mockMvc.perform(get("/routes/8/analytics").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeId").value(8))
                .andExpect(jsonPath("$.data.retentionRate").value(0.3))
                .andExpect(jsonPath("$.data.trend[0].visits").value(2));

        verify(routeService).checkRouteOwner(8L, 42L);
        verify(routeVisitAnalyticsService).getAnalytics(8, 7);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
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
        authenticate(42L);
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
        Route publicRoute = new Route();
        publicRoute.setId(1);
        publicRoute.setIsPublic(true);
        when(routeService.getById(1)).thenReturn(publicRoute);
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
