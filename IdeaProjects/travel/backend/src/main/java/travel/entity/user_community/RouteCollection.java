package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import travel.entity.route_planning.Route;
import java.time.LocalDateTime;

@TableName("route_collections")
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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRouteId() { return routeId; }
    public void setRouteId(Integer routeId) { this.routeId = routeId; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCollectionTime() { return collectionTime; }
    public void setCollectionTime(LocalDateTime collectionTime) { this.collectionTime = collectionTime; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}