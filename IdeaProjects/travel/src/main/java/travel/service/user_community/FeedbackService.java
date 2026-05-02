package travel.service.user_community;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.user_community.Feedback;

import java.util.List;
import java.util.Map;

public interface FeedbackService extends IService<Feedback> {

    /**
     * 提交反馈
     */
    Feedback submitFeedback(Feedback feedback);

    /**
     * 获取用户的反馈列表
     */
    List<Feedback> getByUserId(Integer userId, Integer page, Integer size);

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

    // 以下是Controller中使用的方法

    /**
     * 获取用户反馈列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 反馈列表
     */
    List<Feedback> getUserFeedbackList(Integer userId, int page, int size);

    /**
     * 回复反馈
     * @param feedbackId 反馈ID
     * @param replyContent 回复内容
     * @param replyUserId 回复用户ID
     * @return 是否成功
     */
    boolean replyFeedback(Long feedbackId, String replyContent, String replyUserId);

    /**
     * 标记为已处理
     * @param feedbackId 反馈ID
     * @return 是否成功
     */
    boolean markAsProcessed(Long feedbackId);

    /**
     * 根据类型获取反馈
     * @param type 类型
     * @param page 页码
     * @param size 每页数量
     * @return 反馈列表
     */
    List<Feedback> getFeedbackByType(String type, int page, int size);
}
