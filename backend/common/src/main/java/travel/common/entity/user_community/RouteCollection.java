package travel.common.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import travel.common.entity.route_planning.Route;
import java.time.LocalDateTime;

@TableName("user_collection")
@Data
public class RouteCollection {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 被收藏项ID（对应 user_collection.item_id） */
    @TableField("item_id")
    private Integer routeId;

    @TableField(exist = false)
    private Route route;

    @TableField("user_id")
    private Integer userId;

    @TableField(exist = false)
    private User user;

    /** 收藏类型: route / travel_note */
    @TableField("item_type")
    private String itemType = "route";

    /** 收藏方式: collect / like */
    @TableField("collection_type")
    private String collectionType = "collect";

    /** 收藏时间（对应 user_collection.created_at） */
    @TableField("created_at")
    private LocalDateTime collectionTime;

    @TableField("is_public")
    private Boolean isPublic = false;

    /** Java侧使用，不直接映射DB（与 collectionTime 同源） */
    @TableField(exist = false)
    private LocalDateTime createdAt;

    @TableField("notes")
    private String notes;

    @TableField("category")
    private String category;

    public String getNote() { return notes; }
    public void setNote(String note) { this.notes = note; }

    public void setCreateTime(LocalDateTime createTime) {
        this.createdAt = createTime;
        this.collectionTime = createTime;
    }
}
