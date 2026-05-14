package travel.service.impl.route_planning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travel.entity.route_planning.RouteFeedback;
import travel.exception.BusinessException;
import travel.enums.ErrorCodeEnum;
import travel.mapper.route_planning_mapper.RouteFeedbackMapper;
import travel.service.route_planning.RouteFeedbackService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteFeedbackServiceImpl extends ServiceImpl<RouteFeedbackMapper, RouteFeedback>
        implements RouteFeedbackService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RouteFeedback submitFeedback(RouteFeedback feedback) {
        if (feedback.getUserId() == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        if (feedback.getRouteId() == null) {
            throw new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);
        }

        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        feedback.setDeleted(0);

        save(feedback);
        log.info("用户提交路线反馈: userId={}, routeId={}, rating={}",
                feedback.getUserId(), feedback.getRouteId(), feedback.getRating());

        return feedback;
    }

    @Override
    public List<RouteFeedback> getRouteFeedbacks(Integer routeId) {
        LambdaQueryWrapper<RouteFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteFeedback::getRouteId, routeId)
                .orderByDesc(RouteFeedback::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public List<RouteFeedback> getUserFeedbacks(Integer userId) {
        LambdaQueryWrapper<RouteFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteFeedback::getUserId, userId)
                .orderByDesc(RouteFeedback::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public double getAverageRating(Integer routeId) {
        List<RouteFeedback> feedbacks = getRouteFeedbacks(routeId);

        if (feedbacks.isEmpty()) {
            return 0.0;
        }

        return feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .mapToInt(RouteFeedback::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public Map<String, Object> analyzeFeedbacks(Integer routeId) {
        List<RouteFeedback> feedbacks = getRouteFeedbacks(routeId);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalFeedbacks", feedbacks.size());
        analysis.put("averageRating", getAverageRating(routeId));

        // 统计评分分布
        Map<Integer, Long> ratingDistribution = feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .collect(Collectors.groupingBy(RouteFeedback::getRating, Collectors.counting()));
        analysis.put("ratingDistribution", ratingDistribution);

        // 统计常见问题标签
        Map<String, Long> tagFrequency = feedbacks.stream()
                .filter(f -> f.getTags() != null && !f.getTags().isEmpty())
                .flatMap(f -> Arrays.stream(f.getTags().split(",")))
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        analysis.put("commonIssues", tagFrequency);

        // 提取改进建议关键词
        List<String> suggestions = feedbacks.stream()
                .filter(f -> f.getImprovementSuggestions() != null)
                .map(RouteFeedback::getImprovementSuggestions)
                .collect(Collectors.toList());
        analysis.put("improvementSuggestions", suggestions);

        return analysis;
    }
}
