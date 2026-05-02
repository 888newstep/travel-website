package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_note_comments")
public class TravelNoteComment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("travel_note_id")
    private Integer travelNoteId;

    @TableField("user_id")
    private Integer userId;

    @TableField("content")
    private String content;

    @TableField("parent_id")
    private Integer parentId;

    @TableField("likes_count")
    private Integer likesCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    // 关联关系
    @TableField(exist = false)
    private TravelNote travelNote;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private TravelNoteComment parentComment;
}
