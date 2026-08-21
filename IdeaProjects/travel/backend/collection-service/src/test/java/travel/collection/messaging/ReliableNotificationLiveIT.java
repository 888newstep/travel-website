package travel.collection.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import travel.collection.CollectionServiceApplication;
import travel.common.config.RabbitMQConfig;
import travel.common.service.MessageProducerService;
import travel.common.service.RabbitReliableMessageRepublisher;
import travel.common.service.RedisMessageIdempotencyService;
import travel.common.vo.NotificationMessageVO;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = CollectionServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.cloud.sentinel.enabled=false",
                "spring.main.banner-mode=off",
                "spring.sql.init.mode=never",
                "seata.enabled=false",
                "mq.status-persistence.enabled=true",
                "mq.reliable-notification.topology.enabled=true",
                "mq.reliable-notification.producer.enabled=true",
                "mq.reliable-notification.consumer.enabled=true",
                "mq.reliable-notification.max-retries=1",
                "mq.reliable-notification.confirm-timeout-ms=10000",
                "jwt.secret=reliable-notification-live-test-secret-32-bytes"
        })
class ReliableNotificationLiveIT {

    private static final Duration DATABASE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    private MessageProducerService messageProducerService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisMessageIdempotencyService idempotencyService;

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void shouldVerifyPublisherConsumerIdempotencyRetryAndDeadLetter() throws Exception {
        String runId = UUID.randomUUID().toString().replace("-", "");
        String retryTapQueue = "travel.live.retry.tap." + runId;
        String deadLetterTapQueue = "travel.live.dlq.tap." + runId;
        Integer validUserId = requiredUserId();
        Integer missingUserId = missingUserId();
        String validTitle = "RabbitMQ live " + runId;
        String malformedMessageId = "mq-live-malformed-" + runId;
        String retryMessageId = "mq-live-retry-" + runId;
        String validMessageId = null;

        declareTapQueue(
                retryTapQueue,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_EXCHANGE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_1_ROUTING_KEY);
        declareTapQueue(
                deadLetterTapQueue,
                RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_EXCHANGE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_ROUTING_KEY);

        try {
            messageProducerService.sendNotification(
                    validUserId,
                    "system",
                    validTitle,
                    "Reliable notification live verification");

            String consumedMessageId = awaitValue(
                    () -> findString(
                            "SELECT source_message_id FROM notification WHERE title = ? ORDER BY id DESC LIMIT 1",
                            validTitle),
                    value -> value != null && !value.isBlank(),
                    DATABASE_TIMEOUT,
                    "notification was not consumed");
            validMessageId = consumedMessageId;
            String confirmedStatus = awaitValue(
                    () -> findString(
                            "SELECT status FROM mq_message_status WHERE message_id = ?",
                            consumedMessageId),
                    "CONFIRMED"::equals,
                    DATABASE_TIMEOUT,
                    "publisher confirm was not persisted");
            String redisState = awaitValue(
                    () -> redisTemplate.opsForValue().get(idempotencyService.keyFor(consumedMessageId)),
                    RedisMessageIdempotencyService.COMPLETED::equals,
                    DATABASE_TIMEOUT,
                    "consumer idempotency state was not completed");

            System.out.printf(
                    "RABBIT_LIVE_RESULT scenario=publish-consume messageId=%s userId=%d "
                            + "status=%s redisState=%s databaseCount=%d%n",
                    consumedMessageId,
                    validUserId,
                    confirmedStatus,
                    redisState,
                    notificationCount(consumedMessageId));

            publishConfirmed(
                    notification(validUserId, validTitle),
                    consumedMessageId,
                    "duplicate-" + runId);
            Thread.sleep(1_000L);
            awaitValue(
                    () -> notificationCount(consumedMessageId),
                    count -> count == 1,
                    Duration.ofSeconds(10),
                    "duplicate notification was not idempotent");
            assertEquals(RedisMessageIdempotencyService.COMPLETED,
                    redisTemplate.opsForValue().get(idempotencyService.keyFor(consumedMessageId)));
            System.out.printf(
                    "RABBIT_LIVE_RESULT scenario=duplicate messageId=%s databaseCount=%d redisState=%s%n",
                    consumedMessageId,
                    notificationCount(consumedMessageId),
                    RedisMessageIdempotencyService.COMPLETED);

            publishRawConfirmed(
                    ("{\"userId\":" + validUserId
                            + ",\"type\":\"system\",\"content\":\"missing title\"}")
                            .getBytes(StandardCharsets.UTF_8),
                    malformedMessageId,
                    "malformed-" + runId);
            Message malformedDeadLetter = receiveRequired(
                    deadLetterTapQueue,
                    MESSAGE_TIMEOUT,
                    "malformed notification did not reach DLQ");
            assertEquals(malformedMessageId, malformedDeadLetter.getMessageProperties().getMessageId());
            assertTrue(headerText(malformedDeadLetter,
                    RabbitReliableMessageRepublisher.FAILURE_REASON_HEADER).contains("invalid"));
            System.out.printf(
                    "RABBIT_LIVE_RESULT scenario=non-retryable-dlq messageId=%s reason=%s%n",
                    malformedMessageId,
                    compact(headerText(
                            malformedDeadLetter,
                            RabbitReliableMessageRepublisher.FAILURE_REASON_HEADER)));

            publishConfirmed(
                    notification(missingUserId, "RabbitMQ retry " + runId),
                    retryMessageId,
                    "retry-" + runId);
            Message retryMessage = receiveRequired(
                    retryTapQueue,
                    MESSAGE_TIMEOUT,
                    "failed notification did not reach retry queue");
            assertEquals(retryMessageId, retryMessage.getMessageProperties().getMessageId());
            assertEquals(1, retryCount(retryMessage));

            Message retriedDeadLetter = receiveRequired(
                    deadLetterTapQueue,
                    MESSAGE_TIMEOUT,
                    "retried notification did not reach DLQ");
            assertEquals(retryMessageId, retriedDeadLetter.getMessageProperties().getMessageId());
            assertEquals(1, retryCount(retriedDeadLetter));
            assertEquals(0, notificationCount(retryMessageId));
            System.out.printf(
                    "RABBIT_LIVE_RESULT scenario=retry-dlq messageId=%s retryCount=%d "
                            + "databaseCount=%d reason=%s%n",
                    retryMessageId,
                    retryCount(retriedDeadLetter),
                    notificationCount(retryMessageId),
                    compact(headerText(
                            retriedDeadLetter,
                            RabbitReliableMessageRepublisher.FAILURE_REASON_HEADER)));
        } finally {
            amqpAdmin.deleteQueue(retryTapQueue);
            amqpAdmin.deleteQueue(deadLetterTapQueue);
            cleanup(validMessageId, malformedMessageId, retryMessageId, validTitle);
        }
    }

