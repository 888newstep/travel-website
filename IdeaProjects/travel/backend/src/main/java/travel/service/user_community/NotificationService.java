package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    /**
     * 创建通知
     */
    Notification createNotification(Notification notification);

    /**
     * 获取用户的通知列表
     */
    List<Notification> getByUserId(Integer userId, Integer page, Integer size);

    /**
     * 获取当前用户的通知列表
     */
    List<Notification> getCurrentUserNotifications(Integer page, Integer size);

    /**
     * 标记通知为已读
     */
    boolean markAsRead(Integer id, Integer userId);

    /**
     * 标记所有通知为已读
     */
    boolean markAllAsRead();

    /**
     * 删除通知
     */
    boolean deleteNotification(Integer id, Integer userId);

    /**
     * 获取未读通知数量
     */
    Integer getUnreadCount();

    /**
     * 发送系统通知
     */
    void sendSystemNotification(Integer userId, String type, String title, String content, String redirectUrl);

    /**
     * 发送批量通知
     */
    void sendBatchNotification(List<Integer> userIds, String type, String title, String content, String redirectUrl);

    /**
     * 发送预警通知
     */
    boolean sendWarnNotification(String phone, String weather, Integer crowdLevel, Long attractionId, Long routeId);

    /**
     * 发送路线调整通知
     */
    boolean sendRouteAdjustNotification(String phone, String routeName, String adjustReason);
}