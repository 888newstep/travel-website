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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumerService {

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final CacheUtil cacheUtil;
    private final PerformanceMonitorService performanceMonitorService;

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

            long startTime = System.currentTimeMillis();

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

            long duration = System.currentTimeMillis() - startTime;
            performanceMonitorService.recordTaskExecution(message.getTaskType(), duration);

            log.info("异步任务处理成功: taskId={}, duration={}ms", message.getTaskId(), duration);
        } catch (Exception e) {
            log.error("处理异步任务失败: taskId={}, error={}",
                    message.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 处理路线统计更新任务
     */
    private void handleRouteStatisticsUpdate(Map<String, Object> params) {
        log.info("执行路线统计更新任务: params={}", params);

        Integer routeId = (Integer) params.get("routeId");
        if (routeId == null) {
            log.warn("路线统计更新任务缺少routeId参数");
            return;
        }

        try {
            // TODO: 集成实际的路线统计服务
            // 1. 计算路线的平均评分
            // 2. 更新路线的浏览数、收藏数等统计数据
            // 3. 更新缓存

            cacheUtil.delete(CacheUtil.generateKey("route", "statistics", routeId));

            log.info("路线统计更新完成: routeId={}", routeId);
        } catch (Exception e) {
            log.error("路线统计更新失败: routeId={}, error={}", routeId, e.getMessage(), e);
        }
    }

    /**
     * 处理用户活动日志记录
     */
    private void handleUserActivityLog(Map<String, Object> params) {
        log.info("记录用户活动日志: params={}", params);

        Integer userId = (Integer) params.get("userId");
        String activityType = (String) params.get("activityType");
        String activityDetail = (String) params.get("activityDetail");

        if (userId == null || activityType == null) {
            log.warn("用户活动日志缺少必要参数");
            return;
        }

        try {
            // TODO: 集成实际的用户活动日志服务
            // 1. 记录用户活动到数据库
            // 2. 更新用户活跃度统计
            // 3. 用于个性化推荐

            String cacheKey = CacheUtil.generateKey("user", "activity", userId);
            cacheUtil.delete(cacheKey);

            log.info("用户活动日志记录完成: userId={}, activityType={}", userId, activityType);
        } catch (Exception e) {
            log.error("用户活动日志记录失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 处理缓存预热任务
     */
    private void handleCachePreheat(Map<String, Object> params) {
        log.info("执行缓存预热任务: params={}", params);

        String cacheType = (String) params.get("cacheType");
        Integer limit = params.get("limit") != null ? (Integer) params.get("limit") : 100;

        if (cacheType == null) {
            log.warn("缓存预热任务缺少cacheType参数");
            return;
        }

        try {
            // TODO: 集成实际的缓存预热逻辑
            switch (cacheType) {
                case "HOT_ROUTES":
                    // 预热热门路线缓存
                    log.info("预热热门路线缓存: limit={}", limit);
                    break;
                case "POPULAR_ATTRACTIONS":
                    // 预热热门景点缓存
                    log.info("预热热门景点缓存: limit={}", limit);
                    break;
                case "CITY_INFO":
                    // 预热城市信息缓存
                    log.info("预热城市信息缓存");
                    break;
                default:
                    log.warn("未知的缓存预热类型: cacheType={}", cacheType);
            }

            log.info("缓存预热任务完成: cacheType={}", cacheType);
        } catch (Exception e) {
            log.error("缓存预热任务失败: cacheType={}, error={}", cacheType, e.getMessage(), e);
        }
    }
}
