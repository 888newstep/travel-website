package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.user_community.Feedback;
import travel.entity.user_community.User;
import travel.enums.ErrorCodeEnum;
import travel.exception.BusinessException;
import travel.mapper.user_community_mapper.FeedbackMapper;
import travel.service.user_community.FeedbackService;
import travel.service.user_community.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Feedback submitFeedback(Feedback feedback) {
        User currentUser = userService.getCurrentUser();
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
        Page<Feedback> feedbackPage = page(new Page<>(page, size), queryWrapper);
        return feedbackPage.getRecords();
    }

    @Override
    public List<Feedback> getCurrentUserFeedbacks(Integer page, Integer size) {
        User currentUser = userService.getCurrentUser();
        return getByUserId(currentUser.getId(), page, size);
    }

    @Override
    public Feedback getFeedbackDetail(Long id) {
        Feedback feedback = getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCodeEnum.FEEDBACK_NOT_EXIST);
        }

        User currentUser = userService.getCurrentUser();
        if (!currentUser.getId().equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return feedback;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, String status) {
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

        User currentUser = userService.getCurrentUser();
        if (!currentUser.getId().equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        return removeById(id);
    }

    @Override
    public List<Feedback> getAllFeedbacks(Integer page, Integer size, String status, String type) {
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Feedback::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Feedback::getType, type);
        }
        queryWrapper.orderByDesc(Feedback::getCreatedAt);
        return page(new Page<>(page, size), queryWrapper).getRecords();
    }

    @Override
    public Map<String, Object> getFeedbackStatistics() {
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
        LambdaQueryWrapper<Feedback> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Feedback::getStatus, "completed");
        long completedCount = count(completedWrapper);
        statistics.put("completedCount", (int) completedCount);

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
        return getByUserId(userId, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyFeedback(Long id, String replyContent) {
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
        log.info("标记反馈为已处理: feedbackId={}", feedbackId);
        Feedback feedback = getById(feedbackId);
        if (feedback == null) {
            return false;
        }
        feedback.setStatus("resolved");
        feedback.setUpdatedAt(java.time.LocalDateTime.now());
        return updateById(feedback);
    }

    @Override
    public List<Feedback> getFeedbackByType(String type, int page, int size) {
        log.info("根据类型获取反馈: type={}, page={}, size={}", type, page, size);
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Feedback::getType, type);
        }
        queryWrapper.orderByDesc(Feedback::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Feedback> pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        return page(pageParam, queryWrapper).getRecords();
    }
}