package travel.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import travel.common.service.MqMessageStatusService;

@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";

    public static final String CACHE_UPDATE_EXCHANGE = "cache.update.exchange";
    public static final String CACHE_UPDATE_QUEUE = "cache.update.queue";
    public static final String CACHE_UPDATE_ROUTING_KEY = "cache.update";

    public static final String ASYNC_TASK_EXCHANGE = "async.task.exchange";
    public static final String ASYNC_TASK_QUEUE = "async.task.queue";
    public static final String ASYNC_TASK_ROUTING_KEY = "async.task.execute";

    /**
     * 独立版本化的可靠通知拓扑。
     *
     * <p>不修改旧 notification.queue 的声明参数，避免云端已有同名队列发生
     * PRECONDITION_FAILED；切换生产端前需要先完成新拓扑和消费者的灰度验证。</p>
     */
    public static final String RELIABLE_NOTIFICATION_EXCHANGE = "notification.reliable.exchange.v1";
    public static final String RELIABLE_NOTIFICATION_QUEUE = "notification.reliable.queue.v1";
    public static final String RELIABLE_NOTIFICATION_ROUTING_KEY = "notification.reliable.send.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_EXCHANGE = "notification.reliable.retry.exchange.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_1_QUEUE = "notification.reliable.retry.1.queue.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_2_QUEUE = "notification.reliable.retry.2.queue.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_3_QUEUE = "notification.reliable.retry.3.queue.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_1_ROUTING_KEY = "notification.reliable.retry.1.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_2_ROUTING_KEY = "notification.reliable.retry.2.v1";
    public static final String RELIABLE_NOTIFICATION_RETRY_3_ROUTING_KEY = "notification.reliable.retry.3.v1";
    public static final String RELIABLE_NOTIFICATION_DEAD_LETTER_EXCHANGE = "notification.reliable.dlx.v1";
    public static final String RELIABLE_NOTIFICATION_DEAD_LETTER_QUEUE = "notification.reliable.dlq.v1";
    public static final String RELIABLE_NOTIFICATION_DEAD_LETTER_ROUTING_KEY = "notification.reliable.dlq.v1";

    public static final int RELIABLE_NOTIFICATION_RETRY_1_TTL_MILLIS = 5_000;
    public static final int RELIABLE_NOTIFICATION_RETRY_2_TTL_MILLIS = 30_000;
    public static final int RELIABLE_NOTIFICATION_RETRY_3_TTL_MILLIS = 120_000;

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Queue cacheUpdateQueue() {
        return new Queue(CACHE_UPDATE_QUEUE, true);
    }

    @Bean
    public TopicExchange cacheUpdateExchange() {
        return new TopicExchange(CACHE_UPDATE_EXCHANGE);
    }

    @Bean
    public Binding cacheUpdateBinding(Queue cacheUpdateQueue, TopicExchange cacheUpdateExchange) {
        return BindingBuilder.bind(cacheUpdateQueue)
                .to(cacheUpdateExchange)
                .with(CACHE_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Queue asyncTaskQueue() {
        return new Queue(ASYNC_TASK_QUEUE, true, false, false);
    }

    @Bean
    public TopicExchange asyncTaskExchange() {
        return new TopicExchange(ASYNC_TASK_EXCHANGE);
    }

    @Bean
    public Binding asyncTaskBinding(Queue asyncTaskQueue, TopicExchange asyncTaskExchange) {
        return BindingBuilder.bind(asyncTaskQueue)
                .to(asyncTaskExchange)
                .with(ASYNC_TASK_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            ObjectProvider<MqMessageStatusService> statusServiceProvider,
            @Value("${mq.status-persistence.enabled:false}") boolean statusPersistenceEnabled) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = correlationData == null ? null : correlationData.getId();
            if (ack) {
                log.debug("RabbitMQ publisher confirm ack: messageId={}",
                        messageId == null ? "unknown" : messageId);
                markConfirmedSafely(statusServiceProvider, statusPersistenceEnabled, messageId);
            } else {
                log.error("RabbitMQ publisher confirm nack: messageId={}, cause={}",
                        messageId == null ? "unknown" : messageId, cause);
                markFailedSafely(statusServiceProvider, statusPersistenceEnabled, messageId, cause);
            }
        });
        template.setReturnsCallback(returned -> {
            if (returned == null) {
                log.error("RabbitMQ returned callback received a null message");
                return;
            }
            log.error(
                    "RabbitMQ message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());
            String messageId = returned.getMessage() == null
                    || returned.getMessage().getMessageProperties() == null
                    ? null
                    : returned.getMessage().getMessageProperties().getMessageId();
            markReturnedSafely(statusServiceProvider, statusPersistenceEnabled, messageId, returned);
        });
        return template;
    }

    private void markConfirmedSafely(
            ObjectProvider<MqMessageStatusService> statusServiceProvider,
            boolean statusPersistenceEnabled,
            String messageId) {
        if (!statusPersistenceEnabled || isBlank(messageId)) {
            return;
        }
        try {
            MqMessageStatusService statusService = statusServiceProvider.getIfAvailable();
            if (statusService == null) {
                log.warn("消息状态持久化已开启，但 MqMessageStatusService 不可用: messageId={}", messageId);
                return;
            }
            statusService.markConfirmed(messageId);
        } catch (Exception e) {
            log.error("RabbitMQ confirm 回调更新消息状态失败: messageId={}, error={}",
                    messageId, e.getMessage(), e);
        }
    }

    private void markFailedSafely(
            ObjectProvider<MqMessageStatusService> statusServiceProvider,
            boolean statusPersistenceEnabled,
            String messageId,
            String cause) {
        if (!statusPersistenceEnabled || isBlank(messageId)) {
            return;
        }
        try {
            MqMessageStatusService statusService = statusServiceProvider.getIfAvailable();
            if (statusService == null) {
                log.warn("消息状态持久化已开启，但 MqMessageStatusService 不可用: messageId={}", messageId);
                return;
            }
            statusService.markFailed(messageId, new IllegalStateException(
                    cause == null || cause.isBlank() ? "broker publisher confirm nack" : cause));
        } catch (Exception e) {
            log.error("RabbitMQ nack 回调更新消息状态失败: messageId={}, error={}",
                    messageId, e.getMessage(), e);
        }
    }

    private void markReturnedSafely(
            ObjectProvider<MqMessageStatusService> statusServiceProvider,
            boolean statusPersistenceEnabled,
            String messageId,
            ReturnedMessage returned) {
        if (!statusPersistenceEnabled || isBlank(messageId)) {
            if (statusPersistenceEnabled) {
                log.warn("RabbitMQ returned 消息缺少 messageId，无法更新状态: exchange={}, routingKey={}",
                        returned.getExchange(), returned.getRoutingKey());
            }
            return;
        }
        try {
            MqMessageStatusService statusService = statusServiceProvider.getIfAvailable();
            if (statusService == null) {
                log.warn("消息状态持久化已开启，但 MqMessageStatusService 不可用: messageId={}", messageId);
                return;
            }
            String reason = "broker returned " + returned.getReplyCode() + ": " + returned.getReplyText();
            statusService.markReturned(messageId, new IllegalStateException(reason));
        } catch (Exception e) {
            log.error("RabbitMQ returned 回调更新消息状态失败: messageId={}, error={}",
                    messageId, e.getMessage(), e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
