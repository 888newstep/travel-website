package travel.entity.travel_recommendation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件版本实体
 */
@Data
@TableName("file_version")
public class FileVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer fileId;

    private String version;

    private String filePath;

    private Long fileSize;

    private String changeDescription;

    private Integer createdBy;

    private LocalDateTime createdAt;

    private Integer deleted;
}
