package travel.route.service;

import com.baomidou.mybatisplus.extension.service.IService;
import travel.common.entity.user_community.RouteComment;

import java.util.List;
import java.util.Map;

/**
 * 路线反馈服务接口 (已合并到 route_comment 表)
 */
public interface RouteFeedbackService extends IService<RouteComment> {

    /**
     * 提交路线反馈
     */
    RouteComment submitFeedback(RouteComment feedback);

    /**
     * 获取某条路线的所有反馈
     */
    List<RouteComment> getRouteFeedbacks(Integer routeId);

    /**
     * 获取用户的反馈历史
     */
    List<RouteComment> getUserFeedbacks(Integer userId);

    /**
     * 分析反馈数据，生成改进建议
     */
    Map<String, Object> analyzeFeedbacks(Integer routeId);
}