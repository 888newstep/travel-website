package travel.entity.travel_realtime;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 景点实时状态实体（与数据库表映射）
 */
@TableName("attraction_realtime_status") // 关联数据库表名
public class AttractionRealtimeStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联景点ID */
    @TableField("attraction_id")
    private Long attractionId;

    /** 实时天气（如：晴、小雨、暴雨） */
    private String weather;

    /** 实时温度（℃） */
    private Integer temperature;

    /** 实时人流数量 */
    private Integer crowdCount;

    /** 人流等级（1-5级：1=人少，5=爆满） */
    private Integer crowdLevel;

    /** 最后更新时间（自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识（0=未删，1=已删） */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttractionId() {
        return attractionId;
    }

    public void setAttractionId(Long attractionId) {
        this.attractionId = attractionId;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public void setTemperature(Integer temperature) {
        this.temperature = temperature;
    }

    public Integer getCrowdCount() {
        return crowdCount;
    }

    public void setCrowdCount(Integer crowdCount) {
        this.crowdCount = crowdCount;
    }

    public Integer getCrowdLevel() {
        return crowdLevel;
    }

    public void setCrowdLevel(Integer crowdLevel) {
        this.crowdLevel = crowdLevel;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}