package travel.collection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.Feedback;

import java.util.List;
import java.util.Map;

public interface FeedbackService extends IService<Feedback> {

    /**
     * 提交反馈
     */
    Feedback submitFeedback(Feedback feedback);

    /**
     * 获取当前用户的反馈列表
     */
    List<Feedback> getCurrentUserFeedbacks(Integer page, Integer size);

    /**
     * 获取反馈详情
     */
    Feedback getFeedbackDetail(Long id);

    /**
     * 回复反馈
     */
    boolean replyFeedback(Long id, String replyContent);

    /**
     * 更新反馈状态
     */
    boolean updateStatus(Long id, String status);

    /**
     * 删除反馈
     */
    boolean deleteFeedback(Long id);

    /**
     * 获取所有反馈（管理员用）
     */
    List<Feedback> getAllFeedbacks(Integer page, Integer size, String status, String type);

    /**
     * 获取反馈统计
     */
    Map<String, Object> getFeedbackStatistics();

    /**
     * 获取用户反馈列表
     */
    List<Feedback> getUserFeedbackList(Integer userId, int page, int size);

    /**
     * 标记为已处理
     */
    boolean markAsProcessed(Long feedbackId);

    /**
     * 根据类型获取反馈
     */
    List<Feedback> getFeedbackByType(String type, int page, int size);

    /**
     * 获取所有反馈类型
     */
    List<Map<String, String>> getFeedbackTypes();
}
