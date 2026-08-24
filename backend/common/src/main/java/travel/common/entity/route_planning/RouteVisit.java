package travel.common.entity.route_planning;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("route_visit")
public class RouteVisit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer routeId;

    private Integer userId;

    private String visitorHash;

    private String visitorType;

    private LocalDate visitDate;

    private LocalDateTime visitedAt;
}
