package travel.attraction.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import travel.attraction.dto.AttractionWarning;
import travel.common.entity.travel_realtime.AttractionRealtimeStatus;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.CacheUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttractionRealtimeStatusServiceImplTest {

    @Test
    void shouldBuildWarningsFromRealtimeStatusInsteadOfMockData() {
        AttractionRealtimeStatusMapper mapper = mock(AttractionRealtimeStatusMapper.class);
        CacheUtil cacheUtil = mock(CacheUtil.class);
        AttractionRealtimeStatusServiceImpl service = spy(
                new AttractionRealtimeStatusServiceImpl(mapper, cacheUtil));
        AttractionRealtimeStatus realtimeStatus = status(8L);
        realtimeStatus.setCrowdLevel(5);
        realtimeStatus.setWeather("大雨");
        when(cacheUtil.get(anyString(), eq(List.class))).thenReturn(null);
        doReturn(List.of(realtimeStatus)).when(service).list(any(Wrapper.class));

        List<AttractionWarning> warnings = service.getActiveWarns();

        assertEquals(2, warnings.size());
        assertEquals("crowd:8", warnings.get(0).getWarnId());
        assertEquals("严重", warnings.get(0).getWarnLevel());
        assertEquals("weather:8", warnings.get(1).getWarnId());
        verify(cacheUtil).set(eq("attraction:active_warns"), eq(warnings), eq(5L), any());
    }

    @Test
    void shouldFailBatchAtomicallyWhenDatabaseUpdateFails() {
        AttractionRealtimeStatusMapper mapper = mock(AttractionRealtimeStatusMapper.class);
        CacheUtil cacheUtil = mock(CacheUtil.class);
        AttractionRealtimeStatusServiceImpl service = spy(
                new AttractionRealtimeStatusServiceImpl(mapper, cacheUtil));
        AttractionRealtimeStatus incoming = status(8L);
        AttractionRealtimeStatus existing = status(8L);
        existing.setId(10L);
        doReturn(existing).when(service).getOne(any(Wrapper.class), eq(false));
        doReturn(false).when(service).updateById(any(AttractionRealtimeStatus.class));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.batchUpdateStatus(List.of(incoming)));

        assertEquals(5008, exception.getCode());
    }

    @Test
    void shouldPreserveRequestedOrderWhenCacheAndDatabaseResultsAreMixed() {
        AttractionRealtimeStatusMapper mapper = mock(AttractionRealtimeStatusMapper.class);
        CacheUtil cacheUtil = mock(CacheUtil.class);
        AttractionRealtimeStatusServiceImpl service = spy(
                new AttractionRealtimeStatusServiceImpl(mapper, cacheUtil));
        AttractionRealtimeStatus cached = status(1L);
        AttractionRealtimeStatus fromDatabase = status(2L);
        when(cacheUtil.get("attraction:status:1", AttractionRealtimeStatus.class)).thenReturn(cached);
        when(cacheUtil.get("attraction:status:2", AttractionRealtimeStatus.class)).thenReturn(null);
        doReturn(List.of(fromDatabase)).when(service).list(any(Wrapper.class));

        List<AttractionRealtimeStatus> result = service.getByAttractionIds(List.of(2L, 1L));

        assertEquals(List.of(2L, 1L), result.stream()
                .map(AttractionRealtimeStatus::getAttractionId)
                .toList());
    }

    private AttractionRealtimeStatus status(Long attractionId) {
        AttractionRealtimeStatus status = new AttractionRealtimeStatus();
        status.setAttractionId(attractionId);
        status.setUpdateTime(LocalDateTime.of(2026, 8, 18, 12, 0));
        status.setDeleted(0);
        return status;
    }
}
