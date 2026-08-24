package travel.common.entity.route_planning;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import travel.common.entity.travel_recommendation.City;
import travel.common.entity.user_community.User;
import java.time.LocalDateTime;

/**
 * 路线实体
 */
@Data
@TableName("route")
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
    private Boolean isPublic = false;

    /**
     * 路线生命周期：DRAFT、PUBLISHED、ARCHIVED。
     * isPublic 继续保留用于兼容旧客户端，状态字段是新的业务判断依据。
     */
    private String status = "DRAFT";

    /**
     * 乐观锁版本，防止两个编辑请求覆盖彼此的修改。
     */
    @Version
    private Integer version = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
