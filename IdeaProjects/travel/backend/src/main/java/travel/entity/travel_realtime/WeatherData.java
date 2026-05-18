package travel.entity.travel_realtime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 天气数据实体
 */
@Data
@TableName("weather_data")
public class WeatherData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer cityId;

    private String cityName;

    private String weather;

    private Integer temperature;

    private Integer humidity;

    private String windDirection;

    private Integer windSpeed;

    private String airQuality;

    private LocalDateTime forecastTime;

    private LocalDateTime createdAt;

    private Integer deleted;
}
