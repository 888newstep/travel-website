package travel.collection.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.collection.dto.UserStatisticsResponse;
import travel.collection.service.impl.UserStatisticsServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserStatisticsServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnFrontendStatisticsContract() {
        authenticate(42L);
        TravelNoteService travelNoteService = mock(TravelNoteService.class);
        RouteCollectionService routeCollectionService = mock(RouteCollectionService.class);
        RouteShareService routeShareService = mock(RouteShareService.class);
        when(travelNoteService.countByUserId(42)).thenReturn(3);
        when(routeCollectionService.countByUserId(42)).thenReturn(4L);
        when(routeShareService.countByUserId(42)).thenReturn(5L);
        UserStatisticsServiceImpl service = new UserStatisticsServiceImpl(
                travelNoteService, routeCollectionService, routeShareService);

        UserStatisticsResponse result = service.getCurrentUserStats();

        assertEquals(3, result.totalNotes());
        assertEquals(4, result.totalCollections());
        assertEquals(5, result.totalShares());
    }

    @Test
    void shouldPropagateStatisticsDependencyFailure() {
        authenticate(42L);
        TravelNoteService travelNoteService = mock(TravelNoteService.class);
        when(travelNoteService.countByUserId(42)).thenThrow(new IllegalStateException("database unavailable"));
        UserStatisticsServiceImpl service = new UserStatisticsServiceImpl(
                travelNoteService, mock(RouteCollectionService.class), mock(RouteShareService.class));

        assertThrows(IllegalStateException.class, service::getCurrentUserStats);
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
