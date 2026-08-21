package travel.route.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import travel.common.exception.BusinessException;
import travel.common.utils.AICacheManager;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QwenServiceTest {

    private AICacheManager cacheManager;
    private QwenService qwenService;

    @BeforeEach
    void setUp() {
        cacheManager = mock(AICacheManager.class);
        qwenService = new QwenService(cacheManager);
    }

    @Test
    void shouldFailBeforeReadingCacheWhenServiceIsDisabled() {
        ReflectionTestUtils.setField(qwenService, "enabled", false);
        ReflectionTestUtils.setField(qwenService, "apiKey", "configured-key");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> qwenService.chatCompletion("hello", "system"));

        assertEquals(5006, exception.getCode());
        verifyNoInteractions(cacheManager);
    }

    @Test
    void shouldFailBeforeReadingCacheWhenApiKeyIsBlank() {
        ReflectionTestUtils.setField(qwenService, "enabled", true);
        ReflectionTestUtils.setField(qwenService, "apiKey", "  ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> qwenService.recommendItinerary("Hangzhou", "food", 2, "2000"));

        assertEquals(5006, exception.getCode());
        verifyNoInteractions(cacheManager);
    }

    @Test
    void shouldUseDestinationPreferencesDaysAndBudgetForItineraryCache() {
        ReflectionTestUtils.setField(qwenService, "enabled", true);
        ReflectionTestUtils.setField(qwenService, "apiKey", "configured-key");
        when(cacheManager.getOrSetItineraryCache(
                eq("Hangzhou"), eq(2), eq("food"), eq("2000"), any()))
                .thenReturn("cached-itinerary");

        String result = qwenService.recommendItinerary("Hangzhou", "food", 2, "2000");

        assertEquals("cached-itinerary", result);
        verify(cacheManager).getOrSetItineraryCache(
                eq("Hangzhou"), eq(2), eq("food"), eq("2000"),
                org.mockito.ArgumentMatchers.<Supplier<String>>any());
    }
}