    private void declareTapQueue(String queueName, String exchangeName, String routingKey) {
        Queue queue = QueueBuilder.nonDurable(queueName)
                .exclusive()
                .autoDelete()
                .build();
        assertNotNull(amqpAdmin.declareQueue(queue));
        DirectExchange exchange = new DirectExchange(exchangeName, true, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        amqpAdmin.declareBinding(binding);
    }

    private void publishConfirmed(Object payload, String messageId, String correlationId) throws Exception {
        CorrelationData correlationData = new CorrelationData(correlationId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY,
                payload,
                message -> {
                    MessageProperties properties = message.getMessageProperties();
                    properties.setMessageId(messageId);
                    properties.setType(NotificationMessageVO.class.getSimpleName());
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                correlationData);
        assertConfirmed(correlationData);
    }

    private void publishRawConfirmed(byte[] payload, String messageId, String correlationId) throws Exception {
        Message message = MessageBuilder.withBody(payload)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(messageId)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        CorrelationData correlationData = new CorrelationData(correlationId);
        rabbitTemplate.send(
                RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY,
                message,
                correlationData);
        assertConfirmed(correlationData);
    }

    private void assertConfirmed(CorrelationData correlationData) throws Exception {
        CorrelationData.Confirm confirm = correlationData.getFuture().get(15, TimeUnit.SECONDS);
        assertNotNull(confirm);
        assertTrue(confirm.isAck(), () -> "publisher confirm nack: " + confirm.getReason());
        assertNull(correlationData.getReturned(), "message was returned by broker");
    }

    private Message receiveRequired(String queueName, Duration timeout, String failureMessage) {
        Message message = rabbitTemplate.receive(queueName, timeout.toMillis());
        assertNotNull(message, failureMessage);
        return message;
    }

    private NotificationMessageVO notification(Integer userId, String title) {
        return new NotificationMessageVO(
                userId,
                "system",
                title,
                "Reliable notification live verification",
                Map.of("redirectUrl", "/notifications"),
                System.currentTimeMillis());
    }

    private Integer requiredUserId() {
        Integer userId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM user", Integer.class);
        assertNotNull(userId, "at least one user is required for the live test");
        return userId;
    }

    private Integer missingUserId() {
        Integer maxUserId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM user", Integer.class);
        assertNotNull(maxUserId);
        long candidate = (long) maxUserId + 1_000_000L;
        int missingUserId = candidate <= Integer.MAX_VALUE ? (int) candidate : Integer.MAX_VALUE;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE id = ?",
                Integer.class,
                missingUserId);
        assertEquals(0, count);
        return missingUserId;
    }

    private int notificationCount(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE source_message_id = ?",
                Integer.class,
                messageId);
        return count == null ? 0 : count;
    }

    private String findString(String sql, Object... arguments) {
        List<String> values = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> resultSet.getString(1),
                arguments);
        return values.isEmpty() ? null : values.get(0);
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders()
                .get(RabbitReliableMessageRepublisher.RETRY_COUNT_HEADER);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    private String headerText(Message message, String headerName) {
        Object value = message.getMessageProperties().getHeaders().get(headerName);
        return value == null ? "" : value.toString();
    }

    private String compact(String value) {
        return value.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private <T> T awaitValue(
            Supplier<T> supplier,
            Predicate<T> condition,
            Duration timeout,
            String failureMessage) {
        long deadline = System.nanoTime() + timeout.toNanos();
        T value = null;
        while (System.nanoTime() < deadline) {
            value = supplier.get();
            if (condition.test(value)) {
                return value;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("live test interrupted", exception);
            }
        }
        throw new AssertionError(failureMessage + ": lastValue=" + value);
    }

    private void cleanup(
            String validMessageId,
            String malformedMessageId,
            String retryMessageId,
            String validTitle) {
        jdbcTemplate.update("DELETE FROM notification WHERE title = ?", validTitle);
        if (validMessageId != null && !validMessageId.isBlank()) {
            jdbcTemplate.update("DELETE FROM mq_message_status WHERE message_id = ?", validMessageId);
        }
        for (String messageId : List.of(malformedMessageId, retryMessageId)) {
            jdbcTemplate.update("DELETE FROM notification WHERE source_message_id = ?", messageId);
        }
        for (String messageId : new String[]{validMessageId, malformedMessageId, retryMessageId}) {
            if (messageId != null && !messageId.isBlank()) {
                redisTemplate.delete(idempotencyService.keyFor(messageId));
            }
        }
    }
}
