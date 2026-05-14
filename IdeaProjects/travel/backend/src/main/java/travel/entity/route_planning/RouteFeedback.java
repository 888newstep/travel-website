package travel.entity.route_planning;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路线规划用户反馈实体
 */
@Data
@TableName("route_feedback")
public class RouteFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("route_id")
    private Integer routeId;

    @TableField("feedback_type")
    private String feedbackType;  // rating/suggestion/complaint

    @TableField("rating")
    private Integer rating;  // 1-5星评分

    @TableField("comment")
    private String comment;  // 文字反馈

    @TableField("tags")
    private String tags;  // 标签JSON: ["too_far", "too_expensive", "good"]

    @TableField("improvement_suggestions")
    private String improvementSuggestions;  // 改进建议JSON

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
