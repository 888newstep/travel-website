package travel.common.entity.user_community;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import travel.common.entity.route_planning.Route;
import java.time.LocalDateTime;

@TableName("route_share")
@Data
public class RouteShare {

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

    @TableField("share_code")
    private String shareCode;

    @TableField("share_title")
    private String shareTitle;

    @TableField("share_description")
    private String shareDescription;

    @TableField("share_count")
    private Integer shareCount = 0;

    @TableField("visit_count")
    private Integer visitCount = 0;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @lombok.ToString.Exclude
    private String password;

    @TableField("file_name")
    private String fileName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("item_id")
    private Integer itemId;

    @TableField("item_type")
    private String itemType;
}
