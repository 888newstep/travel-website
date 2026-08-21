package travel.common.entity.user_community;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @lombok.ToString.Exclude
    private String password;

    @TableField("avatar")
    private String avatar;

    @TableField("phone")
    private String phone;

    /** 1=普通用户，9=管理员。 */
    @TableField("user_type")
    private Integer userType = 1;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
