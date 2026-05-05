package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("travel_note_tags")
public class TravelNoteTag {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("travel_note_id")
    private Integer travelNoteId;

    @TableField("tag_name")
    private String tagName;

    // 关联关系
    @TableField(exist = false)
    private TravelNote travelNote;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTravelNoteId() {
        return travelNoteId;
    }

    public void setTravelNoteId(Integer travelNoteId) {
        this.travelNoteId = travelNoteId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public TravelNote getTravelNote() {
        return travelNote;
    }

    public void setTravelNote(TravelNote travelNote) {
        this.travelNote = travelNote;
    }
}