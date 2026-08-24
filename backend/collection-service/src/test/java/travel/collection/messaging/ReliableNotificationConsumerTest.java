package travel.collection.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import travel.collection.service.NotificationService;
import travel.common.service.ReliableMessageRepublisher;
import travel.common.service.RedisMessageIdempotencyService;
import travel.common.vo.NotificationMessageVO;
import travel.common.vo.ReliablePublishResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReliableNotificationConsumerTest {

    private static final long DELIVERY_TAG = 42L;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RedisMessageIdempotencyService idempotencyService;

    @Mock
    private ReliableMessageRepublisher messageRepublisher;

    @Mock
    private Channel channel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReliableNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ReliableNotificationConsumer(
                objectMapper,
                notificationService,
                idempotencyService,
                messageRepublisher,
                3);
    }

    @Test
    void shouldAckAlreadyCompletedMessageWithoutWritingAgain() throws Exception {
        Message message = message("message-1", validNotification());
        when(idempotencyService.tryClaim("message-1"))
                .thenReturn(claim(RedisMessageIdempotencyService.ClaimStatus.COMPLETED));

        consumer.onMessage(message, channel);

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(notificationService, never()).createReliableNotification(anyString(), any());
        verify(messageRepublisher, never()).publishRetry(any(), eq(1), anyString());
    }

    @Test
    void shouldPersistClaimedMessageThenAck() throws Exception {
        Message message = message("message-1", validNotification());
        RedisMessageIdempotencyService.ClaimResult claim =
                claim(RedisMessageIdempotencyService.ClaimStatus.CLAIMED);
        when(idempotencyService.tryClaim("message-1")).thenReturn(claim);

        consumer.onMessage(message, channel);

        verify(notificationService).createReliableNotification(eq("message-1"), any(NotificationMessageVO.class));
        verify(idempotencyService).markCompleted(claim);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void shouldRepublishInProgressMessageAndAckOriginal() throws Exception {
        Message message = message("message-1", validNotification());
        when(idempotencyService.tryClaim("message-1"))
                .thenReturn(claim(RedisMessageIdempotencyService.ClaimStatus.IN_PROGRESS));
        when(messageRepublisher.publishRetry(any(), eq(1), anyString()))
                .thenReturn(ReliablePublishResult.success("retry-1"));

        consumer.onMessage(message, channel);

        verify(messageRepublisher).publishRetry(eq(message), eq(1), anyString());
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(notificationService, never()).createReliableNotification(anyString(), any());
    }

    @Test
    void shouldReleaseClaimAndRetryWhenBusinessWriteFails() throws Exception {
        Message message = message("message-1", validNotification());
        RedisMessageIdempotencyService.ClaimResult claim =
                claim(RedisMessageIdempotencyService.ClaimStatus.CLAIMED);
        when(idempotencyService.tryClaim("message-1")).thenReturn(claim);
        doThrow(new IllegalStateException("database unavailable"))
                .when(notificationService)
                .createReliableNotification(eq("message-1"), any(NotificationMessageVO.class));
        when(messageRepublisher.publishRetry(any(), eq(1), anyString()))
                .thenReturn(ReliablePublishResult.success("retry-1"));

        consumer.onMessage(message, channel);

        verify(idempotencyService).release(claim);
        verify(messageRepublisher).publishRetry(eq(message), eq(1), anyString());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void shouldDeadLetterMalformedMessageAndAckOriginal() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("message-1");
        properties.setDeliveryTag(DELIVERY_TAG);
        Message message = new Message("not-json".getBytes(), properties);
        when(messageRepublisher.publishDeadLetter(any(), anyString()))
                .thenReturn(ReliablePublishResult.success("dead-letter-1"));

        consumer.onMessage(message, channel);

        verify(messageRepublisher).publishDeadLetter(eq(message), anyString());
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(idempotencyService, never()).tryClaim(anyString());
        verify(notificationService, never()).createReliableNotification(anyString(), any());
    }

    private NotificationMessageVO validNotification() {
        return new NotificationMessageVO(
                7,
                "NOTICE",
                "title",
                "content",
                Map.of("redirectUrl", "/notifications"),
                1L);
    }

    private Message message(String messageId, NotificationMessageVO body) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        properties.setDeliveryTag(DELIVERY_TAG);
        return new Message(objectMapper.writeValueAsBytes(body), properties);
    }

    private RedisMessageIdempotencyService.ClaimResult claim(
            RedisMessageIdempotencyService.ClaimStatus status) {
        return new RedisMessageIdempotencyService.ClaimResult(status, "key", "token");
    }
}
