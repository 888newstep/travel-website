package travel.route.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import travel.common.exception.GlobalExceptionHandler;
import travel.route.service.BaiduAIService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIImageControllerTest {

    private BaiduAIService baiduAIService;
    private MockMvc mockMvc;
    private AIImageController controller;

    @BeforeEach
    void setUp() {
        baiduAIService = mock(BaiduAIService.class);
        controller = new AIImageController(baiduAIService, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRejectRemoteImageWhenAllowlistIsEmpty() throws Exception {
        ReflectionTestUtils.setField(controller, "allowedImageHosts", "");

        mockMvc.perform(post("/ai/image-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"https://cdn.example.com/a.jpg","analysisType":"scene"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4006));

        verifyNoInteractions(baiduAIService);
    }

    @Test
    void shouldRejectPrivateAddressEvenWhenHostIsAllowlisted() throws Exception {
        ReflectionTestUtils.setField(controller, "allowedImageHosts", "127.0.0.1");

        mockMvc.perform(post("/ai/image-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"http://127.0.0.1/internal.jpg","analysisType":"scene"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4006));

        verifyNoInteractions(baiduAIService);
    }
}
