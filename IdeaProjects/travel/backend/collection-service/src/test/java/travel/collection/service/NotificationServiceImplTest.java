package travel.collection.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.collection.service.impl.NotificationServiceImpl;
import travel.common.entity.user_community.Notification;
import travel.common.exception.BusinessException;
import travel.common.utils.ThirdApiUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificationServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreCallerUserIdWhenMarkingOwnNotificationAsRead() {
        authenticate(42L);
        Notification notification = notification(8, 42);
        NotificationServiceImpl service = spy(createService());
        doReturn(notification).when(service).getById(8);
        doReturn(true).when(service).updateById(any(Notification.class));

        assertTrue(service.markAsRead(8, null));
        assertTrue(notification.getIsRead());
    }

    @Test
    void shouldRejectAccessToAnotherUsersNotification() {
        authenticate(42L);
        NotificationServiceImpl service = spy(createService());
        doReturn(notification(8, 7)).when(service).getById(8);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.deleteNotification(8, 7));

        assertEquals(28001, exception.getCode());
    }

    @Test
    void shouldRejectMissingPhoneBeforeCallingSmsProvider() {
        ThirdApiUtil thirdApiUtil = mock(ThirdApiUtil.class);
        NotificationServiceImpl service = new NotificationServiceImpl(
                thirdApiUtil, mock(UserService.class));

        assertThrows(BusinessException.class,
                () -> service.sendWarnNotification(null, "sunny", 1, 8L, 9L));

        verifyNoInteractions(thirdApiUtil);
    }

    private NotificationServiceImpl createService() {
        return new NotificationServiceImpl(
                mock(ThirdApiUtil.class), mock(UserService.class));
    }

    private Notification notification(Integer id, Integer userId) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setIsRead(false);
        return notification;
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
