package travel.common.utils;

import org.junit.jupiter.api.Test;
import travel.common.exception.BusinessException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThirdApiUtilTest {

    @Test
    void shouldRejectRealtimeSyncWhenProviderIsNotConfigured() {
        ThirdApiUtil thirdApiUtil = new ThirdApiUtil(mock(AMapService.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> thirdApiUtil.getRealtimeData(List.of()));

        assertEquals(20006, exception.getCode());
    }

    @Test
    void shouldRejectSmsInsteadOfReturningSimulatedSuccess() {
        ThirdApiUtil thirdApiUtil = new ThirdApiUtil(mock(AMapService.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> thirdApiUtil.sendSmsNotification("13800000000", "{}", "WARN"));

        assertEquals(13001, exception.getCode());
    }

    @Test
    void shouldUseAmapForTrafficLookup() {
        AMapService aMapService = mock(AMapService.class);
        when(aMapService.drivingRoute(116.30, 39.90, 116.40, 39.95))
                .thenReturn(Map.of("distance", 12000, "duration", 1800));
        ThirdApiUtil thirdApiUtil = new ThirdApiUtil(aMapService);

        Map<String, Object> result = thirdApiUtil.getTransportData(
                "116.30,39.90", "116.40,39.95");

        assertEquals("amap", result.get("source"));
        assertEquals(12000, result.get("distance"));
        verify(aMapService).drivingRoute(116.30, 39.90, 116.40, 39.95);
    }

    @Test
    void shouldRejectInvalidTrafficCoordinates() {
        ThirdApiUtil thirdApiUtil = new ThirdApiUtil(mock(AMapService.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> thirdApiUtil.getTransportData("from", "to"));

        assertEquals(4002, exception.getCode());
    }
}
