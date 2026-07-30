package travel.common.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName("user_collection")
@Data
public class TravelNoteCollection {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 被收藏项ID（对应 user_collection.item_id） */
    @TableField("item_id")
    private Integer noteId;

    @TableField("user_id")
    private Integer userId;

    /** 收藏方式: collect / like */
    @TableField("collection_type")
    private String type; // "collect" or "like"

    /** 收藏类型: route / travel_note */
    @TableField("item_type")
    private String itemType = "travel_note";

    @TableField("created_at")
    private LocalDateTime createdAt;
}
