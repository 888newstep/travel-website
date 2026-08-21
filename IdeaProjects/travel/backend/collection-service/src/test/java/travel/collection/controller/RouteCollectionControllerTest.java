package travel.collection.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.collection.service.RouteCollectionService;
import travel.common.utils.Result;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteCollectionControllerTest {

    @Mock
    private RouteCollectionService routeCollectionService;

    @InjectMocks
    private RouteCollectionController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseAuthenticatedUserWhenCheckingCollection() {
        authenticate(42L);
        when(routeCollectionService.isCollected(8, 42)).thenReturn(true);

        Result<Boolean> result = controller.checkCollected(8);

        assertTrue(result.getData());
        verify(routeCollectionService).isCollected(8, 42);
    }

    @Test
    void shouldUseAuthenticatedUserWhenTogglingCollection() {
        authenticate(42L);
        RouteCollectionController.CollectRequest request = new RouteCollectionController.CollectRequest();
        request.setRouteId(8);
        when(routeCollectionService.toggleCollection(8, 42)).thenReturn(true);

        Result<Map<String, Object>> result = controller.toggleCollection(request);

        assertTrue((Boolean) result.getData().get("collected"));
        verify(routeCollectionService).toggleCollection(8, 42);
    }

    @Test
    void shouldScopeBatchRemovalToAuthenticatedUser() {
        authenticate(42L);
        when(routeCollectionService.batchRemoveCollections(List.of(1, 2), 42)).thenReturn(2);

        controller.batchRemoveCollections(List.of(1, 2));

        verify(routeCollectionService).batchRemoveCollections(List.of(1, 2), 42);
    }

    @Test
    void shouldRemoveCollectionIdempotentlyForAuthenticatedUser() {
        authenticate(42L);
        when(routeCollectionService.uncollectRoute(8, 42)).thenReturn(true);

        Result<Boolean> result = controller.removeCollection(8);

        assertTrue(result.getData());
        verify(routeCollectionService).uncollectRoute(8, 42);
        verify(routeCollectionService, never()).isCollected(8, 42);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
