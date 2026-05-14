package travel.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
