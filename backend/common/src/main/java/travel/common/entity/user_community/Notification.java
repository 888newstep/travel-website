package travel.common.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("type")
    private String type;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("redirect_url")
    private String redirectUrl;

    /**
     * RabbitMQ 原始消息唯一号，用于数据库最终幂等兜底；历史人工创建通知允许为空。
     */
    @TableField("source_message_id")
    private String sourceMessageId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
