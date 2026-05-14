package travel.controller.route_planning_controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import travel.entity.route_planning.RouteFeedback;
import travel.service.route_planning.RouteFeedbackService;
import travel.service.user_community.UserService;
import travel.utils.Result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/route-feedback")
@RequiredArgsConstructor
@Validated
@Tag(name = "路线反馈管理", description = "用户对AI生成路线的反馈接口")
public class RouteFeedbackController {

    private final RouteFeedbackService routeFeedbackService;
    private final UserService userService;

    @PostMapping("/submit")
    @Operation(summary = "提交路线反馈", description = "用户对AI生成的路线进行评分和反馈")
    public Result<RouteFeedback> submitFeedback(@RequestBody @Valid FeedbackRequest request) {
        try {
            var currentUser = userService.getCurrentUser();

            RouteFeedback feedback = new RouteFeedback();
            feedback.setUserId(currentUser.getId());
            feedback.setRouteId(request.getRouteId());
            feedback.setFeedbackType(request.getFeedbackType());
            feedback.setRating(request.getRating());
            feedback.setComment(request.getComment());
            feedback.setTags(request.getTags() != null ? String.join(",", request.getTags()) : null);
            feedback.setImprovementSuggestions(request.getImprovementSuggestions());

            RouteFeedback result = routeFeedbackService.submitFeedback(feedback);
            return Result.success("反馈提交成功", result);
        } catch (Exception e) {
            log.error("提交路线反馈失败: error={}", e.getMessage());
            return Result.error("反馈提交失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}")
    @Operation(summary = "获取路线的所有反馈")
    public Result<List<RouteFeedback>> getRouteFeedbacks(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            List<RouteFeedback> feedbacks = routeFeedbackService.getRouteFeedbacks(routeId);
            return Result.success("获取反馈成功", feedbacks);
        } catch (Exception e) {
            log.error("获取路线反馈失败: error={}", e.getMessage());
            return Result.error("获取反馈失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}/analysis")
    @Operation(summary = "获取路线反馈分析")
    public Result<Map<String, Object>> getRouteFeedbackAnalysis(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            Map<String, Object> analysis = routeFeedbackService.analyzeFeedbacks(routeId);
            return Result.success("获取分析成功", analysis);
        } catch (Exception e) {
            log.error("获取反馈分析失败: error={}", e.getMessage());
            return Result.error("获取分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/route/{routeId}/rating")
    @Operation(summary = "获取路线平均评分")
    public Result<Double> getRouteAverageRating(
            @Parameter(description = "路线ID") @PathVariable Integer routeId) {
        try {
            double averageRating = routeFeedbackService.getAverageRating(routeId);
            return Result.success("获取评分成功", averageRating);
        } catch (Exception e) {
            log.error("获取平均评分失败: error={}", e.getMessage());
            return Result.error("获取评分失败: " + e.getMessage());
        }
    }

    @lombok.Data
    public static class FeedbackRequest {
        @NotBlank(message = "反馈类型不能为空")
        private String feedbackType;  // rating/suggestion/complaint

        private Integer routeId;

        @Min(value = 1, message = "评分最低为1")
        @Max(value = 5, message = "评分最高为5")
        private Integer rating;

        private String comment;

        private List<String> tags;

        private String improvementSuggestions;
    }
}
