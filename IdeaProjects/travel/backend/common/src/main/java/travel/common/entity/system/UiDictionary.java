package travel.common.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ui_dictionary")
public class UiDictionary {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("dict_type")
    private String dictType;

    @TableField("dict_key")
    private String dictKey;

    @TableField("dict_value")
    private String dictValue;

    @TableField("dict_label")
    private String dictLabel;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
