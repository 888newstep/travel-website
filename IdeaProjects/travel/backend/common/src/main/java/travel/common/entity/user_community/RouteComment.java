package travel.common.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import travel.common.entity.route_planning.Route;
import java.time.LocalDateTime;

@Data
@TableName("route_comment")
public class RouteComment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("route_id")
    private Integer routeId;

    @TableField(exist = false)
    private Route route;

    @TableField("user_id")
    private Integer userId;

    @TableField(exist = false)
    private User user;

    @TableField("rating")
    private Double rating;

    @TableField("content")
    private String content;

    @TableField("images")
    private String images;

    @TableField("likes_count")
    private Integer likesCount = 0;

    @TableField("is_anonymous")
    private Boolean isAnonymous = false;

    @TableField("is_published")
    private Boolean isPublished = true;

    @TableField("reply_to")
    private Integer replyTo;

    // 合并自 route_rating 的字段
    @TableField("comfort_rating")
    private Integer comfortRating;

    @TableField("transport_rating")
    private Integer transportRating;

    @TableField("dining_rating")
    private Integer diningRating;

    // 合并自 route_feedback 的字段
    @TableField("feedback_type")
    private String feedbackType;

    @TableField("tags")
    private String tags;

    @TableField("improvement_suggestions")
    private String improvementSuggestions;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 获取点赞数（兼容方法）
     */
    public Integer getLikeCount() {
        return likesCount;
    }
}
