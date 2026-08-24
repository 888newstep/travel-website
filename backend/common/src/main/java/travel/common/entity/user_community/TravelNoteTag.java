package travel.common.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_note_tags")
public class TravelNoteTag {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("note_id")
    private Integer noteId;

    @TableField("tag_name")
    private String tagName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private TravelNote travelNote;
}
