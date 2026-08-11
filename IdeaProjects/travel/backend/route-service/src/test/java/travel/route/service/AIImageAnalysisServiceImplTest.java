package travel.route.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import travel.common.utils.CacheUtil;
import travel.route.dto.ai.AIImageAnalysisResponse;
import travel.route.dto.ai.AIRecognizeAttractionResponse;
import travel.route.service.impl.AIImageAnalysisServiceImpl;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIImageAnalysisServiceImplTest {

    @Mock
    private CacheUtil cacheUtil;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private AIImageAnalysisServiceImpl aiImageAnalysisService;

    @Test
    void shouldReturnFailureResponseWhenAnalyzeImageThrows() throws IOException {
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        AIImageAnalysisResponse response = aiImageAnalysisService.analyzeImage(file, null);

        assertFalse(response.getSuccess());
        assertEquals("read failed", response.getError());
    }

    @Test
    void shouldReturnFailureResponseWhenRecognizeAttractionThrows() throws IOException {
        when(file.getBytes()).thenThrow(new IOException("bad image"));

        AIRecognizeAttractionResponse response = aiImageAnalysisService.recognizeAttraction(file);

        assertFalse(response.getSuccess());
        assertEquals("bad image", response.getError());
    }

    @Test
    void shouldReturnStructuredAnalysisResponseWhenImageReadSucceeds() throws IOException {
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cacheUtil.get(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Object.class))).thenReturn(null);

        AIImageAnalysisResponse response = aiImageAnalysisService.analyzeImage(file, null);

        assertTrue(response.getSuccess());
        assertEquals("comprehensive", response.getAnalysisType());
        assertEquals(2, response.getRecommendations().size());
    }
}