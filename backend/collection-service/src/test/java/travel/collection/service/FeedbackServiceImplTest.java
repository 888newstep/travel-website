package travel.collection.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.collection.service.impl.FeedbackServiceImpl;
import travel.common.exception.BusinessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class FeedbackServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectFeedbackReplyFromRegularUser() {
        authenticate("ROLE_USER");
        FeedbackServiceImpl service = new FeedbackServiceImpl(mock(UserService.class));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.replyFeedback(1L, "reply"));

        assertEquals(28001, exception.getCode());
    }

    @Test
    void shouldAllowAdminToEnterFeedbackReplyFlow() {
        authenticate("ROLE_ADMIN");
        FeedbackServiceImpl service = spy(new FeedbackServiceImpl(mock(UserService.class)));
        doReturn(null).when(service).getById(1L);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.replyFeedback(1L, "reply"));

        assertEquals(25001, exception.getCode());
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        42L, null, List.of(new SimpleGrantedAuthority(role))));
    }
}
