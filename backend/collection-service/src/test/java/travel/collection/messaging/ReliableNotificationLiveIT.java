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
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @Autowired
    private CachingConnectionFactory connectionFactory;

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void shouldVerifyPublisherConsumerIdempotencyRetryAndDeadLetter() throws Exception {
        String runId = UUID.randomUUID().toString().replace("-", "");
        String retryTapQueue = "travel.live.retry.tap." + runId;
        String deadLetterTapQueue = "travel.live.dlq.tap." + runId;
        Integer validUserId = requiredUserId();
        Integer missingUserId = missingUserId();
        String validTitle = "RabbitMQ live " + runId;
        String malformedMessageId = "mq-live-malformed-" + runId;
        String retryMessageId = "mq-live-retry-" + runId;
        String restartMessageId = "mq-live-restart-" + runId;
        String reconnectMessageId = "mq-live-reconnect-" + runId;
        String replayTitle = "RabbitMQ replay " + runId;
        String restartTitle = "RabbitMQ restart " + runId;
        String reconnectTitle = "RabbitMQ reconnect " + runId;
        String concurrentTitlePrefix = "RabbitMQ concurrent " + runId + "-";
        List<String> concurrentMessageIds = new ArrayList<>();
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
                    notification(validUserId, replayTitle),
                    malformedMessageId,
                    "manual-replay-" + runId);
            awaitValue(
                    () -> notificationCount(malformedMessageId),
                    count -> count == 1,
                    DATABASE_TIMEOUT,
                    "manually replayed DLQ notification was not consumed");
            System.out.printf(
                    "RABBIT_LIVE_RESULT scenario=dlq-replay messageId=%s databaseCount=%d%n",
                    malformedMessageId,
                    notificationCount(malformedMessageId));

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

            verifyConsumerRestart(validUserId, restartTitle, restartMessageId, runId);
            verifyConnectionRecovery(validUserId, reconnectTitle, reconnectMessageId, runId);
            verifyConcurrentConsumption(
                    validUserId,
                    concurrentTitlePrefix,
                    concurrentMessageIds,
                    runId,
                    100);
        } finally {
            amqpAdmin.deleteQueue(retryTapQueue);
            amqpAdmin.deleteQueue(deadLetterTapQueue);
            cleanup(
                    validMessageId,
                    malformedMessageId,
                    retryMessageId,
                    restartMessageId,
                    reconnectMessageId,
                    concurrentMessageIds,
                    validTitle,
                    replayTitle,
                    restartTitle,
                    reconnectTitle,
                    concurrentTitlePrefix);
        }
    }

    private void verifyConsumerRestart(
            Integer userId,
            String title,
            String messageId,
            String runId) throws Exception {
        MessageListenerContainer container = listenerRegistry.getListenerContainer("reliableNotificationConsumer");
        assertNotNull(container, "reliable notification listener container was not registered");
        CountDownLatch stopped = new CountDownLatch(1);
        container.stop(stopped::countDown);
        assertTrue(stopped.await(15, TimeUnit.SECONDS), "consumer did not stop in time");
        assertTrue(!container.isRunning(), "consumer remained running after stop");

        publishConfirmed(notification(userId, title), messageId, "restart-publish-" + runId);
        Thread.sleep(1_000L);
        assertEquals(0, notificationCount(messageId), "stopped consumer processed a new message");

        container.start();
        awaitValue(container::isRunning, Boolean.TRUE::equals, Duration.ofSeconds(15),
                "consumer did not restart");
        awaitValue(
                () -> notificationCount(messageId),
                count -> count == 1,
                DATABASE_TIMEOUT,
                "queued notification was not consumed after consumer restart");
        System.out.printf(
                "RABBIT_LIVE_RESULT scenario=consumer-restart messageId=%s databaseCount=%d%n",
                messageId,
                notificationCount(messageId));
    }

    private void verifyConnectionRecovery(
            Integer userId,
            String title,
            String messageId,
            String runId) throws Exception {
        connectionFactory.resetConnection();
        publishConfirmed(notification(userId, title), messageId, "reconnect-publish-" + runId);
        awaitValue(
                () -> notificationCount(messageId),
                count -> count == 1,
                DATABASE_TIMEOUT,
                "notification was not consumed after forced connection reset");
        System.out.printf(
                "RABBIT_LIVE_RESULT scenario=connection-recovery messageId=%s databaseCount=%d%n",
                messageId,
                notificationCount(messageId));
    }

    private void verifyConcurrentConsumption(
            Integer userId,
            String titlePrefix,
            List<String> messageIds,
            String runId,
            int messageCount) {
        ExecutorService executor = Executors.newFixedThreadPool(16);
        long startedAt = System.nanoTime();
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(messageCount);
            for (int index = 0; index < messageCount; index++) {
                String messageId = "mq-live-concurrent-" + runId + "-" + index;
                messageIds.add(messageId);
                String title = titlePrefix + index;
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        publishConfirmed(
                                notification(userId, title),
                                messageId,
                                "concurrent-" + runId + "-" + messageId);
                    } catch (Exception exception) {
                        throw new CompletionException(exception);
                    }
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            awaitValue(
                    () -> notificationCountByTitlePrefix(titlePrefix),
                    count -> count == messageCount,
                    Duration.ofSeconds(60),
                    "concurrent notifications were not fully consumed");
            Set<String> persistedIds = new HashSet<>(jdbcTemplate.queryForList(
                    "SELECT source_message_id FROM notification WHERE title LIKE ?",
                    String.class,
                    escapeLike(titlePrefix) + "%"));
            assertEquals(messageCount, persistedIds.size(), "concurrent source message IDs were not unique");
            assertTrue(persistedIds.containsAll(messageIds), "some concurrent message IDs were not persisted");
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            double throughput = durationMs == 0 ? messageCount : messageCount * 1_000.0 / durationMs;
            System.out.printf(
                    "RABBIT_LIVE_RESULT scenario=concurrent-consume messageId=batch-%s messages=%d "
                            + "databaseCount=%d durationMs=%d throughput=%.2f%n",
                    runId,
                    messageCount,
                    notificationCountByTitlePrefix(titlePrefix),
                    durationMs,
                    throughput);
        } finally {
            executor.shutdownNow();
        }
    }

    private void declareTapQueue(String queueName, String exchangeName, String routingKey) {
        // RabbitTemplate.receive 可能使用不同连接，临时队列不能绑定到声明连接，否则会被 broker 自动删除。
        // RabbitMQ 4.x 已废弃 transient non-exclusive queue，测试探针使用唯一持久队列并在 finally 删除。
        Queue queue = QueueBuilder.durable(queueName).build();
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

    private int notificationCountByTitlePrefix(String titlePrefix) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE title LIKE ?",
                Integer.class,
                escapeLike(titlePrefix) + "%");
        return count == null ? 0 : count;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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
            String restartMessageId,
            String reconnectMessageId,
            List<String> concurrentMessageIds,
            String validTitle,
            String replayTitle,
            String restartTitle,
            String reconnectTitle,
            String concurrentTitlePrefix) {
        jdbcTemplate.update(
                "DELETE FROM notification WHERE title IN (?, ?, ?, ?)",
                validTitle,
                replayTitle,
                restartTitle,
                reconnectTitle);
        jdbcTemplate.update(
                "DELETE FROM notification WHERE title LIKE ?",
                escapeLike(concurrentTitlePrefix) + "%");
        if (validMessageId != null && !validMessageId.isBlank()) {
            jdbcTemplate.update("DELETE FROM mq_message_status WHERE message_id = ?", validMessageId);
        }
        for (String messageId : List.of(
                malformedMessageId,
                retryMessageId,
                restartMessageId,
                reconnectMessageId)) {
            jdbcTemplate.update("DELETE FROM notification WHERE source_message_id = ?", messageId);
        }
        List<String> allMessageIds = new ArrayList<>(concurrentMessageIds);
        allMessageIds.add(validMessageId);
        allMessageIds.add(malformedMessageId);
        allMessageIds.add(retryMessageId);
        allMessageIds.add(restartMessageId);
        allMessageIds.add(reconnectMessageId);
        for (String messageId : allMessageIds) {
            if (messageId != null && !messageId.isBlank()) {
                redisTemplate.delete(idempotencyService.keyFor(messageId));
            }
        }
    }
}
