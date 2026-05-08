package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import travel.entity.route_planning.Route;
import java.time.LocalDateTime;

@TableName("route_collections")
@Data
public class RouteCollection {

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

    @TableField("collection_time")
    private LocalDateTime collectionTime;

    @TableField("is_public")
    private Boolean isPublic = false;

    @TableField("notes")
    private String notes;

    @TableField("category")
    private String category;

    public String getNote() { return notes; }
    public void setNote(String note) { this.notes = note; }
}