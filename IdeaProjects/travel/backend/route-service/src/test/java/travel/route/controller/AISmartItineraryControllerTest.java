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
import travel.route.dto.ai.AISmartItineraryPlan;
import travel.route.dto.ai.AISmartItineraryResponse;
import travel.route.service.AISmartItineraryService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AISmartItineraryControllerTest {

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @Mock
    private AISmartItineraryService aiSmartItineraryService;

    @InjectMocks
    private AISmartItineraryController controller;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldBindDynamicPreferencesAsJsonNodes() throws Exception {
        when(aiSmartItineraryService.generateItinerary(
                anyMap(), eq(1000.0), eq(2), eq(7), eq(9)))
                .thenReturn(AISmartItineraryResponse.builder()
                        .userId(9)
                        .cityId(7)
                        .days(2)
                        .itinerary(AISmartItineraryPlan.builder().success(true).build())
                        .build());

        String requestBody = """
                {
                  "userId": 9,
                  "cityId": 7,
                  "days": 2,
                  "budget": 1000.0,
                  "preferences": {
                    "pace": "relaxed",
                    "avoidCrowd": true
                  }
                }
                """;

        mockMvc.perform(post("/ai/smart-itinerary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.itinerary.success").value(true));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, com.fasterxml.jackson.databind.JsonNode>> captor =
                ArgumentCaptor.forClass(Map.class);
        verify(aiSmartItineraryService).generateItinerary(
                captor.capture(), eq(1000.0), eq(2), eq(7), eq(9));
        assertEquals("relaxed", captor.getValue().get("pace").asText());
        assertEquals(true, captor.getValue().get("avoidCrowd").asBoolean());
    }

    @Test
    void shouldRejectInvalidDaysBeforeCallingService() throws Exception {
        String requestBody = """
                {
                  "cityId": 7,
                  "days": 0
                }
                """;

        mockMvc.perform(post("/ai/smart-itinerary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiSmartItineraryService);
    }
}
