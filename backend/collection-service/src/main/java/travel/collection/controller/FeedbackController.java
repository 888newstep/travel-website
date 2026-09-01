package travel.collection.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.common.entity.user_community.Feedback;
import travel.collection.service.FeedbackService;
import travel.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 反馈管理控制器
 * 处理用户反馈、建议和意见收集
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 提交反馈
     * POST /api/feedback/submit
     */
    @PostMapping("/submit")
    public Result<Feedback> submitFeedback(@RequestBody Feedback feedback) {
        log.info("提交反馈请求: userId={}, type={}", feedback.getUserId(), feedback.getType());
        Feedback result = feedbackService.submitFeedback(feedback);
        return Result.success("反馈提交成功", result);
    }

    /**
     * 查询用户反馈列表
     * GET /api/feedback/list/{userId}
     */
    @GetMapping("/list/{userId}")
    public Result<List<Feedback>> getUserFeedbackList(@PathVariable Integer userId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        log.info("查询用户反馈列表请求: userId={}, page={}, size={}", userId, page, size);
        List<Feedback> feedbackList = feedbackService.getCurrentUserFeedbacks(page, size);
        return Result.success("查询反馈列表成功", feedbackList);
    }

    /**
     * 获取反馈详情
     * GET /api/feedback/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result<Feedback> getFeedbackDetail(@PathVariable Long id) {
        log.info("获取反馈详情请求: id={}", id);
        Feedback feedback = feedbackService.getFeedbackDetail(id);
        return Result.success("获取反馈详情成功", feedback);
    }

    /**
     * 回复反馈
     * POST /api/feedback/reply/{id}
     */
    @PostMapping("/reply/{id}")
    public Result<Boolean> replyFeedback(@PathVariable Long id, @RequestBody Map<String, String> replyData) {
        log.info("回复反馈请求: id={}", id);
        String replyContent = replyData.get("replyContent");
        boolean result = feedbackService.replyFeedback(id, replyContent);
        return Result.success("回复反馈成功", result);
    }

    /**
     * 标记反馈为已处理
     * PUT /api/feedback/process/{id}
     */
    @PutMapping("/process/{id}")
    public Result<Boolean> markAsProcessed(@PathVariable Long id) {
        log.info("标记反馈为已处理请求: id={}", id);
        boolean result = feedbackService.markAsProcessed(id);
        return Result.success("标记成功", result);
    }

    /**
     * 删除反馈
     * DELETE /api/feedback/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteFeedback(@PathVariable Long id) {
        log.info("删除反馈请求: id={}", id);
        boolean result = feedbackService.deleteFeedback(id);
        return Result.success("删除反馈成功", result);
    }

    /**
     * 获取反馈统计信息
     * GET /api/feedback/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFeedbackStatistics() {
        log.info("获取反馈统计信息请求");
        Map<String, Object> statistics = feedbackService.getFeedbackStatistics();
        return Result.success("获取统计信息成功", statistics);
    }

    /**
     * 获取所有可用的反馈类型
     * GET /api/feedback/types
     */
    @GetMapping("/types")
    public Result<List<Map<String, String>>> getFeedbackTypes() {
        log.info("获取反馈类型列表请求");
        List<Map<String, String>> types = feedbackService.getFeedbackTypes();
        return Result.success("获取反馈类型成功", types);
    }

    /**
     * 按类型查询反馈
     * GET /api/feedback/type/{type}
     */
    @GetMapping("/type/{type}")
    public Result<List<Feedback>> getFeedbackByType(@PathVariable String type,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        log.info("按类型查询反馈请求: type={}, page={}, size={}", type, page, size);
        List<Feedback> feedbackList = feedbackService.getFeedbackByType(type, page, size);
        return Result.success("查询成功", feedbackList);
    }
}
