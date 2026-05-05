package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String type;

    private String content;

    private String contactInfo;

    private String status;

    private String replyContent;

    private LocalDateTime replyTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}