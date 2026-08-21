package travel.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.Feedback;
import travel.common.entity.user_community.User;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.mapper.user_community_mapper.FeedbackMapper;
import travel.collection.service.FeedbackService;
import travel.collection.service.UserService;
import travel.collection.util.CurrentUserSupport;
import travel.common.security.AuthenticatedUserSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    private static final Set<String> FEEDBACK_TYPES = Set.of("suggestion", "bug", "complaint", "other");
    private static final Set<String> FEEDBACK_STATUSES = Set.of("pending", "processing", "resolved");

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Feedback submitFeedback(Feedback feedback) {
        validateFeedback(feedback);
        User currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
        feedback.setUserId(currentUser.getId());
        feedback.setStatus("pending");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());

        save(feedback);
        return feedback;
    }

    public List<Feedback> getByUserId(Integer userId, Integer page, Integer size) {
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Feedback::getUserId, userId);
        queryWrapper.orderByDesc(Feedback::getCreatedAt);
        Page<Feedback> feedbackPage = page(createPage(page, size), queryWrapper);
        return feedbackPage.getRecords();
    }

    @Override
    public List<Feedback> getCurrentUserFeedbacks(Integer page, Integer size) {
        User currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
        return getByUserId(currentUser.getId(), page, size);
    }

    @Override
    public Feedback getFeedbackDetail(Long id) {
        Feedback feedback = getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCodeEnum.FEEDBACK_NOT_EXIST);
        }

        User currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
        if (!currentUser.getId().equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return feedback;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, String status) {
        AuthenticatedUserSupport.requireAdmin();
        validateStatus(status);
        Feedback feedback = getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCodeEnum.FEEDBACK_NOT_EXIST);
        }

        feedback.setStatus(status);
        feedback.setUpdatedAt(LocalDateTime.now());
        return updateById(feedback);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFeedback(Long id) {
        Feedback feedback = getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCodeEnum.FEEDBACK_NOT_EXIST);
        }

        User currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
        if (!currentUser.getId().equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return removeById(id);
    }

    @Override
    public List<Feedback> getAllFeedbacks(Integer page, Integer size, String status, String type) {
        AuthenticatedUserSupport.requireAdmin();
        if (status != null && !status.isBlank()) {
            validateStatus(status);
        }
        if (type != null && !type.isBlank()) {
            validateType(type);
        }
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Feedback::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Feedback::getType, type);
        }
        queryWrapper.orderByDesc(Feedback::getCreatedAt);
        return page(createPage(page, size), queryWrapper).getRecords();
    }

    @Override
    public Map<String, Object> getFeedbackStatistics() {
        AuthenticatedUserSupport.requireAdmin();
        Map<String, Object> statistics = new HashMap<>();

        // 总数量
        long totalCount = count();
        statistics.put("totalCount", (int) totalCount);

        // 待处理数量
        LambdaQueryWrapper<Feedback> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Feedback::getStatus, "pending");
        long pendingCount = count(pendingWrapper);
        statistics.put("pendingCount", (int) pendingCount);

        // 处理中数量
        LambdaQueryWrapper<Feedback> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.eq(Feedback::getStatus, "processing");
        long processingCount = count(processingWrapper);
        statistics.put("processingCount", (int) processingCount);

        // 已完成数量
        LambdaQueryWrapper<Feedback> resolvedWrapper = new LambdaQueryWrapper<>();
        resolvedWrapper.eq(Feedback::getStatus, "resolved");
        long resolvedCount = count(resolvedWrapper);
        statistics.put("resolvedCount", (int) resolvedCount);
        statistics.put("completedCount", (int) resolvedCount);

        // 类型统计
        statistics.put("byType", Map.of());

        return statistics;
    }

    /**
     * 分页查询辅助方法
     */
    // 以下是Controller中使用的方法实现

    @Override
    public List<Feedback> getUserFeedbackList(Integer userId, int page, int size) {
        log.info("获取用户反馈列表: userId={}, page={}, size={}", userId, page, size);
        User currentUser = CurrentUserSupport.requireUser(userService.getCurrentUser());
        return getByUserId(currentUser.getId(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyFeedback(Long id, String replyContent) {
        AuthenticatedUserSupport.requireAdmin();
        if (replyContent == null || replyContent.isBlank() || replyContent.length() > 5000) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Feedback feedback = getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCodeEnum.FEEDBACK_NOT_EXIST);
        }

        feedback.setReplyContent(replyContent);
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setStatus("resolved");
        feedback.setUpdatedAt(LocalDateTime.now());

        return updateById(feedback);
    }

    @Override
    public boolean markAsProcessed(Long feedbackId) {
        AuthenticatedUserSupport.requireAdmin();
        log.info("标记反馈为已处理: feedbackId={}", feedbackId);
        Feedback feedback = getById(feedbackId);
        if (feedback == null) {
            throw new BusinessException(ErrorCodeEnum.FEEDBACK_NOT_EXIST);
        }
        feedback.setStatus("resolved");
        feedback.setUpdatedAt(java.time.LocalDateTime.now());
        return updateById(feedback);
    }

    @Override
    public List<Feedback> getFeedbackByType(String type, int page, int size) {
        AuthenticatedUserSupport.requireAdmin();
        validateType(type);
        log.info("根据类型获取反馈: type={}, page={}, size={}", type, page, size);
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Feedback::getType, type);
        }
        queryWrapper.orderByDesc(Feedback::getCreatedAt);
        Page<Feedback> pageParam = createPage(page, size);
        return page(pageParam, queryWrapper).getRecords();
    }

    @Override
    public List<Map<String, String>> getFeedbackTypes() {
        return List.of(
                Map.of("value", "suggestion", "label", "建议"),
                Map.of("value", "bug", "label", "问题反馈"),
                Map.of("value", "complaint", "label", "投诉"),
                Map.of("value", "other", "label", "其他")
        );
    }

    private void validateFeedback(Feedback feedback) {
        if (feedback == null || feedback.getContent() == null || feedback.getContent().isBlank()
                || feedback.getContent().length() > 5000
                || (feedback.getContactInfo() != null && feedback.getContactInfo().length() > 100)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        validateType(feedback.getType());
    }

    private void validateType(String type) {
        if (type == null || !FEEDBACK_TYPES.contains(type)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private void validateStatus(String status) {
        if (status == null || !FEEDBACK_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
    }

    private Page<Feedback> createPage(Integer page, Integer size) {
        if (page == null || page < 0 || size == null || size <= 0 || size > 100) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return new Page<>((long) page + 1, size);
    }
}
