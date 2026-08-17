package travel.route.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import travel.route.service.QwenService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AIControllerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @Mock
    private QwenService qwenService;

    @InjectMocks
    private AIController aiController;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldBindJsonNodePreferencesAndPreserveItineraryResponse() throws Exception {
        String endpoint = "/ai/itinerary/generate";
        when(qwenService.recommendItinerary(anyString(), eq(2), eq("总预算不超过1800元")))
                .thenReturn("generated-itinerary");

        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("pace", "relaxed");
        preferences.put("themes", List.of("food", "culture"));
        preferences.put("avoidCrowd", true);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("destination", "Hangzhou");
        body.put("days", 2);
        body.put("budget", 1800);
        body.put("preferences", preferences);
        String requestBody = new ObjectMapper().writeValueAsString(body);

        mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.destination").value("Hangzhou"))
                .andExpect(jsonPath("$.data.days").value(2))
                .andExpect(jsonPath("$.data.source").value("qwen"))
                .andExpect(jsonPath("$.data.itinerary").value("generated-itinerary"));

        ArgumentCaptor<String> preferencesCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenService).recommendItinerary(preferencesCaptor.capture(), eq(2), eq("总预算不超过1800元"));
        assertTrue(preferencesCaptor.getValue().contains("avoidCrowd"));
        assertTrue(preferencesCaptor.getValue().contains("relaxed"));
    }

    @Test
    void shouldRejectItineraryDaysAboveThirty() throws Exception {
        String requestBody = new ObjectMapper().writeValueAsString(
                Map.of("destination", "Hangzhou", "days", 31));

        mockMvc.perform(post("/ai/itinerary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qwenService);
    }

    @Test
    void shouldRejectItineraryPreferencesAboveTwentyFields() throws Exception {
        Map<String, Object> preferences = new LinkedHashMap<>();
        for (int index = 1; index <= 21; index++) {
            preferences.put("p" + index, true);
        }
        Map<String, Object> body = Map.of(
                "destination", "Hangzhou",
                "days", 2,
                "preferences", preferences);

        mockMvc.perform(post("/ai/itinerary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qwenService);
    }

    @Test
    void shouldBindRecommendationJsonNodePreferences() throws Exception {
        when(qwenService.chatCompletion(anyString(), anyString())).thenReturn("ai-recommendation");

        Map<String, Object> preferences = Map.of("pace", "relaxed", "avoidCrowd", true);
        Map<String, Object> body = Map.of(
                "userId", 9,
                "location", "Nanjing",
                "preferences", preferences,
                "budget", 1200,
                "duration", 2,
                "cityId", 7);

        mockMvc.perform(post("/ai/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].description").value("ai-recommendation"))
                .andExpect(jsonPath("$.data[0].source").value("qwen"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenService).chatCompletion(promptCaptor.capture(), anyString());
        assertTrue(promptCaptor.getValue().contains("relaxed"));
    }
}
