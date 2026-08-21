package travel.route.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.exception.GlobalExceptionHandler;
import travel.route.dto.ai.AIAskQuestionResponse;
import travel.route.dto.ai.AIOptimizeRouteResponse;
import travel.route.dto.ai.AIOptimizeSuggestion;
import travel.route.service.AIAssistantService;
import travel.route.service.QwenService;
import travel.route.service.RouteService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AIAssistantControllerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @Mock
    private QwenService qwenService;

    @Mock
    private AIAssistantService aiAssistantService;

    @Mock
    private RouteService routeService;

    @InjectMocks
    private AIAssistantController controller;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, null, List.of()));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void shouldBindTypedAskRequestAndReturnTypedResponse() throws Exception {
        AIAskQuestionResponse response = AIAskQuestionResponse.builder()
                .question("How long should I stay?")
                .answer("Three days is a reasonable starting point.")
                .confidence(null)
                .timestamp(LocalDateTime.of(2026, 8, 11, 10, 0))
                .source("qwen")
                .build();
        when(aiAssistantService.askQuestion("How long should I stay?", 7)).thenReturn(response);

        mockMvc.perform(post("/ai/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new AskRequest("How long should I stay?", 7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.question").value("How long should I stay?"))
                .andExpect(jsonPath("$.data.answer").value("Three days is a reasonable starting point."))
                .andExpect(jsonPath("$.data.source").value("qwen"));

        verify(aiAssistantService).askQuestion(eq("How long should I stay?"), eq(7));
    }

    @Test
    void shouldRejectBlankAskQuestionBeforeCallingService() throws Exception {
        mockMvc.perform(post("/ai/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new AskRequest(" ", 7))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiAssistantService);
    }

    @Test
    void shouldReturnServiceUnavailableWhenAssistantProviderFails() throws Exception {
        when(aiAssistantService.askQuestion("provider failure", 7))
                .thenThrow(new BusinessException(ErrorCodeEnum.SYSTEM_DEPENDENCY_ERROR));

        mockMvc.perform(post("/ai/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new AskRequest("provider failure", 7))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(5006));
    }

    @Test
    void shouldKeepLegacyPostOptimizeRouteAndNewAlias() throws Exception {
        AIOptimizeRouteResponse response = AIOptimizeRouteResponse.builder()
                .success(true)
                .routeId(12)
                .suggestions(List.of(new AIOptimizeSuggestion("transport", "Use metro")))
                .optimizedScore(null)
                .source("qwen")
                .build();
        when(aiAssistantService.optimizeRouteByAI(12)).thenReturn(response);

        mockMvc.perform(post("/ai/assistant/optimize/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeId").value(12))
                .andExpect(jsonPath("$.data.suggestions[0].type").value("transport"))
                .andExpect(jsonPath("$.data.source").value("qwen"));

        mockMvc.perform(get("/ai/assistant/optimize-route/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeId").value(12));
    }

    private record AskRequest(String question, Integer userId) {
    }
}
