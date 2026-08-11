package travel.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 可靠通知的独立 RabbitMQ 拓扑。
 *
 * <p>通过开关控制声明，默认不影响现有线上队列。重试队列使用 TTL + DLX
 * 回流主交换机，避免依赖 RabbitMQ 延迟消息插件。</p>
 */
@Configuration
@ConditionalOnProperty(
        name = "mq.reliable-notification.topology.enabled",
        havingValue = "true")
public class ReliableNotificationRabbitConfig {

    @Bean
    public TopicExchange reliableNotificationExchange() {
        return new TopicExchange(RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue reliableNotificationQueue() {
        return QueueBuilder.durable(RabbitMQConfig.RELIABLE_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange",
                        RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key",
                        RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding reliableNotificationBinding(
            Queue reliableNotificationQueue,
            TopicExchange reliableNotificationExchange) {
        return BindingBuilder.bind(reliableNotificationQueue)
                .to(reliableNotificationExchange)
                .with(RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public DirectExchange reliableNotificationRetryExchange() {
        return new DirectExchange(RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue reliableNotificationRetry1Queue() {
        return retryQueue(
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_1_QUEUE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_1_TTL_MILLIS);
    }

    @Bean
    public Queue reliableNotificationRetry2Queue() {
        return retryQueue(
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_2_QUEUE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_2_TTL_MILLIS);
    }

    @Bean
    public Queue reliableNotificationRetry3Queue() {
        return retryQueue(
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_3_QUEUE,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_3_TTL_MILLIS);
    }

    @Bean
    public Binding reliableNotificationRetry1Binding(
            Queue reliableNotificationRetry1Queue,
            DirectExchange reliableNotificationRetryExchange) {
        return retryBinding(
                reliableNotificationRetry1Queue,
                reliableNotificationRetryExchange,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_1_ROUTING_KEY);
    }

    @Bean
    public Binding reliableNotificationRetry2Binding(
            Queue reliableNotificationRetry2Queue,
            DirectExchange reliableNotificationRetryExchange) {
        return retryBinding(
                reliableNotificationRetry2Queue,
                reliableNotificationRetryExchange,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_2_ROUTING_KEY);
    }

    @Bean
    public Binding reliableNotificationRetry3Binding(
            Queue reliableNotificationRetry3Queue,
            DirectExchange reliableNotificationRetryExchange) {
        return retryBinding(
                reliableNotificationRetry3Queue,
                reliableNotificationRetryExchange,
                RabbitMQConfig.RELIABLE_NOTIFICATION_RETRY_3_ROUTING_KEY);
    }

    @Bean
    public DirectExchange reliableNotificationDeadLetterExchange() {
        return new DirectExchange(
                RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue reliableNotificationDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding reliableNotificationDeadLetterBinding(
            Queue reliableNotificationDeadLetterQueue,
            DirectExchange reliableNotificationDeadLetterExchange) {
        return BindingBuilder.bind(reliableNotificationDeadLetterQueue)
                .to(reliableNotificationDeadLetterExchange)
                .with(RabbitMQConfig.RELIABLE_NOTIFICATION_DEAD_LETTER_ROUTING_KEY);
    }

    private Queue retryQueue(String queueName, int ttlMillis) {
        return QueueBuilder.durable(queueName)
                .ttl(ttlMillis)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY)
                .build();
    }

    private Binding retryBinding(Queue queue, DirectExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
