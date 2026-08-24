package travel.collection.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.collection.service.NotificationService;
import travel.common.entity.user_community.Notification;
import travel.common.exception.BusinessException;
import travel.common.utils.Result;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void shouldReturnTypedNotificationList() {
        Notification notification = new Notification();
        notification.setId(8);
        when(notificationService.getCurrentUserNotifications(1, 20))
                .thenReturn(List.of(notification));

        Result<List<Notification>> result = controller.getNotifications(1, 20);

        assertTrue(result.isSuccess());
        assertEquals(8, result.getData().get(0).getId());
    }

    @Test
    void shouldRejectFailedReadUpdate() {
        when(notificationService.markAsRead(8, null)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> controller.markAsRead(8));

        assertEquals(26003, exception.getCode());
    }

    @Test
    void shouldReturnBooleanForSuccessfulDelete() {
        when(notificationService.deleteNotification(8, null)).thenReturn(true);

        Result<Boolean> result = controller.deleteNotification(8);

        assertTrue(result.getData());
    }
}
