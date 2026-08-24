package travel.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RouteCollectionVO {
    private Integer id;
    private Integer routeId;
    private Integer userId;
    private String routeTitle;
    private String routeCoverImage;
    private Integer routeDurationDays;
    private String routeDifficulty;
    private LocalDateTime collectionTime;
    private Boolean isPublic;
    private String notes;
}
