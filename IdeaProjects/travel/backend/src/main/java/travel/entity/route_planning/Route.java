package travel.entity.route_planning;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import travel.entity.travel_recommendation.City;
import travel.entity.user_community.User;
import java.time.LocalDateTime;

/**
 * 路线实体
 */
@Data
@TableName("routes")
public class Route {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 路线标题
     */
    private String title;

    /**
     * 路线描述
     */
    private String description;

    /**
     * 城市ID
     */
    private Integer cityId;

    /**
     * 城市对象
     */
    @TableField(exist = false)
    private City city;

    /**
     * 天数
     */
    private Integer durationDays = 1;

    /**
     * 难度
     */
    private String difficulty = "中等";

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 创建用户ID
     */
    private Integer userId;

    /**
     * 创建用户对象
     */
    @TableField(exist = false)
    private User user;

    /**
     * 浏览数
     */
    private Integer viewCount = 0;

    /**
     * 点赞数
     */
    private Integer likeCount = 0;

    /**
     * 是否公开
     */
    private Boolean isPublic = true;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
