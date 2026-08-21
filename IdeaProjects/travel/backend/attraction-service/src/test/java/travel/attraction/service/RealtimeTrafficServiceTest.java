package travel.attraction.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.AMapService;
import travel.common.utils.CacheUtil;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeTrafficServiceTest {

    @Test
    void shouldUpdateRealWeatherAndPreserveExistingCrowdData() {
        AttractionRealtimeStatusMapper mapper = mock(AttractionRealtimeStatusMapper.class);
        AMapService aMapService = mock(AMapService.class);
        CacheUtil cacheUtil = mock(CacheUtil.class);
        AttractionRealtimeStatus existing = new AttractionRealtimeStatus();
        existing.setId(10L);
        existing.setAttractionId(8L);
        existing.setCrowdCount(321);
        existing.setCrowdLevel(2);
        when(mapper.selectByAttractionId(8L)).thenReturn(existing);
        when(aMapService.getWeatherByLocation("116.4,39.9"))
                .thenReturn(Map.of("weather", "晴", "temperature", 28));
        RealtimeTrafficService service = new RealtimeTrafficService(mapper, aMapService, cacheUtil);

        service.updateAttractionRealtimeStatus(8L, 116.4, 39.9);

        ArgumentCaptor<AttractionRealtimeStatus> captor =
                ArgumentCaptor.forClass(AttractionRealtimeStatus.class);
        verify(mapper).updateById(captor.capture());
        AttractionRealtimeStatus updated = captor.getValue();
        assertEquals("晴", updated.getWeather());
        assertEquals(28, updated.getTemperature());
        assertEquals(321, updated.getCrowdCount());
        assertEquals(2, updated.getCrowdLevel());
    }

    @Test
    void shouldFailWithoutWritingWhenWeatherProviderReturnsNoData() {
        AttractionRealtimeStatusMapper mapper = mock(AttractionRealtimeStatusMapper.class);
        AMapService aMapService = mock(AMapService.class);
        CacheUtil cacheUtil = mock(CacheUtil.class);
        when(aMapService.getWeather("110000")).thenReturn(null);
        RealtimeTrafficService service = new RealtimeTrafficService(mapper, aMapService, cacheUtil);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateAttractionRealtimeStatus(8L, null, null));

        assertEquals(20001, exception.getCode());
        verify(mapper, never()).insert(
                org.mockito.ArgumentMatchers.<AttractionRealtimeStatus>any());
        verify(mapper, never()).updateById(
                org.mockito.ArgumentMatchers.<AttractionRealtimeStatus>any());
    }
}
