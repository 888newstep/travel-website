package travel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import travel.config.RabbitMQConfig;
import travel.entity.vo.AsyncTaskMessageVO;
import travel.entity.vo.CacheUpdateMessageVO;
import travel.entity.vo.NotificationMessageVO;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducerService {

    private final RabbitTemplate rabbitTemplate;

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
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    message
            );
            log.info("发送通知消息成功: userId={}, type={}", userId, type);
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
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CACHE_UPDATE_EXCHANGE,
                    RabbitMQConfig.CACHE_UPDATE_ROUTING_KEY,
                    message
            );
            log.info("发送缓存更新消息成功: cacheKey={}, operation={}", cacheKey, operation);
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
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ASYNC_TASK_EXCHANGE,
                    RabbitMQConfig.ASYNC_TASK_ROUTING_KEY,
                    message
            );
            log.info("发送异步任务消息成功: taskType={}, taskId={}", taskType, taskId);
        } catch (Exception e) {
            log.error("发送异步任务消息失败: taskType={}, error={}", taskType, e.getMessage(), e);
        }
    }
}
