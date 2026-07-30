package travel.common.vo;

import lombok.Data;

/**
 * 路线点VO
 */
@Data
public class RoutePointVO {

    /**
     * 点ID
     */
    private String pointId;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 点类型：1-起点 2-途经点 3-终点
     */
    private Integer pointType;

    /**
     * 景点ID（如果是景点）
     */
    private Long attractionId;

    /**
     * 景点名称（如果是景点）
     */
    private String attractionName;

    /**
     * 停留时间（分钟）
     */
    private Integer stayTime;

    /**
     * 到达时间
     */
    private String arrivalTime;

    /**
     * 离开时间
     */
    private String departureTime;
}
