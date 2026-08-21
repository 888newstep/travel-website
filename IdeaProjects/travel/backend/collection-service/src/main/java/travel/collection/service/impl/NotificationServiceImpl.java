package travel.collection.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.entity.user_community.Notification;
import travel.common.entity.user_community.User;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.user_community_mapper.NotificationMapper;
import travel.common.vo.NotificationMessageVO;
import travel.common.security.AuthenticatedUserSupport;
import travel.collection.service.NotificationService;
import travel.collection.service.UserService;
import travel.collection.util.CurrentUserSupport;
import travel.common.utils.ThirdApiUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
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
    @Transactional(rollbackFor = Exception.class)
    public Notification createReliableNotification(
            String sourceMessageId,
            NotificationMessageVO message) {
        if (sourceMessageId == null || sourceMessageId.isBlank()) {
            throw new IllegalArgumentException("sourceMessageId cannot be blank");
        }
        if (message == null) {
            throw new IllegalArgumentException("notification message cannot be null");
        }

        Notification existing = lambdaQuery()
                .eq(Notification::getSourceMessageId, sourceMessageId)
                .one();
        if (existing != null) {
            return existing;
        }

        Notification notification = new Notification();
        notification.setUserId(message.getUserId());
        notification.setType(message.getType());
        notification.setTitle(message.getTitle());
        notification.setContent(message.getContent());
        notification.setRedirectUrl(resolveRedirectUrl(message));
        notification.setSourceMessageId(sourceMessageId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());

        try {
            save(notification);
            return notification;
        } catch (DuplicateKeyException duplicateKeyException) {
            // 两个消费者同时越过 Redis 快速路径时，唯一键竞争仍应收敛为成功。
            Notification concurrent = lambdaQuery()
                    .eq(Notification::getSourceMessageId, sourceMessageId)
                    .one();
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicateKeyException;
        }
    }

    private String resolveRedirectUrl(NotificationMessageVO message) {
        if (message.getExtraData() == null) {
            return null;
        }
        Object redirectUrl = message.getExtraData().get("redirectUrl");
        return redirectUrl == null ? null : String.valueOf(redirectUrl);
    }

    @Override
    public List<Notification> getByUserId(Integer userId, Integer page, Integer size) {
        Integer currentUserId = resolveCurrentUserId();
        Page<Notification> pageParam = createPage(page, size);
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, currentUserId);
        queryWrapper.orderByDesc(Notification::getCreatedAt);
        return page(pageParam, queryWrapper).getRecords();
    }

    @Override
    public List<Notification> getCurrentUserNotifications(Integer page, Integer size) {
        return getByUserId(resolveCurrentUserId(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Integer id, Integer userId) {
        Integer currentUserId = resolveCurrentUserId();
        validateNotificationId(id);
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_NOT_EXIST);
        }

        if (!currentUserId.equals(notification.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        return updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead() {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, resolveCurrentUserId());
        queryWrapper.eq(Notification::getIsRead, false);

        Notification notification = new Notification();
        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());

        return update(notification, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotification(Integer id, Integer userId) {
        Integer currentUserId = resolveCurrentUserId();
        validateNotificationId(id);
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCodeEnum.NOTIFICATION_NOT_EXIST);
        }

        if (!currentUserId.equals(notification.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return removeById(id);
    }

    @Override
    public Integer getUnreadCount() {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, resolveCurrentUserId());
        queryWrapper.eq(Notification::getIsRead, false);
        return (int) count(queryWrapper);
    }

    /**
     * 优先使用 JWT 过滤器写入的认证主体，避免每次通知读请求都回源 user-service。
     * 非 HTTP 线程或历史调用未建立 SecurityContext 时保留原有 Feign 兜底。
     */
    private Integer resolveCurrentUserId() {
        Integer authenticatedUserId = AuthenticatedUserSupport.getIntegerUserIdOrNull();
        if (authenticatedUserId != null) {
            return authenticatedUserId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Number number
                    && number.longValue() > 0
                    && number.longValue() <= Integer.MAX_VALUE) {
                return number.intValue();
            }
        }

        User currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
        return currentUser.getId();
    }

    private Page<Notification> createPage(Integer page, Integer size) {
        if (page == null || page <= 0 || page > 1_000_000 || size == null || size <= 0 || size > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return new Page<>(page, size);
    }

    private void validateNotificationId(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
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
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
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
                log.error("景点预警短信发送失败：手机号{}，景点{}", maskPhone(phone), attractionId);
                return false;
            }

            log.info("景点预警短信发送成功：手机号{}，景点{}", maskPhone(phone), attractionId);
            return true;
        } catch (Exception e) {
            log.error("发送景点预警通知异常", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public boolean sendRouteAdjustNotification(String phone, String routeName, String adjustReason) {
        // 1. 手机号校验
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
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
                log.error("行程调整短信发送失败：手机号{}，路线{}", maskPhone(phone), routeName);
                return false;
            }

            log.info("行程调整短信发送成功：手机号{}，路线{}", maskPhone(phone), routeName);
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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
