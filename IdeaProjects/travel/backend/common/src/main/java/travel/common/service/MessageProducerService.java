package travel.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import travel.common.config.RabbitMQConfig;
import travel.common.vo.AsyncTaskMessageVO;
import travel.common.vo.CacheUpdateMessageVO;
import travel.common.vo.NotificationMessageVO;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MessageProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final MqMessageStatusService mqMessageStatusService;
    private final ObjectMapper objectMapper;
    private final boolean statusPersistenceEnabled;
    private final boolean reliableNotificationProducerEnabled;

    /**
     * 消息状态表属于第一阶段可选能力，默认关闭，避免未执行数据库迁移时影响现有发布链路。
     */
    @Autowired
    public MessageProducerService(
            RabbitTemplate rabbitTemplate,
            MqMessageStatusService mqMessageStatusService,
            ObjectMapper objectMapper,
            @Value("${mq.status-persistence.enabled:false}") boolean statusPersistenceEnabled,
            @Value("${mq.reliable-notification.producer.enabled:false}")
            boolean reliableNotificationProducerEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.mqMessageStatusService = mqMessageStatusService;
        this.objectMapper = objectMapper;
        this.statusPersistenceEnabled = statusPersistenceEnabled;
        this.reliableNotificationProducerEnabled = reliableNotificationProducerEnabled;
    }

    /**
     * 保留旧构造函数，兼容未开启状态持久化的单元测试和调用方。
     */
    public MessageProducerService(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, null, null, false, false);
    }

    /**
     * 保留旧的四参数构造函数，避免现有单元测试和手工装配调用方受到影响。
     */
    public MessageProducerService(
            RabbitTemplate rabbitTemplate,
            MqMessageStatusService mqMessageStatusService,
            ObjectMapper objectMapper,
            boolean statusPersistenceEnabled) {
        this(rabbitTemplate, mqMessageStatusService, objectMapper,
                statusPersistenceEnabled, false);
    }

    /**
     * 发送通知消息
     */
    public void sendNotification(Integer userId, String type, String title, String content) {
        try {
            NotificationMessageVO message = new NotificationMessageVO(
                    userId,
                    type,
                    title,
                    content,
                    Map.of(),
                    System.currentTimeMillis()
            );
            String exchange = reliableNotificationProducerEnabled
                    ? RabbitMQConfig.RELIABLE_NOTIFICATION_EXCHANGE
                    : RabbitMQConfig.NOTIFICATION_EXCHANGE;
            String routingKey = reliableNotificationProducerEnabled
                    ? RabbitMQConfig.RELIABLE_NOTIFICATION_ROUTING_KEY
                    : RabbitMQConfig.NOTIFICATION_ROUTING_KEY;
            String messageId = sendMessage(exchange, routingKey, message,
                    reliableNotificationProducerEnabled);
            log.info("发送通知消息已提交，等待 broker confirm: messageId={}, userId={}, type={}",
                    messageId, userId, type);
        } catch (Exception e) {
            log.error("发送通知消息失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发送缓存更新消息
     */
    public void sendCacheUpdate(String cacheKey, String operation, Object data) {
        try {
            CacheUpdateMessageVO message = new CacheUpdateMessageVO(
                    cacheKey,
                    operation,
                    data,
                    null
            );
            String messageId = sendMessage(
                    RabbitMQConfig.CACHE_UPDATE_EXCHANGE,
                    RabbitMQConfig.CACHE_UPDATE_ROUTING_KEY,
                    message
            );
            log.info("发送缓存更新消息已提交，等待 broker confirm: messageId={}, cacheKey={}, operation={}",
                    messageId, cacheKey, operation);
        } catch (Exception e) {
            log.error("发送缓存更新消息失败: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * 发送异步任务消息
     */
    public void sendAsyncTask(String taskType, String taskId, Map<String, Object> params) {
        try {
            AsyncTaskMessageVO message = new AsyncTaskMessageVO(
                    taskType,
                    taskId,
                    params,
                    System.currentTimeMillis()
            );
            String messageId = sendMessage(
                    RabbitMQConfig.ASYNC_TASK_EXCHANGE,
                    RabbitMQConfig.ASYNC_TASK_ROUTING_KEY,
                    message
            );
            log.info("发送异步任务消息已提交，等待 broker confirm: messageId={}, taskType={}, taskId={}",
                    messageId, taskType, taskId);
        } catch (Exception e) {
            log.error("发送异步任务消息失败: taskType={}, error={}", taskType, e.getMessage(), e);
        }
    }

    /**
     * 为每条消息生成唯一关联号，配合 publisher confirm 识别异步投递结果。
     */
    private String sendMessage(String exchange, String routingKey, Object message) {
        return sendMessage(exchange, routingKey, message, false);
    }

    private String sendMessage(
            String exchange,
            String routingKey,
            Object message,
            boolean forceMessageMetadata) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        String messageType = message.getClass().getSimpleName();

        if (!statusPersistenceEnabled && !forceMessageMetadata) {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
            log.debug("RabbitMQ message published: messageId={}, exchange={}, routingKey={}",
                    messageId, exchange, routingKey);
            return messageId;
        }

        if (statusPersistenceEnabled && (mqMessageStatusService == null || objectMapper == null)) {
            throw new IllegalStateException("消息状态持久化已开启，但依赖未注入");
        }

        if (statusPersistenceEnabled) {
            String payloadJson = serializePayload(message, messageId);
            mqMessageStatusService.createPending(messageId, messageType, exchange, routingKey, payloadJson);
        }

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    message,
                    addMessageMetadata(messageId, messageType),
                    correlationData
            );
        } catch (Exception e) {
            // 本地发送调用未返回，可以安全标记为失败。
            markFailedSafely(messageId, e);
            throw e;
        }

        // DISPATCHED 仅表示本地 RabbitTemplate 调用已返回，不代表 broker confirm 或消费者处理成功。
        if (statusPersistenceEnabled) {
            markDispatchedSafely(messageId, exchange, routingKey);
        }
        log.debug("RabbitMQ message published: messageId={}, exchange={}, routingKey={}",
                messageId, exchange, routingKey);
        return messageId;
    }

    private String serializePayload(Object message, String messageId) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("消息载荷序列化失败: messageId=" + messageId, e);
        }
    }

    private MessagePostProcessor addMessageMetadata(String messageId, String messageType) {
        return (rabbitMessage) -> {
            if (rabbitMessage == null || rabbitMessage.getMessageProperties() == null) {
                throw new IllegalStateException("RabbitMQ 消息属性为空: messageId=" + messageId);
            }
            rabbitMessage.getMessageProperties().setMessageId(messageId);
            rabbitMessage.getMessageProperties().setType(messageType);
            return rabbitMessage;
        };
    }

    private void markDispatchedSafely(String messageId, String exchange, String routingKey) {
        try {
            mqMessageStatusService.markDispatched(messageId);
        } catch (Exception e) {
            // 发送调用已经返回，消息可能已进入 broker，不能伪造为 FAILED。
            log.error("消息已提交但状态更新失败，当前投递状态未知: messageId={}, exchange={}, routingKey={}, error={}",
                    messageId, exchange, routingKey, e.getMessage(), e);
        }
    }

    private void markFailedSafely(String messageId, Exception sendException) {
        try {
            mqMessageStatusService.markFailed(messageId, sendException);
        } catch (Exception stateException) {
            log.error("消息发送失败且状态更新也失败: messageId={}, sendError={}, stateError={}",
                    messageId, sendException.getMessage(), stateException.getMessage(), stateException);
        }
    }
}
