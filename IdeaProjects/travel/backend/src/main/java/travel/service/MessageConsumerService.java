package travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import travel.config.RabbitMQConfig;
import travel.entity.vo.AsyncTaskMessageVO;
import travel.entity.vo.CacheUpdateMessageVO;
import travel.entity.vo.NotificationMessageVO;
import travel.entity.user_community.Notification;
import travel.entity.user_community.User;
import travel.mapper.user_community_mapper.NotificationMapper;
import travel.service.user_community.UserService;
import travel.utils.CacheUtil;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumerService {

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final CacheUtil cacheUtil;

    /**
     * 消费通知消息
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessageVO message) {
        try {
            log.info("收到通知消息: userId={}, title={}", message.getUserId(), message.getTitle());

            Notification notification = new Notification();
            notification.setUserId(message.getUserId());
            notification.setType(message.getType());
            notification.setTitle(message.getTitle());
            notification.setContent(message.getContent());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationMapper.insert(notification);

            cacheUtil.delete(CacheUtil.generateKey(
                    CacheUtil.USER_KEY_PREFIX,
                    "notifications",
                    message.getUserId()
            ));

            log.info("通知消息处理成功: userId={}, notificationId={}",
                    message.getUserId(), notification.getId());
        } catch (Exception e) {
            log.error("处理通知消息失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 消费缓存更新消息
     */
    @RabbitListener(queues = RabbitMQConfig.CACHE_UPDATE_QUEUE)
    public void handleCacheUpdate(CacheUpdateMessageVO message) {
        try {
            log.info("收到缓存更新消息: cacheKey={}, operation={}",
                    message.getCacheKey(), message.getOperation());

            switch (message.getOperation()) {
                case "DELETE":
                    cacheUtil.delete(message.getCacheKey());
                    break;
                case "UPDATE":
                    if (message.getData() != null && message.getExpireTime() != null) {
                        cacheUtil.set(message.getCacheKey(), message.getData(),
                                message.getExpireTime(), java.util.concurrent.TimeUnit.SECONDS);
                    }
                    break;
                default:
                    log.warn("未知的缓存操作: operation={}", message.getOperation());
            }

            log.info("缓存更新消息处理成功: cacheKey={}", message.getCacheKey());
        } catch (Exception e) {
            log.error("处理缓存更新消息失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 消费异步任务消息
     */
    @RabbitListener(queues = RabbitMQConfig.ASYNC_TASK_QUEUE)
    public void handleAsyncTask(AsyncTaskMessageVO message) {
        try {
            log.info("收到异步任务消息: taskType={}, taskId={}",
                    message.getTaskType(), message.getTaskId());

            switch (message.getTaskType()) {
                case "ROUTE_STATISTICS_UPDATE":
                    handleRouteStatisticsUpdate(message.getParams());
                    break;
                case "USER_ACTIVITY_LOG":
                    handleUserActivityLog(message.getParams());
                    break;
                case "CACHE_PREHEAT":
                    handleCachePreheat(message.getParams());
                    break;
                default:
                    log.warn("未知的任务类型: taskType={}", message.getTaskType());
            }

            log.info("异步任务处理成功: taskId={}", message.getTaskId());
        } catch (Exception e) {
            log.error("处理异步任务失败: taskId={}, error={}",
                    message.getTaskId(), e.getMessage(), e);
        }
    }

    private void handleRouteStatisticsUpdate(java.util.Map<String, Object> params) {
        log.info("执行路线统计更新任务: params={}", params);
    }

    private void handleUserActivityLog(java.util.Map<String, Object> params) {
        log.info("记录用户活动日志: params={}", params);
    }

    private void handleCachePreheat(java.util.Map<String, Object> params) {
        log.info("执行缓存预热任务: params={}", params);
    }
}
