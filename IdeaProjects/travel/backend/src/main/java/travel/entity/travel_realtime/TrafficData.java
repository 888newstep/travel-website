package travel.entity.travel_realtime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交通状况数据实体
 */
@Data
@TableName("traffic_data")
public class TrafficData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer cityId;

    private String roadName;

    private String congestionLevel;

    private Double averageSpeed;

    private Integer incidentCount;

    private String description;

    private LocalDateTime dataTime;

    private LocalDateTime createdAt;

    private Integer deleted;
}
