package travel.service.impl.user_community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import travel.entity.user_community.Notification;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.user_community_mapper.NotificationMapper;
import travel.service.user_community.NotificationService;
import travel.service.user_community.UserService;
import travel.utils.ThirdApiUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 通知服务实现类
 * 集成阿里云短信API + APP推送（可扩展极光/个推）
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    // 短信模板参数
    @Value("${travel.third-api.sms.warn-template-code:SMS_280000002}")
    private String warnTemplateCode;
    @Value("${travel.third-api.sms.adjust-template-code:SMS_280000003}")
    private String adjustTemplateCode;

    private final ThirdApiUtil thirdApiUtil;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createNotification(Notification notification) {
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        save(notification);
        return notification;
    }

    @Override
    public List<Notification> getByUserId(Integer userId, Integer page, Integer size) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId);
        queryWrapper.orderByDesc(Notification::getCreatedAt);
        // 分页查询
        return page((page - 1) * size, size, queryWrapper);
    }

    @Override
    public List<Notification> getCurrentUserNotifications(Integer page, Integer size) {
        User currentUser = userService.getCurrentUser();
        return getByUserId(currentUser.getId(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Integer id) {
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        return updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead() {
        User currentUser = userService.getCurrentUser();
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, currentUser.getId());
        queryWrapper.eq(Notification::getIsRead, false);

        Notification notification = new Notification();
        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());

        return update(notification, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotification(Integer id) {
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return removeById(id);
    }

    @Override
    public Integer getUnreadCount() {
        User currentUser = userService.getCurrentUser();
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, currentUser.getId());
        queryWrapper.eq(Notification::getIsRead, false);
        return (int) count(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSystemNotification(Integer userId, String type, String title, String content, String redirectUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRedirectUrl(redirectUrl);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        save(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatchNotification(List<Integer> userIds, String type, String title, String content, String redirectUrl) {
        for (Integer userId : userIds) {
            sendSystemNotification(userId, type, title, content, redirectUrl);
        }
    }

    @Override
    public boolean sendWarnNotification(String phone, String weather, Integer crowdLevel, Long attractionId, Long routeId) {
        // 1. 手机号校验
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 2. 构建预警短信模板参数
        ObjectNode templateParam = objectMapper.createObjectNode();
        templateParam.put("weather", weather == null ? "未知" : weather);
        templateParam.put("crowdLevel", getCrowdLevelDesc(crowdLevel));
        templateParam.put("attractionId", attractionId);
        templateParam.put("routeId", routeId);

        try {
            // 3. 调用短信工具类发送
            boolean smsSuccess = thirdApiUtil.sendSmsNotification(phone, templateParam.toString(), warnTemplateCode);
            if (!smsSuccess) {
                log.error("景点预警短信发送失败：手机号{}，景点{}", phone, attractionId);
                return false;
            }

            // 4. 发送APP推送（模拟，实际对接极光/个推）
            sendAppWarnNotification(phone, weather, crowdLevel, attractionId, routeId);

            log.info("景点预警通知发送成功：手机号{}，景点{}", phone, attractionId);
            return true;
        } catch (Exception e) {
            log.error("发送景点预警通知异常", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public boolean sendRouteAdjustNotification(String phone, String routeName, String adjustReason) {
        // 1. 手机号校验
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        // 2. 构建行程调整模板参数
        ObjectNode templateParam = objectMapper.createObjectNode();
        templateParam.put("routeName", routeName == null ? "未知路线" : routeName);
        templateParam.put("adjustReason", adjustReason == null ? "实时数据更新" : adjustReason);

        try {
            // 3. 发送短信
            boolean smsSuccess = thirdApiUtil.sendSmsNotification(phone, templateParam.toString(), adjustTemplateCode);
            if (!smsSuccess) {
                log.error("行程调整短信发送失败：手机号{}，路线{}", phone, routeName);
                return false;
            }

            // 4. 发送APP推送
            sendAppAdjustNotification(phone, routeName, adjustReason);

            log.info("行程调整通知发送成功：手机号{}，路线{}", phone, routeName);
            return true;
        } catch (Exception e) {
            log.error("发送行程调整通知异常", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 辅助方法：人流等级描述转换
     */
    private String getCrowdLevelDesc(Integer crowdLevel) {
        if (crowdLevel == null) return "未知";
        return switch (crowdLevel) {
            case 1 -> "人少（<1000人）";
            case 2 -> "较少（1000-3000人）";
            case 3 -> "中等（3000-5000人）";
            case 4 -> "较多（5000-8000人）";
            case 5 -> "爆满（>8000人）";
            default -> "未知";
        };
    }

    /**
     * 模拟发送APP预警推送（实际对接极光/个推API）
     */
    private void sendAppWarnNotification(String phone, String weather, Integer crowdLevel, Long attractionId, Long routeId) {
        log.info("向手机号{}发送APP预警推送：景点{}，天气{}，人流{}", phone, attractionId, weather, crowdLevel);
        // 对接极光推送示例：
        // JPushClient jPushClient = new JPushClient(masterSecret, appKey);
        // PushPayload payload = PushPayload.newBuilder()
        //         .setPlatform(Platform.android_ios())
        //         .setAudience(Audience.alias(phone))
        //         .setNotification(Notification.alert("景点预警：" + getCrowdLevelDesc(crowdLevel)))
        //         .build();
        // jPushClient.sendPush(payload);
    }

    /**
     * 模拟发送APP行程调整推送
     */
    private void sendAppAdjustNotification(String phone, String routeName, String adjustReason) {
        log.info("向手机号{}发送APP行程调整推送：路线{}，原因{}", phone, routeName, adjustReason);
    }

    /**
     * 分页查询辅助方法
     */
    private List<Notification> page(int offset, int limit, LambdaQueryWrapper<Notification> queryWrapper) {
        // 实际项目中应该使用MyBatis-Plus的分页插件
        // 这里暂时使用limit查询
        queryWrapper.last("LIMIT " + offset + ", " + limit);
        return list(queryWrapper);
    }
}
