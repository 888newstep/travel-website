package travel.service.route_planning;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.entity.route_planning.RouteFeedback;

import java.util.List;
import java.util.Map;

public interface RouteFeedbackService extends IService<RouteFeedback> {

    /**
     * 提交路线反馈
     */
    RouteFeedback submitFeedback(RouteFeedback feedback);

    /**
     * 获取某条路线的所有反馈
     */
    List<RouteFeedback> getRouteFeedbacks(Integer routeId);

    /**
     * 获取用户的反馈历史
     */
    List<RouteFeedback> getUserFeedbacks(Integer userId);

    /**
     * 获取路线的平均评分
     */
    double getAverageRating(Integer routeId);

    /**
     * 分析反馈数据，生成改进建议
     */
    Map<String, Object> analyzeFeedbacks(Integer routeId);
}
