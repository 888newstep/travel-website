package travel.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import travel.common.config.RabbitMQConfig;
import travel.common.vo.ReliablePublishResult;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 可靠消息转发器。
 *
 * <p>原消息只有在目标交换机返回 publisher confirm ack 且没有 returned message
 * 时才允许由消费者确认，避免“先 ack 原消息、后发现重试消息未进入 broker”的丢失窗口。</p>
 */
@Slf4j
@Service
public class RabbitReliableMessageRepublisher implements ReliableMessageRepublisher {

    public static final String RETRY_COUNT_HEADER = "x-travel-retry-count";
    public static final String FAILURE_REASON_HEADER = "x-travel-failure-reason";
    public static final String ORIGINAL_MESSAGE_ID_HEADER = "x-travel-original-message-id";

    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;
    private final int maxRetryCount;

    @Autowired
    public RabbitReliableMessageRepublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${mq.reliable-notification.confirm-timeout-ms:5000}") long confirmTimeoutMillis,
            @Value("${mq.reliable-notification.max-retries:3}") int maxRetryCount) {
        this.rabbitTemplate = rabbitTemplate;
        if (confirmTimeoutMillis <= 0) {
            throw new IllegalArgumentException("confirmTimeoutMillis must be positive");
        }
        if (maxRetryCount < 1 || maxRetryCount > 3) {
            throw new IllegalArgumentException("maxRetryCount must be between 1 and 3");
        }
        this.confirmTimeout = Duration.ofMillis(confirmTimeoutMillis);
        this.maxRetryCount = maxRetryCount;
    }

    public RabbitReliableMessageRepublisher(RabbitTemplate rabbitTemplate, Duration confirmTimeout) {
        this(rabbitTemplate, confirmTimeout, 3);
    }

    public RabbitReliableMessageRepublisher(
            RabbitTemplate rabbitTemplate,
            Duration confirmTimeout,
            int maxRetryCount) {
        this.rabbitTemplate = rabbitTemplate;
        if (confirmTimeout == null || confirmTimeout.isZero() || confirmTimeout.isNegative()) {
            throw new IllegalArgumentException("confirmTimeout must be positive");
        }
        if (maxRetryCount < 1 || maxRetryCount > 3) {
            throw new IllegalArgumentException("maxRetryCount must be between 1 and 3");
        }
        this.confirmTimeout = confirmTimeout;
        this.maxRetryCount = maxRetryCount;
    }

    @Override
    public ReliablePublishResult publishRetry(Message sourceMessage, int retryCount, String reason) {
        if (retryCount < 1 || retryCount > maxRetryCount) {
            throw new IllegalArgumentException("retryCount must be between 1 and " + maxRetryCount);
        }
        String routingKey = switch (retryCount) {
            case 1 -> RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_1_ROUTING_KEY;
            case 2 -> RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_2_ROUTING_KEY;
            case 3 -> RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_3_ROUTING_KEY;
            default -> throw new IllegalArgumentException("unsupported retryCount=" + retryCount);
        };
        return publish(
                sourceMessage,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_EXCHANGE,
                routingKey,
                retryCount,
                reason);
    }

    @Override
    public ReliablePublishResult publishDeadLetter(Message sourceMessage, String reason) {
        int currentRetryCount = readRetryCount(sourceMessage);
        return publish(
                sourceMessage,
                RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_EXCHANGE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_ROUTING_KEY,
                currentRetryCount,
                reason);
    }

    private ReliablePublishResult publish(
            Message sourceMessage,
            String exchange,
            String routingKey,
            int retryCount,
            String reason) {
        if (sourceMessage == null) {
            throw new IllegalArgumentException("sourceMessage cannot be null");
        }

        String originalMessageId = sourceMessage.getMessageProperties() == null
                ? null
                : sourceMessage.getMessageProperties().getMessageId();
        String publishId = "reliable-republish-" + UUID.randomUUID();
        Message outboundMessage = MessageBuilder.fromClonedMessage(sourceMessage)
                .setMessageId(originalMessageId)
                .setHeader(ORIGINAL_MESSAGE_ID_HEADER, originalMessageId)
                .setHeader(RETRY_COUNT_HEADER, retryCount)
                .setHeader(FAILURE_REASON_HEADER, truncate(reason))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                // deliveryTag 属于消费端通道状态，不能把原消息的投递标签带入新消息。
                // Spring AMQP 3.x 的 setDeliveryTag 参数是 primitive long，传 null 会在运行时拆箱 NPE。
                .setDeliveryTag(0L)
                .setRedelivered(false)
                .build();

        CorrelationData correlationData = new CorrelationData(publishId);
        try {
            rabbitTemplate.send(exchange, routingKey, outboundMessage, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.isAck()) {
                String cause = confirm == null ? "missing publisher confirm" : confirm.getReason();
                return ReliablePublishResult.failure(publishId, "publisher nack: " + cause);
            }
            if (correlationData.getReturned() != null) {
                return ReliablePublishResult.failure(
                        publishId,
                        "message returned by broker: " + correlationData.getReturned().getReplyText());
            }
            return ReliablePublishResult.success(publishId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ReliablePublishResult.failure(publishId, "publisher confirm interrupted");
        } catch (Exception e) {
            log.warn(
                    "可靠消息转发未确认: publishId={}, exchange={}, routingKey={}, error={}",
                    publishId,
                    exchange,
                    routingKey,
                    e.getMessage());
            return ReliablePublishResult.failure(publishId, formatError(e));
        }
    }

    private int readRetryCount(Message message) {
        if (message == null || message.getMessageProperties() == null) {
            return 0;
        }
        Object header = message.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);
        if (header instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (header != null) {
            try {
                return Math.max(0, Integer.parseInt(header.toString()));
            } catch (NumberFormatException ignored) {
                log.debug("无法解析可靠消息重试次数: value={}", header);
            }
        }
        return 0;
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "unknown failure";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private String formatError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + truncate(message);
    }
}
