package travel.attraction.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.attraction.service.AttractionRealtimeStatusService;
import travel.attraction.service.RealtimeTrafficService;
import travel.common.exception.BusinessException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RealtimeStatusControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectOversizedReadBatchBeforeServiceCall() {
        AttractionRealtimeStatusService statusService = mock(AttractionRealtimeStatusService.class);
        RealtimeTrafficService trafficService = mock(RealtimeTrafficService.class);
        RealtimeStatusController controller = new RealtimeStatusController(statusService, trafficService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getBatchRealtimeStatus(Collections.nCopies(101, 1L)));

        assertEquals(4000, exception.getCode());
        verifyNoInteractions(statusService, trafficService);
    }

    @Test
    void shouldRejectIncompleteCoordinatePairBeforeServiceCall() {
        authenticateAdmin();
        AttractionRealtimeStatusService statusService = mock(AttractionRealtimeStatusService.class);
        RealtimeTrafficService trafficService = mock(RealtimeTrafficService.class);
        RealtimeStatusController controller = new RealtimeStatusController(statusService, trafficService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.updateAttractionStatus(Map.of(
                        "attractionId", 8,
                        "longitude", 116)));

        assertEquals(4000, exception.getCode());
        verifyNoInteractions(statusService, trafficService);
    }

    @Test
    void shouldReportHistoricalAggregatesAsUnavailableWithoutHistoryTable() {
        AttractionRealtimeStatusService statusService = mock(AttractionRealtimeStatusService.class);
        RealtimeTrafficService trafficService = mock(RealtimeTrafficService.class);
        RealtimeStatusController controller = new RealtimeStatusController(statusService, trafficService);

        BusinessException historicalException = assertThrows(
                BusinessException.class, () -> controller.getHistoricalAvgCrowdCount(8L));
        BusinessException sevenDayException = assertThrows(
                BusinessException.class, () -> controller.get7DaysAvgCrowdCount(8L));

        assertEquals(20007, historicalException.getCode());
        assertEquals(20007, sevenDayException.getCode());
        verifyNoInteractions(statusService, trafficService);
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        42L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
