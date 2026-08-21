package travel.collection.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.collection.service.RouteShareService;
import travel.common.entity.user_community.RouteShare;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteShareControllerTest {

    @Mock
    private RouteShareService routeShareService;

    @InjectMocks
    private RouteShareController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreClientUserAndItemTypeWhenGeneratingRouteShare() {
        authenticate(42L);
        when(routeShareService.generateShareCode(any(RouteShare.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        controller.generateShareCode(Map.of(
                "itemId", 8,
                "itemType", "route",
                "userId", 999));

        ArgumentCaptor<RouteShare> captor = ArgumentCaptor.forClass(RouteShare.class);
        verify(routeShareService).generateShareCode(captor.capture());
        assertEquals(42, captor.getValue().getUserId());
        assertEquals(8, captor.getValue().getRouteId());
        assertEquals("route", captor.getValue().getItemType());
    }

    @Test
    void shouldScopeShareCancellationToAuthenticatedUser() {
        authenticate(42L);
        when(routeShareService.cancelShare(8, 42)).thenReturn(true);

        controller.cancelShare(8L);

        verify(routeShareService).cancelShare(8, 42);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
