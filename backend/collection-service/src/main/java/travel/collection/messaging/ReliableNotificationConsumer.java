package travel.collection.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import travel.collection.service.NotificationService;
import travel.common.config.RabbitMQConfig;
import travel.common.exception.NonRetryableMessageException;
import travel.common.service.RabbitReliableMessageRepublisher;
import travel.common.service.RedisMessageIdempotencyService;
import travel.common.service.ReliableMessageRepublisher;
import travel.common.vo.NotificationMessageVO;
import travel.common.vo.ReliablePublishResult;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
        name = {
                "mq.reliable-notification.topology.enabled",
                "mq.reliable-notification.consumer.enabled"
        },
        havingValue = "true")
public class ReliableNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final RedisMessageIdempotencyService idempotencyService;
    private final ReliableMessageRepublisher messageRepublisher;
    private final int maxRetryCount;

    public ReliableNotificationConsumer(
            ObjectMapper objectMapper,
            NotificationService notificationService,
            RedisMessageIdempotencyService idempotencyService,
            ReliableMessageRepublisher messageRepublisher,
            @Value("${mq.reliable-notification.max-retries:3}")
            int maxRetryCount) {
        if (objectMapper == null || notificationService == null || idempotencyService == null
                || messageRepublisher == null) {
            throw new IllegalArgumentException("notification consumer dependencies cannot be null");
        }
        if (maxRetryCount < 1 || maxRetryCount > 3) {
            throw new IllegalArgumentException("maxRetryCount must be between 1 and 3");
        }
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
        this.messageRepublisher = messageRepublisher;
        this.maxRetryCount = maxRetryCount;
    }

    @RabbitListener(
            id = "reliableNotificationConsumer",
            queues = RabbitMQConfig.RELIABLE_NOTIFICATION_QUEUE,
            ackMode = "MANUAL")
    public void onMessage(Message sourceMessage, Channel channel) throws IOException {
        if (sourceMessage == null || channel == null) {
            throw new IllegalArgumentException("sourceMessage and channel cannot be null");
        }

        String messageId = extractMessageId(sourceMessage);
        if (isBlank(messageId)) {
            handleNonRetryable(sourceMessage, channel, "missing message id");
            return;
        }

        NotificationMessageVO notificationMessage;
        try {
            notificationMessage = parseMessage(sourceMessage);
        } catch (NonRetryableMessageException exception) {
            handleNonRetryable(sourceMessage, channel, exception.getMessage());
            return;
        }

        RedisMessageIdempotencyService.ClaimResult claim;
        try {
            claim = idempotencyService.tryClaim(messageId);
        } catch (Exception exception) {
            log.warn("notification idempotency claim failed, message will be requeued: messageId={}, error={}",
                    messageId, exception.getMessage());
            nackForRequeue(sourceMessage, channel);
            return;
        }

        if (claim.status() == RedisMessageIdempotencyService.ClaimStatus.COMPLETED) {
            ack(sourceMessage, channel);
            return;
        }

        if (claim.status() == RedisMessageIdempotencyService.ClaimStatus.IN_PROGRESS) {
            retryInProgress(sourceMessage, channel, messageId);
            return;
        }

        try {
            notificationService.createReliableNotification(messageId, notificationMessage);
            idempotencyService.markCompleted(claim);
        } catch (Exception exception) {
            try {
                idempotencyService.release(claim);
            } catch (Exception releaseException) {
                log.warn("notification idempotency release failed: messageId={}, error={}",
                        messageId, releaseException.getMessage());
            }
            try {
                retryOrDeadLetter(sourceMessage, channel, messageId, exception.getMessage());
            } catch (Exception republishException) {
                log.warn("notification retry publish failed, message will be requeued: messageId={}, error={}",
                        messageId, republishException.getMessage());
                nackForRequeue(sourceMessage, channel);
            }
            return;
        }
        ack(sourceMessage, channel);
    }

    private NotificationMessageVO parseMessage(Message sourceMessage) {
        try {
            if (sourceMessage.getBody() == null) {
                throw new IllegalArgumentException("notification message body cannot be null");
            }
            NotificationMessageVO message = objectMapper.readValue(
                    sourceMessage.getBody(), NotificationMessageVO.class);
            validateMessage(message);
            return message;
        } catch (IOException | IllegalArgumentException exception) {
            throw new NonRetryableMessageException("invalid notification message payload", exception);
        }
    }

    private void validateMessage(NotificationMessageVO message) {
        if (message == null || message.getUserId() == null || message.getUserId() <= 0
                || isBlank(message.getType()) || isBlank(message.getTitle())) {
            throw new NonRetryableMessageException("notification message is invalid");
        }
    }

    private void handleNonRetryable(Message sourceMessage, Channel channel, String reason) throws IOException {
        ReliablePublishResult result = messageRepublisher.publishDeadLetter(sourceMessage, reason);
        if (result.confirmed()) {
            ack(sourceMessage, channel);
        } else {
            nackForRequeue(sourceMessage, channel);
        }
    }

    private void retryInProgress(Message sourceMessage, Channel channel, String messageId) throws IOException {
        int retryCount = readRetryCount(sourceMessage);
        if (retryCount >= maxRetryCount) {
            ReliablePublishResult result = messageRepublisher.publishDeadLetter(
                    sourceMessage,
                    "message remained in progress after " + maxRetryCount + " retries: " + messageId);
            if (result.confirmed()) {
                ack(sourceMessage, channel);
            } else {
                nackForRequeue(sourceMessage, channel);
            }
            return;
        }
        ReliablePublishResult result = messageRepublisher.publishRetry(
                sourceMessage,
                retryCount + 1,
                "message is already being processed: " + messageId);
        if (result.confirmed()) {
            ack(sourceMessage, channel);
        } else {
            nackForRequeue(sourceMessage, channel);
        }
    }

    private void retryOrDeadLetter(
            Message sourceMessage,
            Channel channel,
            String messageId,
            String reason) throws IOException {
        int retryCount = readRetryCount(sourceMessage);
        ReliablePublishResult result;
        if (retryCount < maxRetryCount) {
            result = messageRepublisher.publishRetry(
                    sourceMessage,
                    retryCount + 1,
                    formatFailureReason(messageId, reason));
        } else {
            result = messageRepublisher.publishDeadLetter(
                    sourceMessage,
                    formatFailureReason(messageId, reason));
        }

        if (result.confirmed()) {
            ack(sourceMessage, channel);
        } else {
            nackForRequeue(sourceMessage, channel);
        }
    }

    private void ack(Message sourceMessage, Channel channel) throws IOException {
        channel.basicAck(deliveryTag(sourceMessage), false);
    }

    private void nackForRequeue(Message sourceMessage, Channel channel) throws IOException {
        channel.basicNack(deliveryTag(sourceMessage), false, true);
    }

    private String extractMessageId(Message sourceMessage) {
        MessageProperties properties = sourceMessage.getMessageProperties();
        if (properties == null) {
            return null;
        }
        String messageId = properties.getMessageId();
        if (!isBlank(messageId)) {
            return messageId;
        }
        Object originalMessageId = properties.getHeaders()
                .get(RabbitReliableMessageRepublisher.ORIGINAL_MESSAGE_ID_HEADER);
        return originalMessageId == null ? null : String.valueOf(originalMessageId);
    }

    private int readRetryCount(Message sourceMessage) {
        MessageProperties properties = sourceMessage.getMessageProperties();
        if (properties == null) {
            return 0;
        }
        Map<String, Object> headers = properties.getHeaders();
        Object value = headers.get(RabbitReliableMessageRepublisher.RETRY_COUNT_HEADER);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value != null) {
            try {
                return Math.max(0, Integer.parseInt(value.toString()));
            } catch (NumberFormatException ignored) {
                log.debug("invalid notification retry count: value={}", value);
            }
        }
        return 0;
    }

    private String formatFailureReason(String messageId, String reason) {
        String detail = isBlank(reason) ? "unknown notification failure" : reason;
        return "messageId=" + messageId + ", reason=" + detail;
    }

    private long deliveryTag(Message sourceMessage) {
        MessageProperties properties = sourceMessage.getMessageProperties();
        return properties == null ? 0L : properties.getDeliveryTag();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
