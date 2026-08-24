package travel.route.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import travel.route.dto.optimization.ApplyOptimizationRequest;
import travel.route.service.RouteOptimizationService;
import travel.route.service.RouteService;
import travel.common.exception.GlobalExceptionHandler;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @Mock
    private RouteOptimizationService routeOptimizationService;

    @Mock
    private RouteService routeService;

    @InjectMocks
    private RouteOptimizationController routeOptimizationController;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(routeOptimizationController)
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
    void shouldBindFrontendSuggestionPayload() throws Exception {
        authenticate(42L);
        when(routeOptimizationService.applyOptimization(any(ApplyOptimizationRequest.class)))
                .thenReturn(true);

        String requestBody = """
                {
                  "routeId": 7,
                  "suggestionId": 3,
                  "suggestion": {
                    "type": "distance",
                    "targetAttractionId": 101,
                    "attractionOrder": [101, 102]
                  }
                }
                """;

        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<ApplyOptimizationRequest> requestCaptor =
                ArgumentCaptor.forClass(ApplyOptimizationRequest.class);
        verify(routeOptimizationService).applyOptimization(requestCaptor.capture());

        ApplyOptimizationRequest request = requestCaptor.getValue();
        assertEquals(7, request.getRouteId());
        assertEquals(3, request.getSuggestionId());
        assertNotNull(request.getSuggestion());
        assertEquals("distance", request.getSuggestion().get("type").asText());
        assertEquals(101, request.getSuggestion().get("targetAttractionId").asInt());
        assertEquals(2, request.getSuggestion().get("attractionOrder").size());
    }

    @Test
    void shouldBindLegacyOptimizationPayload() throws Exception {
        authenticate(42L);
        when(routeOptimizationService.applyOptimization(any(ApplyOptimizationRequest.class)))
                .thenReturn(true);

        String requestBody = """
                {
                  "routeId": 7,
                  "optimizationType": "shortest",
                  "parameters": {
                    "maxDistance": 1000,
                    "orderedAttractionIds": [101, 102]
                  }
                }
                """;

        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<ApplyOptimizationRequest> requestCaptor =
                ArgumentCaptor.forClass(ApplyOptimizationRequest.class);
        verify(routeOptimizationService).applyOptimization(requestCaptor.capture());

        ApplyOptimizationRequest request = requestCaptor.getValue();
        assertEquals("shortest", request.getOptimizationType());
        assertEquals(1000, request.getParameters().get("maxDistance").asInt());
        assertEquals(2, request.getParameters().get("orderedAttractionIds").size());
        assertEquals(101, request.getParameters().get("orderedAttractionIds").get(0).asInt());
    }

    @Test
    void shouldPreserveUnknownFieldsInBoundedExtensions() throws Exception {
        authenticate(42L);
        when(routeOptimizationService.applyOptimization(any(ApplyOptimizationRequest.class)))
                .thenReturn(true);

        String requestBody = """
                {
                  "routeId": 7,
                  "suggestionId": 3,
                  "dryRun": true,
                  "algorithmVersion": "v2"
                }
                """;

        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<ApplyOptimizationRequest> requestCaptor =
                ArgumentCaptor.forClass(ApplyOptimizationRequest.class);
        verify(routeOptimizationService).applyOptimization(requestCaptor.capture());

        Map<String, JsonNode> extensions = requestCaptor.getValue().getExtensions();
        assertNotNull(extensions);
        assertEquals(true, extensions.get("dryRun").asBoolean());
        assertEquals("v2", extensions.get("algorithmVersion").asText());
    }

    @Test
    void shouldRejectNonPositiveRouteIdBeforeCallingService() throws Exception {
        String requestBody = """
                {"routeId":0,"suggestionId":3}
                """;

        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeOptimizationService);
    }

    @Test
    void shouldReturnUnauthorizedHttpStatusWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":7,\"suggestionId\":3}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(28002));

        verifyNoInteractions(routeOptimizationService, routeService);
    }

    @Test
    void shouldRejectNonPositiveSuggestionIdBeforeCallingService() throws Exception {
        String requestBody = """
                {"routeId":7,"suggestionId":0}
                """;

        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeOptimizationService);
    }

    @Test
    void shouldRejectMoreThanTwentyUnknownFields() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("routeId", 7);
        for (int index = 1; index <= 21; index++) {
            body.put("extension" + index, true);
        }

        mockMvc.perform(post("/route-optimization/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routeOptimizationService);
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
