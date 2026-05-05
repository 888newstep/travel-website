package travel.entity.user_community;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_plan")
public class TravelPlan {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String description;

    private Integer userId;

    private String startDate;

    private String endDate;

    private String destinations;

    private String status;

    private String coverImage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}