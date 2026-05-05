package travel.entity.route_planning;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路线评分实体
 */
@Data
@TableName("route_rating")
public class RouteRating {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 路线ID
     */
    private Integer routeId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 评分 (1-5)
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String review;

    /**
     * 舒适度评分 (1-5)
     */
    private Integer comfortRating;

    /**
     * 交通便利性评分 (1-5)
     */
    private Integer transportRating;

    /**
     * 餐饮体验评分 (1-5)
     */
    private Integer diningRating;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
