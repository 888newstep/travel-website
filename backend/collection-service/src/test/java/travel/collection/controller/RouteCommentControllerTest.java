package travel.collection.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.collection.service.RouteCommentService;
import travel.common.entity.user_community.RouteComment;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteCommentControllerTest {

    @Mock
    private RouteCommentService routeCommentService;

    @InjectMocks
    private RouteCommentController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseAuthenticatedUserWhenCreatingComment() {
        authenticate(42L);
        RouteComment request = new RouteComment();
        request.setRouteId(8);
        request.setUserId(999);
        request.setRating(4.5);
        request.setContent("good route");
        when(routeCommentService.createComment(8, 42, 4.5, "good route", null, false, null))
                .thenReturn(request);

        controller.createComment(request);

        verify(routeCommentService).createComment(
                eq(8), eq(42), eq(4.5), eq("good route"), eq(null), eq(false), eq(null));
    }

    @Test
    void shouldUseAuthenticatedUserWhenDeletingComment() {
        authenticate(42L);
        when(routeCommentService.deleteComment(8, 42)).thenReturn(true);

        controller.deleteComment(8);

        verify(routeCommentService).deleteComment(8, 42);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
