package travel.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travel.common.entity.user_community.RouteComment;
import travel.common.exception.BusinessException;
import travel.common.enums.ErrorCodeEnum;
import travel.common.mapper.route_planning_mapper.RouteCommentMapper;
import travel.route.service.RouteFeedbackService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteFeedbackServiceImpl extends ServiceImpl<RouteCommentMapper, RouteComment>
        implements RouteFeedbackService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RouteComment submitFeedback(RouteComment feedback) {
        if (feedback.getUserId() == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        if (feedback.getRouteId() == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        feedback.setFeedbackType("feedback");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());

        save(feedback);
        log.info("用户提交路线反馈: userId={}, routeId={}, rating={}",
                feedback.getUserId(), feedback.getRouteId(), feedback.getRating());

        return feedback;
    }

    @Override
    public List<RouteComment> getRouteFeedbacks(Integer routeId) {
        LambdaQueryWrapper<RouteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteComment::getRouteId, routeId)
                .eq(RouteComment::getFeedbackType, "feedback")
                .orderByDesc(RouteComment::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public List<RouteComment> getUserFeedbacks(Integer userId) {
        LambdaQueryWrapper<RouteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteComment::getUserId, userId)
                .eq(RouteComment::getFeedbackType, "feedback")
                .orderByDesc(RouteComment::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> analyzeFeedbacks(Integer routeId) {
        List<RouteComment> feedbacks = getRouteFeedbacks(routeId);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalFeedbacks", feedbacks.size());

        double averageRating = feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .mapToDouble(RouteComment::getRating)
                .average()
                .orElse(0.0);
        analysis.put("averageRating", averageRating);

        // 统计评分分布
        Map<Double, Long> ratingDistribution = feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .collect(Collectors.groupingBy(RouteComment::getRating, Collectors.counting()));
        analysis.put("ratingDistribution", ratingDistribution);

        // 统计常见问题标签
        Map<String, Long> tagFrequency = feedbacks.stream()
                .filter(f -> f.getTags() != null && !f.getTags().isEmpty())
                .flatMap(f -> Arrays.stream(f.getTags().split(",")))
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        analysis.put("commonIssues", tagFrequency);

        // 提取改进建议
        List<String> suggestions = feedbacks.stream()
                .filter(f -> f.getImprovementSuggestions() != null)
                .map(RouteComment::getImprovementSuggestions)
                .collect(Collectors.toList());
        analysis.put("improvementSuggestions", suggestions);

        return analysis;
    }
}